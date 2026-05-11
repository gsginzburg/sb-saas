/*
 * Copyright 2026 Gary Ginzburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CardModule, InputTextModule, PasswordModule, ButtonModule, MessageModule],
  template: `
    <div class="flex align-items-center justify-content-center min-h-screen">
      <p-card header="Dispatch Management" [style]="{ width: '400px' }">
        <form (ngSubmit)="login()">
          <div class="field mb-4">
            <label class="block mb-2 font-medium">Email</label>
            <input
              pInputText
              type="email"
              [(ngModel)]="email"
              name="email"
              class="w-full"
              placeholder="admin@dispatch.local"
              required
            />
          </div>
          <div class="field mb-4">
            <label class="block mb-2 font-medium">Password</label>
            <p-password
              [(ngModel)]="password"
              name="password"
              [feedback]="false"
              styleClass="w-full"
              inputStyleClass="w-full"
              placeholder="Password"
              [toggleMask]="true"
            />
          </div>
          @if (error) {
            <p-message severity="error" [text]="error" styleClass="w-full mb-3" />
          }
          <p-button
            type="submit"
            label="Sign In"
            icon="pi pi-sign-in"
            [loading]="loading"
            styleClass="w-full"
          />
        </form>
      </p-card>
    </div>
  `
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  loading = false;
  error = '';

  login(): void {
    if (!this.email || !this.password) {
      this.error = 'Email and password are required';
      return;
    }
    this.loading = true;
    this.error = '';
    this.auth.login({ email: this.email, password: this.password })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: () => this.router.navigate(['/']),
        error: (err) => {
          this.error =
            err?.error?.error || err?.error?.message || 'Invalid credentials. Please try again.';
        }
      });
  }
}
