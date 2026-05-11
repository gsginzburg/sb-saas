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

import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, DatePipe } from '@angular/common';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmationService, MessageService } from 'primeng/api';
import { TenantService } from '../../core/services/tenant.service';
import { ClusterService } from '../../core/services/cluster.service';
import { Tenant, Cluster, TenantMembership } from '../../core/models/api.model';

type TagSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

interface StatusOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-tenants',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
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
      <h2 class="m-0">Tenants</h2>
      <p-button label="New Tenant" icon="pi pi-plus" (click)="openCreateDialog()" />
    </div>

    <p-table
      [value]="tenants()"
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
          <th>Name</th>
          <th>Status</th>
          <th>Cluster</th>
          <th>Created</th>
          <th style="width: 10rem">Actions</th>
        </tr>
      </ng-template>
      <ng-template pTemplate="body" let-tenant>
        <tr>
          <td class="font-medium">{{ tenant.name }}</td>
          <td>
            <p-tag
              [value]="tenant.status"
              [severity]="statusSeverity(tenant.status)"
            />
          </td>
          <td>{{ tenant.clusterName || '—' }}</td>
          <td>{{ tenant.createdAt | date: 'short' }}</td>
          <td>
            <p-button
              icon="pi pi-pencil"
              [rounded]="true"
              [text]="true"
              severity="info"
              (click)="openEditDialog(tenant)"
              pTooltip="Edit status"
            />
            <p-button
              icon="pi pi-sitemap"
              [rounded]="true"
              [text]="true"
              severity="secondary"
              (click)="openUsersDialog(tenant)"
              pTooltip="Manage users"
            />
            <p-button
              icon="pi pi-trash"
              [rounded]="true"
              [text]="true"
              severity="danger"
              (click)="confirmDelete(tenant)"
              pTooltip="Delete"
            />
          </td>
        </tr>
      </ng-template>
      <ng-template pTemplate="emptymessage">
        <tr>
          <td colspan="5" class="text-center p-4 text-color-secondary">No tenants found.</td>
        </tr>
      </ng-template>
    </p-table>

    <!-- Create Dialog -->
    <p-dialog
      header="New Tenant"
      [(visible)]="showCreateDialog"
      [modal]="true"
      [style]="{ width: '450px' }"
      [draggable]="false"
    >
      <div class="field mb-3">
        <label class="block mb-2 font-medium">Name <span class="text-red-500">*</span></label>
        <input
          pInputText
          [(ngModel)]="newTenantName"
          class="w-full"
          placeholder="Enter tenant name"
        />
      </div>
      <div class="field mb-3">
        <label class="block mb-2 font-medium">Cluster <span class="text-red-500">*</span></label>
        <p-select
          [options]="clusters()"
          [(ngModel)]="selectedClusterId"
          optionLabel="name"
          optionValue="id"
          placeholder="Select a cluster"
          styleClass="w-full"
          [filter]="true"
          filterPlaceholder="Search clusters..."
        />
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
          (click)="createTenant()"
          [loading]="saving()"
          [disabled]="!newTenantName.trim() || !selectedClusterId"
        />
      </ng-template>
    </p-dialog>

    <!-- Edit Status / Assign Cluster Dialog -->
    <p-dialog
      header="Edit Tenant: {{ editingTenant?.name }}"
      [(visible)]="showEditDialog"
      [modal]="true"
      [style]="{ width: '420px' }"
      [draggable]="false"
    >
      <div class="field mb-3">
        <label class="block mb-2 font-medium">Status</label>
        <p-select
          [options]="statusOptions"
          [(ngModel)]="editStatus"
          optionLabel="label"
          optionValue="value"
          styleClass="w-full"
        />
      </div>
      <div class="field mb-3">
        <label class="block mb-2 font-medium">Assign Cluster</label>
        <p-select
          [options]="clusters()"
          [(ngModel)]="editClusterId"
          optionLabel="name"
          optionValue="id"
          placeholder="Keep current cluster"
          styleClass="w-full"
          [filter]="true"
          filterPlaceholder="Search clusters..."
          [showClear]="true"
        />
      </div>
      <ng-template pTemplate="footer">
        <p-button
          label="Cancel"
          severity="secondary"
          (click)="showEditDialog = false"
        />
        <p-button
          label="Save"
          icon="pi pi-check"
          (click)="updateTenant()"
          [loading]="saving()"
        />
      </ng-template>
    </p-dialog>

    <!-- Tenant Users Dialog -->
    <p-dialog
      header="Users: {{ selectedTenant?.name }}"
      [(visible)]="showUsersDialog"
      [modal]="true"
      [style]="{ width: '600px' }"
      [draggable]="false"
    >
      <div class="mb-3">
        <p-table [value]="tenantUsers()" [loading]="loadingUsers()" styleClass="p-datatable-sm">
          <ng-template pTemplate="header">
            <tr>
              <th>User</th>
              <th>Role</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-membership>
            <tr>
              <td>{{ membership.userEmail || membership.userId }}</td>
              <td>{{ membership.role }}</td>
              <td>
                <p-tag [value]="membership.status" severity="info" />
              </td>
              <td>
                <p-button
                  icon="pi pi-times"
                  [rounded]="true"
                  [text]="true"
                  severity="danger"
                  pTooltip="Remove from tenant"
                  (click)="removeUserFromTenant(membership)"
                />
              </td>
            </tr>
          </ng-template>
          <ng-template pTemplate="emptymessage">
            <tr>
              <td colspan="4" class="text-center p-3 text-color-secondary">
                No users in this tenant.
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>

      <div class="border-top-1 surface-border pt-3">
        <h4 class="mt-0 mb-3">Add User to Tenant</h4>
        <div class="flex gap-2 align-items-end">
          <div class="flex-1">
            <label class="block mb-1 text-sm">User ID</label>
            <input
              pInputText
              [(ngModel)]="addUserId"
              class="w-full"
              placeholder="User UUID"
            />
          </div>
          <div class="flex-1">
            <label class="block mb-1 text-sm">Role</label>
            <p-select
              [options]="roleOptions"
              [(ngModel)]="addUserRole"
              styleClass="w-full"
            />
          </div>
          <p-button
            label="Add"
            icon="pi pi-plus"
            (click)="assignUser()"
            [loading]="savingUser()"
            [disabled]="!addUserId.trim()"
          />
        </div>
      </div>

      <ng-template pTemplate="footer">
        <p-button
          label="Close"
          severity="secondary"
          (click)="showUsersDialog = false"
        />
      </ng-template>
    </p-dialog>
  `
})
export class TenantsComponent implements OnInit {
  private tenantService = inject(TenantService);
  private clusterService = inject(ClusterService);
  private confirmationService = inject(ConfirmationService);
  private messageService = inject(MessageService);

  // Signals
  tenants = signal<Tenant[]>([]);
  clusters = signal<Cluster[]>([]);
  loading = signal(false);
  saving = signal(false);
  totalRecords = signal(0);
  tenantUsers = signal<TenantMembership[]>([]);
  loadingUsers = signal(false);
  savingUser = signal(false);

  pageSize = 20;

  // Create dialog
  showCreateDialog = false;
  newTenantName = '';
  selectedClusterId = '';

  // Edit dialog
  showEditDialog = false;
  editingTenant: Tenant | null = null;
  editStatus = '';
  editClusterId = '';

  // Users dialog
  showUsersDialog = false;
  selectedTenant: Tenant | null = null;
  addUserId = '';
  addUserRole = 'MEMBER';

  statusOptions: StatusOption[] = [
    { label: 'Active', value: 'ACTIVE' },
    { label: 'Inactive', value: 'INACTIVE' },
    { label: 'Archived', value: 'ARCHIVED' }
  ];

  roleOptions = ['MEMBER', 'ADMIN', 'OWNER'];

  ngOnInit(): void {
    this.loadClusters();
    this.loadTenants(0);
  }

  loadClusters(): void {
    this.clusterService.getAllClusters().subscribe({
      next: (c) => this.clusters.set(c),
      error: () =>
        this.messageService.add({
          severity: 'warn',
          summary: 'Warning',
          detail: 'Could not load clusters'
        })
    });
  }

  loadTenants(page: number): void {
    this.loading.set(true);
    this.tenantService.getTenants(page, this.pageSize).subscribe({
      next: (result) => {
        this.tenants.set(result.content);
        this.totalRecords.set(result.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load tenants'
        });
      }
    });
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    const page = Math.floor((event.first ?? 0) / (event.rows ?? this.pageSize));
    this.loadTenants(page);
  }

  openCreateDialog(): void {
    this.newTenantName = '';
    this.selectedClusterId = '';
    this.showCreateDialog = true;
  }

  createTenant(): void {
    if (!this.newTenantName.trim() || !this.selectedClusterId) return;
    this.saving.set(true);
    this.tenantService.createTenant(this.newTenantName.trim(), this.selectedClusterId).subscribe({
      next: () => {
        this.showCreateDialog = false;
        this.saving.set(false);
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Tenant created successfully'
        });
        this.loadTenants(0);
      },
      error: (err) => {
        this.saving.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: err?.error?.error || 'Failed to create tenant'
        });
      }
    });
  }

  openEditDialog(tenant: Tenant): void {
    this.editingTenant = tenant;
    this.editStatus = tenant.status;
    this.editClusterId = tenant.clusterId || '';
    this.showEditDialog = true;
  }

  updateTenant(): void {
    if (!this.editingTenant) return;
    this.saving.set(true);

    const statusChanged = this.editStatus !== this.editingTenant.status;
    const clusterChanged =
      this.editClusterId && this.editClusterId !== this.editingTenant.clusterId;

    const id = this.editingTenant.id;

    if (statusChanged && clusterChanged) {
      // Chain both calls
      this.tenantService.updateStatus(id, this.editStatus).subscribe({
        next: () => {
          this.tenantService.assignCluster(id, this.editClusterId).subscribe({
            next: () => this.onEditSuccess(),
            error: (err) => this.onEditError(err)
          });
        },
        error: (err) => this.onEditError(err)
      });
    } else if (statusChanged) {
      this.tenantService.updateStatus(id, this.editStatus).subscribe({
        next: () => this.onEditSuccess(),
        error: (err) => this.onEditError(err)
      });
    } else if (clusterChanged) {
      this.tenantService.assignCluster(id, this.editClusterId).subscribe({
        next: () => this.onEditSuccess(),
        error: (err) => this.onEditError(err)
      });
    } else {
      this.saving.set(false);
      this.showEditDialog = false;
    }
  }

  private onEditSuccess(): void {
    this.showEditDialog = false;
    this.saving.set(false);
    this.messageService.add({
      severity: 'success',
      summary: 'Updated',
      detail: 'Tenant updated successfully'
    });
    this.loadTenants(0);
  }

  private onEditError(err: unknown): void {
    this.saving.set(false);
    const e = err as { error?: { error?: string } };
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: e?.error?.error || 'Failed to update tenant'
    });
  }

  confirmDelete(tenant: Tenant): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete tenant "<strong>${tenant.name}</strong>"? This action cannot be undone.`,
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.tenantService.deleteTenant(tenant.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Deleted',
              detail: `Tenant "${tenant.name}" deleted`
            });
            this.loadTenants(0);
          },
          error: (err) => {
            const e = err as { error?: { error?: string } };
            this.messageService.add({
              severity: 'error',
              summary: 'Error',
              detail: e?.error?.error || 'Failed to delete tenant'
            });
          }
        });
      }
    });
  }

  openUsersDialog(tenant: Tenant): void {
    this.selectedTenant = tenant;
    this.addUserId = '';
    this.addUserRole = 'MEMBER';
    this.showUsersDialog = true;
    this.loadTenantUsers(tenant.id);
  }

  loadTenantUsers(tenantId: string): void {
    this.loadingUsers.set(true);
    this.tenantService.getTenantUsers(tenantId).subscribe({
      next: (users) => {
        this.tenantUsers.set(users);
        this.loadingUsers.set(false);
      },
      error: () => {
        this.loadingUsers.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load tenant users'
        });
      }
    });
  }

  assignUser(): void {
    if (!this.selectedTenant || !this.addUserId.trim()) return;
    this.savingUser.set(true);
    this.tenantService
      .assignUser(this.selectedTenant.id, this.addUserId.trim(), this.addUserRole)
      .subscribe({
        next: () => {
          this.savingUser.set(false);
          this.addUserId = '';
          this.messageService.add({
            severity: 'success',
            summary: 'Added',
            detail: 'User added to tenant'
          });
          this.loadTenantUsers(this.selectedTenant!.id);
        },
        error: (err) => {
          this.savingUser.set(false);
          const e = err as { error?: { error?: string } };
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: e?.error?.error || 'Failed to add user'
          });
        }
      });
  }

  removeUserFromTenant(membership: TenantMembership): void {
    if (!this.selectedTenant) return;
    this.confirmationService.confirm({
      message: `Remove user from tenant "${this.selectedTenant.name}"?`,
      header: 'Confirm',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.tenantService
          .removeUser(this.selectedTenant!.id, membership.userId)
          .subscribe({
            next: () => {
              this.messageService.add({
                severity: 'success',
                summary: 'Removed',
                detail: 'User removed from tenant'
              });
              this.loadTenantUsers(this.selectedTenant!.id);
            },
            error: (err) => {
              const e = err as { error?: { error?: string } };
              this.messageService.add({
                severity: 'error',
                summary: 'Error',
                detail: e?.error?.error || 'Failed to remove user'
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
      case 'ARCHIVED':
        return 'danger';
      default:
        return 'secondary';
    }
  }
}
