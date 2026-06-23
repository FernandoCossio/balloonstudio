import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { ReportesService, VentasReporteData } from '../../service/reportes.service';

@Component({
    selector: 'app-reportes-dashboard',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        DecimalPipe,
        TableModule,
        ButtonModule,
        TagModule,
        ToastModule
    ],
    providers: [MessageService],
    templateUrl: './reportes-dashboard.html',
    styleUrl: './reportes-dashboard.scss'
})
export class ReportesDashboard implements OnInit {
    private reportesService = inject(ReportesService);
    private messageService = inject(MessageService);

    // State signals
    readonly loading = signal<boolean>(false);
    readonly rawVentas = signal<VentasReporteData[]>([]);
    readonly searchQuery = signal<string>('');
    readonly selectedEstado = signal<string>('');
    
    // Dates filters
    fechaInicio = '';
    fechaFin = '';

    ngOnInit(): void {
        // Por defecto, filtrar el mes actual
        const now = new Date();
        const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
        const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);

        this.fechaInicio = this.formatDateForInput(firstDay);
        this.fechaFin = this.formatDateForInput(lastDay);

        this.cargarDatos();
    }

    private formatDateForInput(date: Date): string {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    cargarDatos(): void {
        this.loading.set(true);
        this.reportesService.getVentasDatos(this.fechaInicio, this.fechaFin, this.selectedEstado() || undefined).subscribe({
            next: (data) => {
                this.rawVentas.set(data || []);
                this.loading.set(false);
            },
            error: (err) => {
                this.loading.set(false);
                this.messageService.add({
                    severity: 'error',
                    summary: 'Error',
                    detail: 'No se pudieron cargar los datos del reporte de ventas.'
                });
                console.error(err);
            }
        });
    }

    onFiltrosChange(): void {
        this.cargarDatos();
    }

    limpiarFiltros(): void {
        const now = new Date();
        const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
        const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);

        this.fechaInicio = this.formatDateForInput(firstDay);
        this.fechaFin = this.formatDateForInput(lastDay);
        this.selectedEstado.set('');
        this.searchQuery.set('');
        this.cargarDatos();
    }

    // Filtrado en el cliente para la barra de búsqueda "Buscar cliente..."
    readonly ventasFiltradas = computed(() => {
        const query = this.searchQuery().toLowerCase().trim();
        const list = this.rawVentas();
        if (!query) return list;

        return list.filter(v => 
            v.clienteNombre.toLowerCase().includes(query) || 
            String(v.id).includes(query)
        );
    });

    // Cálculos dinámicos de KPI para el mes actual
    readonly kpiTotalVentasMes = computed(() => {
        const now = new Date();
        const currentYear = now.getFullYear();
        const currentMonth = now.getMonth();

        return this.rawVentas()
            .filter(v => {
                const date = new Date(v.fechaReserva);
                return date.getFullYear() === currentYear && date.getMonth() === currentMonth;
            })
            .reduce((sum, v) => sum + v.total, 0);
    });

    readonly kpiEventosMes = computed(() => {
        const now = new Date();
        const currentYear = now.getFullYear();
        const currentMonth = now.getMonth();

        return this.rawVentas()
            .filter(v => {
                const date = new Date(v.fechaReserva);
                return date.getFullYear() === currentYear && date.getMonth() === currentMonth;
            }).length;
    });

    readonly kpiPendientesCobro = computed(() => {
        const now = new Date();
        const currentYear = now.getFullYear();
        const currentMonth = now.getMonth();

        return this.rawVentas()
            .filter(v => {
                const date = new Date(v.fechaReserva);
                return date.getFullYear() === currentYear && 
                       date.getMonth() === currentMonth &&
                       (v.estado === 'PENDIENTE_PAGO' || v.estado === 'PENDIENTE');
            })
            .reduce((sum, v) => sum + v.total, 0);
    });

    exportar(formato: 'pdf' | 'excel'): void {
        this.messageService.add({
            severity: 'info',
            summary: 'Exportando',
            detail: `Generando reporte de ventas en formato ${formato.toUpperCase()}...`
        });

        this.reportesService.descargarVentas(
            formato,
            this.fechaInicio || undefined,
            this.fechaFin || undefined,
            this.selectedEstado() || undefined
        ).subscribe({
            next: (blob: Blob) => {
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `reporte-ventas-${new Date().toISOString().substring(0,10)}.${formato === 'excel' ? 'xlsx' : 'pdf'}`;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);

                this.messageService.add({
                    severity: 'success',
                    summary: 'Exportación Exitosa',
                    detail: `El reporte en ${formato.toUpperCase()} fue descargado.`
                });
            },
            error: (err) => {
                this.messageService.add({
                    severity: 'error',
                    summary: 'Error al exportar',
                    detail: `No se pudo generar el reporte en ${formato.toUpperCase()}.`
                });
                console.error(err);
            }
        });
    }

    getEstadoSeverity(estado: string): 'success' | 'info' | 'warn' | 'danger' {
        switch (estado.toUpperCase()) {
            case 'CONFIRMADA':
            case 'COMPLETADO':
            case 'PAGADO':
                return 'success';
            case 'PENDIENTE':
            case 'PENDIENTE_PAGO':
                return 'warn';
            case 'CANCELADA':
            case 'CANCELADO':
                return 'danger';
            default:
                return 'info';
        }
    }

    getEstadoLabel(estado: string): string {
        switch (estado.toUpperCase()) {
            case 'CONFIRMADA':
            case 'COMPLETADO':
            case 'PAGADO':
                return 'Completado';
            case 'PENDIENTE':
            case 'PENDIENTE_PAGO':
                return 'Pendiente';
            case 'CANCELADA':
            case 'CANCELADO':
                return 'Cancelado';
            case 'EXPIRADA':
                return 'Expirado';
            default:
                return estado;
        }
    }
}
