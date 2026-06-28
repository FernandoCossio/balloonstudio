import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { PaginatorModule } from 'primeng/paginator';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { Observable } from 'rxjs';
import { AuthService } from '../../../auth/service/auth.service';
import { ReservaService } from '../../service/reserva.service';
import { ReservaCard } from '../../components/reserva-card/reserva-card';
import { DetalleReservaDialog } from '../../components/detalle-reserva-dialog/detalle-reserva-dialog';
import type { Page, ReservaResponse } from '../../interface/reserva.interface';

@Component({
    selector: 'app-reserva-list-page',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        ButtonModule,
        PaginatorModule,
        ToastModule,
        ReservaCard,
        DetalleReservaDialog
    ],
    providers: [MessageService],
    templateUrl: './reserva-list-page.html',
    styleUrl: './reserva-list-page.scss'
})
export class ReservaListPage implements OnInit {
    private authService = inject(AuthService);
    private reservaService = inject(ReservaService);
    private messageService = inject(MessageService);

    reservas = signal<ReservaResponse[]>([]);
    loading = signal<boolean>(false);
    totalElements = signal<number>(0);
    page = signal<number>(0);
    size = signal<number>(10);
    sort = signal<string>('fechaReserva,desc');

    // Filtros
    searchQuery = signal<string>('');
    selectedEstadoFilter = signal<string>('');
    fechaInicioFilter = signal<string>('');
    fechaFinFilter = signal<string>('');

    // Modal Detalle
    selectedReserva = signal<ReservaResponse | null>(null);
    detailVisible = signal<boolean>(false);

    ngOnInit() {
        this.loadReservas();
    }

    isAdmin(): boolean {
        return this.authService.hasRole('role_administrador');
    }

    isEmpleado(): boolean {
        return this.authService.hasRole('role_empleado');
    }

    isCliente(): boolean {
        return this.authService.hasRole('role_cliente');
    }

    loadReservas() {
        this.loading.set(true);
        const nameQuery = this.searchQuery();
        const estado = this.selectedEstadoFilter();
        const start = this.fechaInicioFilter() ? new Date(this.fechaInicioFilter()).toISOString() : '';
        const end = this.fechaFinFilter() ? new Date(this.fechaFinFilter()).toISOString() : '';
        const pageNum = this.page();
        const pageSize = this.size();
        const sortVal = this.sort();

        let obs$: Observable<Page<ReservaResponse>>;

        if (this.isAdmin()) {
            obs$ = this.reservaService.findReservasAdmin(nameQuery, estado, start, end, pageNum, pageSize, sortVal);
        } else if (this.isEmpleado()) {
            obs$ = this.reservaService.findReservasEmpleado(nameQuery, start, end, pageNum, pageSize, sortVal);
        } else {
            obs$ = this.reservaService.findReservasCliente(estado, start, end, pageNum, pageSize, sortVal);
        }

        obs$.subscribe({
            next: (data) => {
                this.reservas.set(data.content || []);
                this.totalElements.set(data.totalElements || 0);
                this.loading.set(false);
            },
            error: (err) => {
                this.messageService.add({
                    severity: 'error',
                    summary: 'Error',
                    detail: 'No se pudieron cargar las reservas.'
                });
                this.loading.set(false);
            }
        });
    }

    onSearchChange(val: string) {
        this.searchQuery.set(val);
        this.page.set(0);
        this.loadReservas();
    }

    onEstadoChange(val: string) {
        this.selectedEstadoFilter.set(val);
        this.page.set(0);
        this.loadReservas();
    }

    onFechaInicioChange(val: string) {
        this.fechaInicioFilter.set(val);
        this.page.set(0);
        this.loadReservas();
    }

    onFechaFinChange(val: string) {
        this.fechaFinFilter.set(val);
        this.page.set(0);
        this.loadReservas();
    }

    limpiarFiltros() {
        this.searchQuery.set('');
        this.selectedEstadoFilter.set('');
        this.fechaInicioFilter.set('');
        this.fechaFinFilter.set('');
        this.page.set(0);
        this.loadReservas();
    }

    onPageChange(event: any) {
        this.page.set(event.page);
        this.size.set(event.rows);
        this.loadReservas();
    }

    verDetalle(reserva: ReservaResponse) {
        this.selectedReserva.set(reserva);
        this.detailVisible.set(true);
    }

    descargarRecibo(reserva: ReservaResponse) {
        this.messageService.add({
            severity: 'info',
            summary: 'Descarga',
            detail: `Descarga de recibo para reserva #${reserva.id} iniciada (Simulado).`
        });
    }
}
