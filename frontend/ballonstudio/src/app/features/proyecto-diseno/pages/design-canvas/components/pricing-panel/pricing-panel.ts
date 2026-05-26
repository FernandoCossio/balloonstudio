// pricing-panel.ts
import { Component, inject, signal, computed } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { CanvasStateService } from '../../../../services/canvas-state.service';

@Component({
  selector: 'app-pricing-panel',
  imports: [DecimalPipe],
  templateUrl: './pricing-panel.html',
  styleUrl: './pricing-panel.scss'
})
export class PricingPanel {

  readonly canvasState = inject(CanvasStateService);

  // ── Ajustes configurables ────────────────────────────────────────────────

  /** Costo fijo de entrega y montaje (Bs.) */
  readonly deliverySetup = signal<number>(120);

  /** Porcentaje de impuesto aplicado al subtotal del canvas */
  readonly taxRate = signal<number>(8.5);

  // ── Cálculos derivados ───────────────────────────────────────────────────

  readonly taxAmount = computed(() =>
    this.canvasState.precioTotal() * (this.taxRate() / 100)
  );

  readonly grandTotal = computed(() =>
    this.canvasState.precioTotal() + this.deliverySetup() + this.taxAmount()
  );
}