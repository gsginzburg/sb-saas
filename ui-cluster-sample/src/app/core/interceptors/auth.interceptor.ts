import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  // Attach cluster token to all cluster API calls
  const clusterUrl = auth.getClusterUrl() || environment.clusterUrl;
  if (req.url.startsWith(clusterUrl) || req.url.startsWith(environment.clusterUrl)) {
    const token = auth.getClusterToken();
    if (token) {
      req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    }
    return next(req);
  }

  // Attach dispatch token to dispatch API calls (used only during login flow)
  if (req.url.startsWith(environment.dispatchUrl)) {
    const token = auth.getDispatchToken();
    if (token) {
      req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    }
    return next(req);
  }

  return next(req);
};
