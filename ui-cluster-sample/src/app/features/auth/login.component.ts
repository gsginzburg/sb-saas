import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    MessageModule
  ],
  template: `
    <div class="flex align-items-center justify-content-center min-h-screen surface-ground">
      <div class="w-full" style="max-width: 420px; padding: 1rem;">
        <p-card>
          <ng-template pTemplate="header">
            <div class="text-center p-4 pb-0">
              <div class="text-3xl font-bold text-primary mb-2">Tenant Sample App</div>
              <div class="text-color-secondary text-sm">Sign in to your tenant account</div>
            </div>
          </ng-template>

          <div class="p-2">
            @if (errorMessage()) {
              <p-message severity="error" [text]="errorMessage()!" styleClass="w-full mb-3"/>
            }

            <div class="field mb-4">
              <label for="email" class="block font-medium mb-2">Email</label>
              <input
                id="email"
                pInputText
                type="email"
                [(ngModel)]="email"
                placeholder="Enter your email"
                class="w-full"
                [disabled]="loading()"
                (keyup.enter)="onLogin()"
              />
            </div>

            <div class="field mb-4">
              <label for="password" class="block font-medium mb-2">Password</label>
              <p-password
                inputId="password"
                [(ngModel)]="password"
                placeholder="Enter your password"
                [toggleMask]="true"
                [feedback]="false"
                styleClass="w-full"
                inputStyleClass="w-full"
                [disabled]="loading()"
                (keyup.enter)="onLogin()"
              />
            </div>

            <p-button
              label="Sign In"
              icon="pi pi-sign-in"
              styleClass="w-full"
              [loading]="loading()"
              [disabled]="!email || !password"
              (click)="onLogin()"
            />
          </div>
        </p-card>
      </div>
    </div>
  `
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  loading = signal(false);
  errorMessage = signal<string | null>(null);

  onLogin(): void {
    if (!this.email || !this.password || this.loading()) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.auth.login(this.email, this.password).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err?.error?.error
          || err?.error?.message
          || err?.message
          || 'Login failed. Please check your credentials.';
        this.errorMessage.set(msg);
      }
    });
  }
}
