import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import type { ReservaResponse } from '../../interface/reserva.interface';

@Component({
    selector: 'app-reserva-card',
    standalone: true,
    imports: [CommonModule, ButtonModule, TagModule],
    templateUrl: './reserva-card.html',
    styleUrl: './reserva-card.scss'
})
export class ReservaCard {
    reserva = input.required<ReservaResponse>();
    verDetalle = output<ReservaResponse>();
    descargarRecibo = output<ReservaResponse>();

    getEstadoSeverity(estado: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
        switch (estado?.toUpperCase()) {
            case 'CONFIRMADA':
                return 'success';
            case 'PENDIENTE_PAGO':
                return 'warn';
            case 'EXPIRADA':
                return 'secondary';
            case 'CANCELADA':
                return 'danger';
            default:
                return 'info';
        }
    }

    getEstadoLabel(estado: string): string {
        switch (estado?.toUpperCase()) {
            case 'CONFIRMADA':
                return 'Confirmada';
            case 'PENDIENTE_PAGO':
                return 'Pendiente Pago';
            case 'EXPIRADA':
                return 'Expirada';
            case 'CANCELADA':
                return 'Cancelada';
            default:
                return estado || 'Desconocido';
        }
    }
}
