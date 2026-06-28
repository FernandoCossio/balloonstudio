import { Component, model, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import type { ReservaResponse } from '../../interface/reserva.interface';

@Component({
    selector: 'app-detalle-reserva-dialog',
    standalone: true,
    imports: [CommonModule, DialogModule, ButtonModule, TagModule],
    templateUrl: './detalle-reserva-dialog.html',
    styleUrl: './detalle-reserva-dialog.scss'
})
export class DetalleReservaDialog {
    visible = model<boolean>(false);
    reserva = input<ReservaResponse | null>(null);
    descargarRecibo = output<ReservaResponse>();

    getEstadoSeverity(estado?: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
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

    getEstadoLabel(estado?: string): string {
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
