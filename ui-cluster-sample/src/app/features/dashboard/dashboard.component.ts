import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToolbarModule } from 'primeng/toolbar';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { DividerModule } from 'primeng/divider';
import { ConfirmationService, MessageService } from 'primeng/api';
import { AuthService } from '../../core/services/auth.service';
import { ClusterApiService } from '../../core/services/cluster.service';
import { FullContext, TestRecord } from '../../core/models/api.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ToolbarModule,
    CardModule,
    TableModule,
    ButtonModule,
    TagModule,
    DialogModule,
    InputTextModule,
    InputNumberModule,
    ToastModule,
    ConfirmDialogModule,
    ProgressSpinnerModule,
    DividerModule
  ],
  providers: [MessageService, ConfirmationService],
  template: `
    <p-toast position="top-right"/>
    <p-confirmDialog/>

    <!-- Top Toolbar -->
    <p-toolbar styleClass="surface-card border-none border-bottom-1 surface-border px-4">
      <ng-template pTemplate="start">
        <div class="flex align-items-center gap-2">
          <i class="pi pi-server text-primary text-2xl"></i>
          <span class="text-xl font-bold text-primary">Tenant Sample App</span>
        </div>
      </ng-template>
      <ng-template pTemplate="end">
        <div class="flex align-items-center gap-3">
          <div class="flex align-items-center gap-2">
            <i class="pi pi-user text-color-secondary"></i>
            <span class="text-sm text-color-secondary">{{ auth.getCurrentUser()?.email }}</span>
          </div>
          <p-button
            label="Logout"
            icon="pi pi-sign-out"
            severity="secondary"
            size="small"
            (click)="auth.logout()"
          />
        </div>
      </ng-template>
    </p-toolbar>

    <div class="p-4">

      <!-- Context Loading Spinner -->
      @if (loadingContext()) {
        <div class="flex justify-content-center align-items-center p-6">
          <div class="text-center">
            <p-progressSpinner strokeWidth="4" styleClass="w-4rem h-4rem"/>
            <p class="text-color-secondary mt-3">Loading context...</p>
          </div>
        </div>
      }

      <!-- Context Error -->
      @if (!loadingContext() && contextError()) {
        <div class="surface-card border-round border-1 border-red-300 p-4 mb-4">
          <div class="flex align-items-center gap-2 text-red-500">
            <i class="pi pi-exclamation-triangle"></i>
            <span class="font-medium">Failed to load context: {{ contextError() }}</span>
          </div>
        </div>
      }

      <!-- Context Cards -->
      @if (!loadingContext() && context()) {
        <div class="grid mb-4">

          <!-- Tenant Card -->
          <div class="col-12 md:col-4">
            <p-card styleClass="h-full context-card shadow-1">
              <ng-template pTemplate="header">
                <div class="flex align-items-center gap-2 px-4 pt-4 pb-0">
                  <i class="pi pi-building text-primary"></i>
                  <span class="font-semibold text-lg">Tenant</span>
                </div>
              </ng-template>
              <div class="flex flex-column gap-3">
                <div class="flex flex-column gap-1">
                  <span class="text-xs text-color-secondary font-medium uppercase">Name</span>
                  <span class="font-medium">{{ context()!.tenant.name }}</span>
                </div>
                <div class="flex flex-column gap-1">
                  <span class="text-xs text-color-secondary font-medium uppercase">Status</span>
                  <div>
                    <p-tag
                      [value]="context()!.tenant.status"
                      [severity]="context()!.tenant.status === 'ACTIVE' ? 'success' : 'danger'"
                    />
                  </div>
                </div>
                <div class="flex flex-column gap-1">
                  <span class="text-xs text-color-secondary font-medium uppercase">Tenant ID</span>
                  <span class="font-medium text-sm font-italic">{{ context()!.tenant.id }}</span>
                </div>
                <div class="flex flex-column gap-1">
                  <span class="text-xs text-color-secondary font-medium uppercase">Assigned Cluster</span>
                  <span class="font-medium">{{ context()!.tenant.clusterName }}</span>
                </div>
              </div>
            </p-card>
          </div>

          <!-- User Card -->
          <div class="col-12 md:col-4">
            <p-card styleClass="h-full context-card shadow-1">
              <ng-template pTemplate="header">
                <div class="flex align-items-center gap-2 px-4 pt-4 pb-0">
                  <i class="pi pi-user text-primary"></i>
                  <span class="font-semibold text-lg">User</span>
                </div>
              </ng-template>
              <div class="flex flex-column gap-3">
                <div class="flex flex-column gap-1">
                  <span class="text-xs text-color-secondary font-medium uppercase">Email</span>
                  <span class="font-medium">{{ context()!.user.email }}</span>
                </div>
                <div class="flex flex-column gap-1">
                  <span class="text-xs text-color-secondary font-medium uppercase">Name</span>
                  <span class="font-medium">{{ context()!.user.firstName }} {{ context()!.user.lastName }}</span>
                </div>
                <div class="flex flex-column gap-1">
                  <span class="text-xs text-color-secondary font-medium uppercase">User Type</span>
                  <span class="font-medium">
                    <p-tag [value]="context()!.user.userType" severity="info"/>
                  </span>
                </div>
                @if (context()!.user.roles && context()!.user.roles.length > 0) {
                  <div class="flex flex-column gap-1">
                    <span class="text-xs text-color-secondary font-medium uppercase">Roles</span>
                    <div class="flex flex-wrap gap-1">
                      @for (role of context()!.user.roles; track role) {
                        <p-tag [value]="role" severity="secondary"/>
                      }
                    </div>
                  </div>
                }
              </div>
            </p-card>
          </div>

          <!-- Cluster & Shard Card -->
          <div class="col-12 md:col-4">
            <p-card styleClass="h-full context-card shadow-1">
              <ng-template pTemplate="header">
                <div class="flex align-items-center gap-2 px-4 pt-4 pb-0">
                  <i class="pi pi-database text-primary"></i>
                  <span class="font-semibold text-lg">Cluster &amp; Shard</span>
                </div>
              </ng-template>
              <div class="flex flex-column gap-3">
                <div class="flex flex-column gap-1">
                  <span class="text-xs text-color-secondary font-medium uppercase">Cluster Name</span>
                  <span class="font-medium">{{ context()!.cluster.name }}</span>
                </div>
                <div class="flex flex-column gap-1">
                  <span class="text-xs text-color-secondary font-medium uppercase">Cluster Status</span>
                  <div>
                    <p-tag
                      [value]="context()!.cluster.status"
                      [severity]="context()!.cluster.status === 'ACTIVE' ? 'success' : 'danger'"
                    />
                  </div>
                </div>
                <div class="flex flex-column gap-1">
                  <span class="text-xs text-color-secondary font-medium uppercase">Cluster URL</span>
                  <span class="font-medium text-sm">{{ context()!.cluster.url }}</span>
                </div>
                <p-divider styleClass="my-1"/>
                <div class="flex flex-column gap-1">
                  <span class="text-xs text-color-secondary font-medium uppercase">Shard ID</span>
                  <span class="font-medium font-italic text-sm">{{ context()!.localContext.shardId }}</span>
                </div>
                <div class="flex flex-column gap-1">
                  <span class="text-xs text-color-secondary font-medium uppercase">Schema Name</span>
                  <span class="font-medium font-italic text-sm">{{ context()!.localContext.schemaName }}</span>
                </div>
              </div>
            </p-card>
          </div>

        </div>
      }

      <!-- Test Records Section -->
      <div class="surface-card border-round shadow-1 p-4">
        <div class="flex justify-content-between align-items-center mb-4">
          <div class="flex align-items-center gap-2">
            <i class="pi pi-list text-primary text-xl"></i>
            <h3 class="m-0 text-xl font-semibold">Test Records</h3>
            @if (!loadingRecords() && testRecords().length > 0) {
              <span class="text-color-secondary text-sm">({{ testRecords().length }})</span>
            }
          </div>
          <div class="flex gap-2">
            <p-button
              icon="pi pi-refresh"
              severity="secondary"
              size="small"
              [rounded]="true"
              [text]="true"
              pTooltip="Refresh"
              (click)="loadRecords()"
              [loading]="loadingRecords()"
            />
            <p-button
              label="New Record"
              icon="pi pi-plus"
              size="small"
              (click)="openCreateDialog()"
            />
          </div>
        </div>

        <p-table
          [value]="testRecords()"
          [loading]="loadingRecords()"
          responsiveLayout="scroll"
          styleClass="p-datatable-sm"
          [paginator]="testRecords().length > 10"
          [rows]="10"
          [showCurrentPageReport]="true"
          currentPageReportTemplate="Showing {first} to {last} of {totalRecords} records"
        >
          <ng-template pTemplate="header">
            <tr>
              <th style="width: 20%">Name</th>
              <th style="width: 35%">Description</th>
              <th style="width: 10%">Value</th>
              <th style="width: 25%">Created</th>
              <th style="width: 10%" class="text-center">Actions</th>
            </tr>
          </ng-template>

          <ng-template pTemplate="body" let-record>
            <tr>
              <td>
                <span class="font-medium">{{ record.name }}</span>
              </td>
              <td>
                <span class="text-color-secondary">{{ record.description || '—' }}</span>
              </td>
              <td>
                @if (record.value !== null && record.value !== undefined) {
                  <span class="font-medium">{{ record.value }}</span>
                } @else {
                  <span class="text-color-secondary">—</span>
                }
              </td>
              <td>
                <span class="text-sm text-color-secondary">{{ record.createdAt | date:'medium' }}</span>
              </td>
              <td class="text-center">
                <p-button
                  icon="pi pi-trash"
                  [rounded]="true"
                  [text]="true"
                  severity="danger"
                  size="small"
                  pTooltip="Delete record"
                  (click)="confirmDelete(record)"
                />
              </td>
            </tr>
          </ng-template>

          <ng-template pTemplate="emptymessage">
            <tr>
              <td colspan="5" class="text-center py-5">
                <div class="flex flex-column align-items-center gap-2 text-color-secondary">
                  <i class="pi pi-inbox text-4xl"></i>
                  <span>No test records found</span>
                  <p-button
                    label="Create your first record"
                    link="true"
                    (click)="openCreateDialog()"
                  />
                </div>
              </td>
            </tr>
          </ng-template>

          <ng-template pTemplate="loadingbody">
            <tr>
              <td colspan="5" class="text-center py-5">
                <p-progressSpinner strokeWidth="4" styleClass="w-3rem h-3rem"/>
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>

    </div>

    <!-- Create Record Dialog -->
    <p-dialog
      header="New Test Record"
      [(visible)]="showCreateDialog"
      [modal]="true"
      [style]="{ width: '420px' }"
      [draggable]="false"
      [resizable]="false"
    >
      <div class="flex flex-column gap-4 pt-2">
        <div class="field">
          <label for="rec-name" class="block font-medium mb-2">
            Name <span class="text-red-500">*</span>
          </label>
          <input
            id="rec-name"
            pInputText
            [(ngModel)]="newRecord.name"
            placeholder="Enter record name"
            class="w-full"
            [disabled]="saving()"
          />
        </div>

        <div class="field">
          <label for="rec-description" class="block font-medium mb-2">Description</label>
          <input
            id="rec-description"
            pInputText
            [(ngModel)]="newRecord.description"
            placeholder="Optional description"
            class="w-full"
            [disabled]="saving()"
          />
        </div>

        <div class="field">
          <label for="rec-value" class="block font-medium mb-2">Value</label>
          <p-inputNumber
            inputId="rec-value"
            [(ngModel)]="newRecord.value"
            placeholder="Optional numeric value"
            styleClass="w-full"
            inputStyleClass="w-full"
            [disabled]="saving()"
          />
        </div>
      </div>

      <ng-template pTemplate="footer">
        <div class="flex justify-content-end gap-2">
          <p-button
            label="Cancel"
            severity="secondary"
            [disabled]="saving()"
            (click)="closeCreateDialog()"
          />
          <p-button
            label="Create"
            icon="pi pi-check"
            [loading]="saving()"
            [disabled]="!newRecord.name || saving()"
            (click)="createRecord()"
          />
        </div>
      </ng-template>
    </p-dialog>
  `
})
export class DashboardComponent implements OnInit {
  auth = inject(AuthService);
  private clusterApi = inject(ClusterApiService);
  private confirmationService = inject(ConfirmationService);
  private messageService = inject(MessageService);

