import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { BadgeModule } from 'primeng/badge';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { SkeletonModule } from 'primeng/skeleton';
import { ConfirmationService, MessageService } from 'primeng/api';

import { EmpleadoResponse, EmpleadoService } from '../../service/empleado.service';
import { AuthService } from '@/app/features/auth/service/auth.service';

@Component({
    selector: 'app-empleado-list',
    standalone: true,
    imports: [
        CommonModule,
        RouterModule,
        FormsModule,
        ButtonModule,
        TableModule,
        TagModule,
        BadgeModule,
        TooltipModule,
        ConfirmDialogModule,
        ToastModule,
        SkeletonModule
    ],
    providers: [ConfirmationService, MessageService],
    templateUrl: './empleado-list.html',
    styleUrl: './empleado-list.scss'
})
export class EmpleadoList implements OnInit {
    private svc = inject(EmpleadoService);
    private auth = inject(AuthService);
    private confirmSvc = inject(ConfirmationService);
    private msgSvc = inject(MessageService);
    private router = inject(Router);

    // ─── Signals for State ───────────────────────────────────────────────
    empleados = signal<EmpleadoResponse[]>([]);
    totalRecords = signal(0);
    loading = signal(true);
    searchQuery = signal('');
    selectedRolFilter = signal<string>(''); // '', 'ADMINISTRADOR', 'EMPLEADO'
    rowsPerPage = signal(10);
    currentPage = signal(0);
    currentSort = signal('nombreCompleto,asc');

    // List of ID's currently undergoing activation/deactivation toggling
    togglingIds = signal<number[]>([]);

    // Metrics computed from a full background list load
    totalCount = signal(0);
    activeCount = signal(0);
    inactiveCount = signal(0);

    ngOnInit() {
        this.loadMetrics();
    }

    // Load full metrics in background (unfiltered count)
    loadMetrics() {
        this.svc.findEmpleados(undefined, undefined, 0, 1000).subscribe({
            next: (page) => {
                const list = page?.content || [];
                this.totalCount.set(list.length);
                this.activeCount.set(list.filter(e => e.activo).length);
                this.inactiveCount.set(list.filter(e => !e.activo).length);
            },
            error: (err) => {
                console.error('[EmpleadoList] Error loading metrics', err);
            }
        });
    }

    // Lazy load triggered by p-table paging, sorting or filtering
    loadLazy(event: TableLazyLoadEvent) {
        this.loading.set(true);
        
        const page = event.first !== undefined && event.rows ? Math.floor(event.first / event.rows) : 0;
        const size = event.rows || 10;
        this.rowsPerPage.set(size);
        this.currentPage.set(page);

        let sortStr = 'nombreCompleto,asc';
        if (event.sortField) {
            const dir = event.sortOrder === 1 ? 'asc' : 'desc';
            sortStr = `${event.sortField},${dir}`;
        }
        this.currentSort.set(sortStr);

        this.svc.findEmpleados(
            this.searchQuery(),
            this.selectedRolFilter() || undefined,
            page,
            size,
            sortStr
        ).subscribe({
            next: (res) => {
                this.empleados.set(res?.content || []);
                this.totalRecords.set(res?.totalElements || 0);
                this.loading.set(false);
            },
            error: (err) => {
                this.msgSvc.add({
                    severity: 'error',
                    summary: 'Error',
                    detail: 'No se pudieron cargar los empleados.'
                });
                this.empleados.set([]);
                this.totalRecords.set(0);
                this.loading.set(false);
            }
        });
    }

    // Trigger reload using current lazy load state
    reloadTable() {
        this.loading.set(true);
        this.svc.findEmpleados(
            this.searchQuery(),
            this.selectedRolFilter() || undefined,
            this.currentPage(),
            this.rowsPerPage(),
            this.currentSort()
        ).subscribe({
            next: (res) => {
                this.empleados.set(res?.content || []);
                this.totalRecords.set(res?.totalElements || 0);
                this.loading.set(false);
            },
            error: () => {
                this.empleados.set([]);
                this.totalRecords.set(0);
                this.loading.set(false);
            }
        });
    }

