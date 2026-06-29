import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { ReportesService, UsuariosReporteData } from '../../service/reportes.service';

@Component({
    selector: 'app-reporte-clientes',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        TableModule,
        ButtonModule,
        TagModule,
        ToastModule,
        TooltipModule
    ],
    providers: [MessageService],
    templateUrl: './reporte-clientes.html',
    styleUrl: './reporte-clientes.scss'
})
export class ReporteClientes implements OnInit {
    private reportesService = inject(ReportesService);
    private messageService = inject(MessageService);

    // State signals
    readonly loading = signal<boolean>(false);
    readonly rawClientes = signal<UsuariosReporteData[]>([]);
    readonly searchQuery = signal<string>('');
    readonly activeFilter = signal<boolean | null>(null);

    // Date filters (optional, defaults to empty to see all registered customers)
    fechaInicio = '';
    fechaFin = '';

    ngOnInit(): void {
        this.cargarDatos();
    }

    cargarDatos(): void {
        this.loading.set(true);
        // Default role is hardcoded to 'CLIENTE' as per request
        const status = this.activeFilter();
        this.reportesService.getUsuariosDatos(
            'CLIENTE',
            status === null ? undefined : status,
            this.searchQuery() || undefined,
            this.fechaInicio || undefined,
            this.fechaFin || undefined
        ).subscribe({
            next: (data) => {
                this.rawClientes.set(data || []);
                this.loading.set(false);
            },
            error: (err) => {
                this.loading.set(false);
                this.messageService.add({
                    severity: 'error',
                    summary: 'Error',
                    detail: 'No se pudieron cargar los datos del reporte de clientes.'
                });
                console.error(err);
            }
        });
    }

    onFiltrosChange(): void {
        this.cargarDatos();
    }

    limpiarFiltros(): void {
        this.fechaInicio = '';
        this.fechaFin = '';
        this.activeFilter.set(null);
        this.searchQuery.set('');
        this.cargarDatos();
    }

    // Dynamic computations for client list
    readonly clientesFiltrados = computed(() => {
        return this.rawClientes();
    });

    // KPI cards
    readonly kpiTotalClientes = computed(() => {
        return this.rawClientes().length;
    });

    readonly kpiClientesActivos = computed(() => {
        return this.rawClientes().filter(c => c.activo).length;
    });

    readonly kpiClientesInactivos = computed(() => {
        return this.rawClientes().filter(c => !c.activo).length;
    });

    exportar(formato: 'pdf' | 'excel'): void {
        this.messageService.add({
            severity: 'info',
            summary: 'Exportando',
            detail: `Generando reporte de clientes en formato ${formato.toUpperCase()}...`
        });

        const status = this.activeFilter();
        this.reportesService.descargarUsuarios(
            formato,
            'CLIENTE',
            status === null ? undefined : status,
            this.searchQuery() || undefined,
            this.fechaInicio || undefined,
            this.fechaFin || undefined
        ).subscribe({
            next: (blob: Blob) => {
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `reporte-clientes-${new Date().toISOString().substring(0,10)}.${formato === 'excel' ? 'xlsx' : 'pdf'}`;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);

                this.messageService.add({
                    severity: 'success',
                    summary: 'Exportación Exitosa',
                    detail: `El reporte de clientes en ${formato.toUpperCase()} fue descargado.`
                });
            },
            error: (err) => {
                this.messageService.add({
                    severity: 'error',
                    summary: 'Error al exportar',
                    detail: `No se pudo generar el reporte de clientes en ${formato.toUpperCase()}.`
                });
                console.error(err);
            }
        });
    }

    getStatusSeverity(activo: boolean): 'success' | 'danger' {
        return activo ? 'success' : 'danger';
    }

    getStatusLabel(activo: boolean): string {
        return activo ? 'Activo' : 'Inactivo';
    }
}
