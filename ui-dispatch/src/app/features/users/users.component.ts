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

import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, DatePipe } from '@angular/common';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmationService, MessageService } from 'primeng/api';
import { UserService, CreateUserRequest } from '../../core/services/user.service';
import { AppUser } from '../../core/models/api.model';

type TagSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

interface SelectOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    PasswordModule,
    SelectModule,
    TagModule,
    ConfirmDialogModule,
    ToastModule,
    TooltipModule
  ],
  providers: [ConfirmationService, MessageService],
  template: `
    <p-toast />
    <p-confirmDialog />

    <div class="flex justify-content-between align-items-center mb-4">
      <h2 class="m-0">Users</h2>
      <p-button label="New User" icon="pi pi-plus" (click)="openCreateDialog()" />
    </div>

    <p-table
      [value]="users()"
      [loading]="loading()"
      [paginator]="true"
      [rows]="pageSize"
      [totalRecords]="totalRecords()"
      [lazy]="true"
      (onLazyLoad)="onLazyLoad($event)"
      responsiveLayout="scroll"
      styleClass="p-datatable-gridlines"
    >
      <ng-template pTemplate="header">
        <tr>
          <th>Email</th>
          <th>Name</th>
          <th>Type</th>
          <th>Status</th>
          <th>Created</th>
          <th style="width: 10rem">Actions</th>
        </tr>
      </ng-template>
      <ng-template pTemplate="body" let-user>
        <tr>
          <td class="font-medium">{{ user.email }}</td>
          <td>{{ user.firstName }} {{ user.lastName }}</td>
          <td>
            <p-tag
              [value]="user.userType"
              [severity]="user.userType === 'BACKOFFICE' ? 'info' : 'secondary'"
            />
          </td>
          <td>
            <p-tag
              [value]="user.status"
              [severity]="statusSeverity(user.status)"
            />
          </td>
          <td>{{ user.createdAt | date: 'short' }}</td>
          <td>
            <p-button
              [icon]="user.status === 'ACTIVE' ? 'pi pi-ban' : 'pi pi-check-circle'"
              [rounded]="true"
              [text]="true"
              [severity]="user.status === 'ACTIVE' ? 'warn' : 'success'"
              (click)="toggleStatus(user)"
              [pTooltip]="user.status === 'ACTIVE' ? 'Deactivate' : 'Activate'"
            />
            <p-button
              icon="pi pi-trash"
              [rounded]="true"
              [text]="true"
              severity="danger"
              (click)="confirmDelete(user)"
              pTooltip="Delete user"
            />
          </td>
        </tr>
      </ng-template>
      <ng-template pTemplate="emptymessage">
        <tr>
          <td colspan="6" class="text-center p-4 text-color-secondary">No users found.</td>
        </tr>
      </ng-template>
    </p-table>

    <!-- Create User Dialog -->
    <p-dialog
      header="New User"
      [(visible)]="showCreateDialog"
      [modal]="true"
      [style]="{ width: '480px' }"
      [draggable]="false"
    >
      <div class="grid">
        <div class="col-12">
          <label class="block mb-2 font-medium">
            Email <span class="text-red-500">*</span>
          </label>
          <input
            pInputText
            type="email"
            [(ngModel)]="form.email"
            class="w-full"
            placeholder="user@example.com"
          />
        </div>
        <div class="col-12">
          <label class="block mb-2 font-medium">
            Password <span class="text-red-500">*</span>
          </label>
          <p-password
            [(ngModel)]="form.password"
            [feedback]="true"
            styleClass="w-full"
            inputStyleClass="w-full"
            placeholder="Minimum 8 characters"
            [toggleMask]="true"
          />
        </div>
        <div class="col-6">
          <label class="block mb-2 font-medium">First Name</label>
          <input
            pInputText
            [(ngModel)]="form.firstName"
            class="w-full"
            placeholder="First name"
          />
        </div>
        <div class="col-6">
          <label class="block mb-2 font-medium">Last Name</label>
          <input
            pInputText
            [(ngModel)]="form.lastName"
            class="w-full"
            placeholder="Last name"
          />
        </div>
        <div class="col-12">
          <label class="block mb-2 font-medium">
            User Type <span class="text-red-500">*</span>
          </label>
          <p-select
            [options]="userTypeOptions"
            [(ngModel)]="form.userType"
            optionLabel="label"
            optionValue="value"
            styleClass="w-full"
          />
        </div>
      </div>
      <ng-template pTemplate="footer">
        <p-button
          label="Cancel"
          severity="secondary"
          (click)="showCreateDialog = false"
        />
        <p-button
          label="Create"
          icon="pi pi-check"
          (click)="createUser()"
          [loading]="saving()"
          [disabled]="!form.email.trim() || !form.password.trim()"
        />
      </ng-template>
    </p-dialog>
  `
})
export class UsersComponent implements OnInit {
  private userService = inject(UserService);
  private confirmationService = inject(ConfirmationService);
  private messageService = inject(MessageService);