  context = signal<FullContext | null>(null);
  testRecords = signal<TestRecord[]>([]);
  loadingContext = signal(false);
  loadingRecords = signal(false);
  contextError = signal<string | null>(null);
  showCreateDialog = false;
  saving = signal(false);
  newRecord: { name: string; description: string; value: number | null } = {
    name: '',
    description: '',
    value: null
  };

  ngOnInit(): void {
    this.loadContext();
    this.loadRecords();
  }

  loadContext(): void {
    this.loadingContext.set(true);
    this.contextError.set(null);
    this.clusterApi.getContext().subscribe({
      next: (ctx) => {
        this.context.set(ctx);
        this.loadingContext.set(false);
      },
      error: (err) => {
        this.loadingContext.set(false);
        const msg = err?.error?.error || err?.message || 'Failed to load context';
        this.contextError.set(msg);
        this.messageService.add({
          severity: 'error',
          summary: 'Context Error',
          detail: msg
        });
      }
    });
  }

  loadRecords(): void {
    this.loadingRecords.set(true);
    this.clusterApi.getTestRecords().subscribe({
      next: (records) => {
        this.testRecords.set(records);
        this.loadingRecords.set(false);
      },
      error: (err) => {
        this.loadingRecords.set(false);
        const msg = err?.error?.error || err?.message || 'Failed to load records';
        this.messageService.add({
          severity: 'error',
          summary: 'Load Error',
          detail: msg
        });
      }
    });
  }

