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
import { TagModule } from 'primeng/tag';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ClusterService } from '../../core/services/cluster.service';
import { Cluster } from '../../core/models/api.model';

type TagSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

@Component({
  selector: 'app-clusters',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
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
      <h2 class="m-0">Clusters</h2>
      <p-button label="New Cluster" icon="pi pi-plus" (click)="openCreateDialog()" />
    </div>

    <p-table
      [value]="clusters()"
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
          <th>URL</th>
          <th>Status</th>
          <th>Created</th>
          <th style="width: 9rem">Actions</th>
        </tr>
      </ng-template>
      <ng-template pTemplate="body" let-cluster>
        <tr>
          <td class="font-medium">{{ cluster.name }}</td>
          <td>
            <a [href]="cluster.url" target="_blank" class="text-primary no-underline hover:underline">
              {{ cluster.url }}
            </a>
          </td>
          <td>
            <p-tag
              [value]="cluster.status"
              [severity]="cluster.status === 'ACTIVE' ? 'success' : 'warn'"
            />
          </td>
          <td>{{ cluster.createdAt | date: 'short' }}</td>
          <td>
            <p-button
              icon="pi pi-pencil"
              [rounded]="true"
              [text]="true"
              severity="info"
              (click)="openEditDialog(cluster)"
              pTooltip="Edit cluster"
            />
            <p-button
              icon="pi pi-trash"
              [rounded]="true"
              [text]="true"
              severity="danger"
              (click)="confirmDelete(cluster)"
              pTooltip="Delete cluster"
            />
          </td>
        </tr>
      </ng-template>
      <ng-template pTemplate="emptymessage">
        <tr>
          <td colspan="5" class="text-center p-4 text-color-secondary">No clusters found.</td>
        </tr>
      </ng-template>
    </p-table>

    <!-- Create Dialog -->
    <p-dialog
      header="New Cluster"
      [(visible)]="showCreateDialog"
      [modal]="true"
      [style]="{ width: '450px' }"
      [draggable]="false"
    >
      <div class="field mb-3">
        <label class="block mb-2 font-medium">Name <span class="text-red-500">*</span></label>
        <input
          pInputText
          [(ngModel)]="form.name"
          class="w-full"
          placeholder="e.g. us-east-1"
        />
      </div>
      <div class="field mb-3">
        <label class="block mb-2 font-medium">URL <span class="text-red-500">*</span></label>
        <input
          pInputText
          [(ngModel)]="form.url"
          class="w-full"
          placeholder="https://cluster.example.com"
          type="url"
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
          (click)="createCluster()"
          [loading]="saving()"
          [disabled]="!form.name.trim() || !form.url.trim()"
        />
      </ng-template>
    </p-dialog>

    <!-- Edit Dialog -->
    <p-dialog
      header="Edit Cluster: {{ editingCluster?.name }}"
      [(visible)]="showEditDialog"
      [modal]="true"
      [style]="{ width: '450px' }"
      [draggable]="false"
    >
      <div class="field mb-3">
        <label class="block mb-2 font-medium">Name <span class="text-red-500">*</span></label>
        <input
          pInputText
          [(ngModel)]="editForm.name"
          class="w-full"
          placeholder="Cluster name"
        />
      </div>
      <div class="field mb-3">
        <label class="block mb-2 font-medium">URL <span class="text-red-500">*</span></label>
        <input
          pInputText
          [(ngModel)]="editForm.url"
          class="w-full"
          placeholder="https://cluster.example.com"
          type="url"
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
          (click)="updateCluster()"
          [loading]="saving()"
          [disabled]="!editForm.name.trim() || !editForm.url.trim()"
        />
      </ng-template>
    </p-dialog>
  `
})
export class ClustersComponent implements OnInit {
  private clusterService = inject(ClusterService);
  private confirmationService = inject(ConfirmationService);
  private messageService = inject(MessageService);

  // Signals
  clusters = signal<Cluster[]>([]);
  loading = signal(false);
  saving = signal(false);
  totalRecords = signal(0);

  pageSize = 20;

  // Create dialog
  showCreateDialog = false;
  form = { name: '', url: '' };

  // Edit dialog
  showEditDialog = false;
  editingCluster: Cluster | null = null;
  editForm = { name: '', url: '' };

  ngOnInit(): void {
    this.loadClusters(0);
  }

  loadClusters(page: number): void {
    this.loading.set(true);
    this.clusterService.getClusters(page, this.pageSize).subscribe({
      next: (result) => {
        this.clusters.set(result.content);
        this.totalRecords.set(result.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load clusters'
        });
      }
    });
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    const page = Math.floor((event.first ?? 0) / (event.rows ?? this.pageSize));
    this.loadClusters(page);
  }

  openCreateDialog(): void {
    this.form = { name: '', url: '' };
    this.showCreateDialog = true;
  }

  createCluster(): void {
    if (!this.form.name.trim() || !this.form.url.trim()) return;
    this.saving.set(true);
    this.clusterService.createCluster(this.form.name.trim(), this.form.url.trim()).subscribe({
      next: () => {
        this.showCreateDialog = false;
        this.saving.set(false);
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Cluster created successfully'
        });
        this.loadClusters(0);
      },
      error: (err) => {
        this.saving.set(false);
        const e = err as { error?: { error?: string } };
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: e?.error?.error || 'Failed to create cluster'
        });
      }
    });
  }

  openEditDialog(cluster: Cluster): void {
    this.editingCluster = cluster;
    this.editForm = { name: cluster.name, url: cluster.url };
    this.showEditDialog = true;
  }

  updateCluster(): void {
    if (!this.editingCluster) return;
    if (!this.editForm.name.trim() || !this.editForm.url.trim()) return;
    this.saving.set(true);
    this.clusterService
      .updateCluster(this.editingCluster.id, this.editForm.name.trim(), this.editForm.url.trim())
      .subscribe({
        next: () => {
          this.showEditDialog = false;
          this.saving.set(false);
          this.messageService.add({
            severity: 'success',
            summary: 'Updated',
            detail: 'Cluster updated successfully'
          });
          this.loadClusters(0);
        },
        error: (err) => {
          this.saving.set(false);
          const e = err as { error?: { error?: string } };
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: e?.error?.error || 'Failed to update cluster'
          });
        }
      });
  }

  confirmDelete(cluster: Cluster): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete cluster "<strong>${cluster.name}</strong>"? This action cannot be undone.`,
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.clusterService.deleteCluster(cluster.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Deleted',
              detail: `Cluster "${cluster.name}" deleted`
            });
            this.loadClusters(0);
          },
          error: (err) => {
            const e = err as { error?: { error?: string } };
            this.messageService.add({
              severity: 'error',
              summary: 'Error',
              detail: e?.error?.error || 'Failed to delete cluster'
            });
          }
        });
      }
    });
  }
}