  // Signals
  users = signal<AppUser[]>([]);
  loading = signal(false);
  saving = signal(false);
  totalRecords = signal(0);

  pageSize = 20;

  // Create dialog
  showCreateDialog = false;
  form: CreateUserRequest & { firstName: string; lastName: string } = {
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    userType: 'TENANT'
  };

  userTypeOptions: SelectOption[] = [
    { label: 'Tenant User', value: 'TENANT' },
    { label: 'Backoffice Admin', value: 'BACKOFFICE' }
  ];

  ngOnInit(): void {
    this.loadUsers(0);
  }

  loadUsers(page: number): void {
    this.loading.set(true);
    this.userService.getUsers(page, this.pageSize).subscribe({
      next: (result) => {
        this.users.set(result.content);
        this.totalRecords.set(result.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load users'
        });
      }
    });
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    const page = Math.floor((event.first ?? 0) / (event.rows ?? this.pageSize));
    this.loadUsers(page);
  }

  openCreateDialog(): void {
    this.form = {
      email: '',
      password: '',
      firstName: '',
      lastName: '',
      userType: 'TENANT'
    };
    this.showCreateDialog = true;
  }

  createUser(): void {
    if (!this.form.email.trim() || !this.form.password.trim()) return;
    this.saving.set(true);

    const payload: CreateUserRequest = {
      email: this.form.email.trim(),
      password: this.form.password,
      userType: this.form.userType
    };
    if (this.form.firstName.trim()) payload.firstName = this.form.firstName.trim();
    if (this.form.lastName.trim()) payload.lastName = this.form.lastName.trim();

    this.userService.createUser(payload).subscribe({
      next: () => {
        this.showCreateDialog = false;
        this.saving.set(false);
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'User created successfully'
        });
        this.loadUsers(0);
      },
      error: (err) => {
        this.saving.set(false);
        const e = err as { error?: { error?: string; message?: string } };
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: e?.error?.error || e?.error?.message || 'Failed to create user'
        });
      }
    });
  }

  toggleStatus(user: AppUser): void {
    const newStatus = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const action = newStatus === 'ACTIVE' ? 'activate' : 'deactivate';

    this.confirmationService.confirm({
      message: `Are you sure you want to ${action} user <strong>${user.email}</strong>?`,
      header: 'Confirm Status Change',
      icon: 'pi pi-question-circle',
      accept: () => {
        this.userService.updateStatus(user.id, newStatus).subscribe({
          next: (updated) => {
            // Update the user in place
            this.users.update(list =>
              list.map(u => (u.id === updated.id ? updated : u))
            );
            this.messageService.add({
              severity: 'success',
              summary: 'Updated',
              detail: `User ${action}d successfully`
            });
          },
          error: (err) => {
            const e = err as { error?: { error?: string } };
            this.messageService.add({
              severity: 'error',
              summary: 'Error',
              detail: e?.error?.error || `Failed to ${action} user`
            });
          }
        });
      }
    });
  }

  confirmDelete(user: AppUser): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete user <strong>${user.email}</strong>? This action cannot be undone.`,
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.userService.deleteUser(user.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Deleted',
              detail: `User "${user.email}" deleted`
            });
            this.loadUsers(0);
          },
          error: (err) => {
            const e = err as { error?: { error?: string } };
            this.messageService.add({
              severity: 'error',
              summary: 'Error',
              detail: e?.error?.error || 'Failed to delete user'
            });
          }
        });
      }
    });
  }

  statusSeverity(status: string): TagSeverity {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'INACTIVE':
        return 'warn';
      case 'SUSPENDED':
        return 'danger';
      default:
        return 'secondary';
    }
  }
}