  openCreateDialog(): void {
    this.newRecord = { name: '', description: '', value: null };
    this.showCreateDialog = true;
  }

  closeCreateDialog(): void {
    this.showCreateDialog = false;
  }

  createRecord(): void {
    if (!this.newRecord.name || this.saving()) {
      return;
    }

    this.saving.set(true);

    const payload: { name: string; description?: string; value?: number } = {
      name: this.newRecord.name
    };
    if (this.newRecord.description) {
      payload.description = this.newRecord.description;
    }
    if (this.newRecord.value !== null && this.newRecord.value !== undefined) {
      payload.value = this.newRecord.value;
    }

    this.clusterApi.createTestRecord(payload).subscribe({
      next: () => {
        this.saving.set(false);
        this.showCreateDialog = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Created',
          detail: `Record "${this.newRecord.name}" created successfully`
        });
        this.loadRecords();
      },
      error: (err) => {
        this.saving.set(false);
        const msg = err?.error?.error || err?.message || 'Failed to create record';
        this.messageService.add({
          severity: 'error',
          summary: 'Create Error',
          detail: msg
        });
      }
    });
  }

  confirmDelete(record: TestRecord): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete "<strong>${record.name}</strong>"?`,
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.clusterApi.deleteTestRecord(record.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Deleted',
              detail: `Record "${record.name}" deleted`
            });
            this.loadRecords();
          },
          error: (err) => {
            const msg = err?.error?.error || err?.message || 'Failed to delete record';
            this.messageService.add({
              severity: 'error',
              summary: 'Delete Error',
              detail: msg
            });
          }
        });
      }
    });
  }
}
