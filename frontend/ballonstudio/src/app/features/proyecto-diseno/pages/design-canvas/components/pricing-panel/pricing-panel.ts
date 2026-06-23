import { Component, inject, signal, computed, Output, EventEmitter } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { CanvasStateService } from '../../../../services/canvas-state.service';
import { MessageService } from 'primeng/api';

import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'app-pricing-panel',
  imports: [DecimalPipe, ToastModule],
  templateUrl: './pricing-panel.html',
  styleUrl: './pricing-panel.scss',
  providers: [MessageService]
})
export class PricingPanel {

  readonly canvasState   = inject(CanvasStateService);
  private messageService = inject(MessageService);

  @Output() onReservar = new EventEmitter<void>();

  // ── Acciones ─────────────────────────────────────────────────────────────

  realizarReserva(): void {
    const proyecto = this.canvasState.proyectoActual();
    if (!proyecto) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Sin proyecto',
        detail: 'No hay un proyecto de diseño activo para reservar'
      });
      return;
    }

    if (this.canvasState.items().length === 0) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Lienzo vacío',
        detail: 'Debes añadir al menos un artículo para reservar'
      });
      return;
    }

    this.onReservar.emit();
  }
}