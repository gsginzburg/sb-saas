import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, switchMap } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api.model';

interface DispatchLoginResponse {
  accessToken: string;
  refreshToken: string;
  userType: string;
  clusterUrl: string;
  userId: string;
  email: string;
}

interface ExchangeResponse {
  accessToken: string;
  tenantId: string;
  shardId: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly CLUSTER_TOKEN_KEY = 'cluster_token';
  private readonly DISPATCH_TOKEN_KEY = 'dispatch_token';
  private readonly USER_KEY = 'cluster_user';
  private readonly CLUSTER_URL_KEY = 'cluster_url';

  private _isLoggedIn = signal(!!localStorage.getItem(this.CLUSTER_TOKEN_KEY));
  isLoggedIn = computed(() => this._isLoggedIn());

  constructor(private http: HttpClient, private router: Router) {}

  login(email: string, password: string): Observable<void> {
    return this.http.post<ApiResponse<DispatchLoginResponse>>(
      `${environment.dispatchUrl}/api/auth/login`,
      { email, password }
    ).pipe(
      map(r => r.data),
      switchMap(dispatchData => {
        // Store dispatch token for the exchange request header
        localStorage.setItem(this.DISPATCH_TOKEN_KEY, dispatchData.accessToken);
        const clusterUrl = dispatchData.clusterUrl || environment.clusterUrl;
        localStorage.setItem(this.CLUSTER_URL_KEY, clusterUrl);

        return this.http.post<ApiResponse<ExchangeResponse>>(
          `${clusterUrl}/api/auth/exchange`,
          { exchangeToken: dispatchData.accessToken }
        ).pipe(
          map(r => r.data),
          tap(clusterData => {
            localStorage.setItem(this.CLUSTER_TOKEN_KEY, clusterData.accessToken);
            localStorage.setItem(this.USER_KEY, JSON.stringify({
              email: dispatchData.email,
              tenantId: clusterData.tenantId,
              shardId: clusterData.shardId
            }));
            // Dispatch token no longer needed after exchange
            localStorage.removeItem(this.DISPATCH_TOKEN_KEY);
            this._isLoggedIn.set(true);
          }),
          map(() => undefined as void)
        );
      })
    );
  }

  logout(): void {
    localStorage.clear();
    this._isLoggedIn.set(false);
    this.router.navigate(['/login']);
  }

  getClusterToken(): string | null {
    return localStorage.getItem(this.CLUSTER_TOKEN_KEY);
  }

  getDispatchToken(): string | null {
    return localStorage.getItem(this.DISPATCH_TOKEN_KEY);
  }

  getClusterUrl(): string {
    return localStorage.getItem(this.CLUSTER_URL_KEY) || environment.clusterUrl;
  }

  getCurrentUser(): { email: string; tenantId: string; shardId: string } | null {
    const s = localStorage.getItem(this.USER_KEY);
    return s ? JSON.parse(s) : null;
  }
}