    onSearchChange(val: string) {
        this.searchQuery.set(val);
        this.currentPage.set(0);
        this.reloadTable();
    }

    setRolFilter(rol: string) {
        this.selectedRolFilter.set(rol);
        this.currentPage.set(0);
        this.reloadTable();
    }

    openNew() {
        this.router.navigate(['/empleados/nuevo']);
    }

    editEmpleado(emp: EmpleadoResponse) {
        this.router.navigate(['/empleados/editar', emp.id]);
    }

    // Logic to prevent self-deactivation
    canToggleState(emp: EmpleadoResponse): boolean {
        const loggedInUid = this.auth.getAccessTokenPayload()?.uid;
        return emp.id !== loggedInUid;
    }

    deactivateEmpleado(emp: EmpleadoResponse) {
        this.confirmSvc.confirm({
            message: `¿Estás seguro de que deseas desactivar a ${emp.nombreCompleto}? El empleado perderá acceso inmediato al sistema y se cerrarán todas sus sesiones activas.`,
            header: 'Confirmar Desactivación',
            icon: 'pi pi-user-minus text-red-500',
            acceptLabel: 'Desactivar',
            rejectLabel: 'Cancelar',
            acceptButtonStyleClass: 'p-button-danger p-button-text',
            rejectButtonStyleClass: 'p-button-text p-button-secondary',
            accept: () => {
                this.togglingIds.update(ids => [...ids, emp.id]);
                this.svc.deactivate(emp.id).subscribe({
                    next: () => {
                        this.msgSvc.add({
                            severity: 'success',
                            summary: 'Empleado Desactivado',
                            detail: `La cuenta de ${emp.nombreCompleto} ha sido desactivada.`
                        });
                        this.togglingIds.update(ids => ids.filter(id => id !== emp.id));
                        this.reloadTable();
                        this.loadMetrics();
                    },
                    error: (err) => {
                        this.msgSvc.add({
                            severity: 'error',
                            summary: 'Error',
                            detail: err?.error?.message || 'No se pudo desactivar el empleado.'
                        });
                        this.togglingIds.update(ids => ids.filter(id => id !== emp.id));
                    }
                });
            }
        });
    }

    activateEmpleado(emp: EmpleadoResponse) {
        this.togglingIds.update(ids => [...ids, emp.id]);
        this.svc.activate(emp.id).subscribe({
            next: () => {
                this.msgSvc.add({
                    severity: 'success',
                    summary: 'Empleado Activado',
                    detail: `La cuenta de ${emp.nombreCompleto} ha sido activada correctamente.`
                });
                this.togglingIds.update(ids => ids.filter(id => id !== emp.id));
                this.reloadTable();
                this.loadMetrics();
            },
            error: (err) => {
                this.msgSvc.add({
                    severity: 'error',
                    summary: 'Error',
                    detail: err?.error?.message || 'No se pudo activar el empleado.'
                });
                this.togglingIds.update(ids => ids.filter(id => id !== emp.id));
            }
        });
    }

    // Helper functions for initial-based colorful avatars
    getInitials(nombre: string): string {
        if (!nombre) return '??';
        const parts = nombre.trim().split(/\s+/);
        if (parts.length >= 2) {
            return (parts[0][0] + parts[1][0]).toUpperCase();
        }
        return parts[0].substring(0, 2).toUpperCase();
    }

    getAvatarColor(nombre: string): string {
        if (!nombre) return '#2196f3';
        let hash = 0;
        for (let i = 0; i < nombre.length; i++) {
            hash = nombre.charCodeAt(i) + ((hash << 5) - hash);
        }
        const h = Math.abs(hash % 360);
        // Beautiful vibrant tone (lightness 42% for white text legibility)
        return `hsl(${h}, 65%, 42%)`;
    }
}
