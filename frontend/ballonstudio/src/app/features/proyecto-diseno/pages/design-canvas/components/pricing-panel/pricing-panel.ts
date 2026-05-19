// pricing-panel.ts
import { Component, inject } from '@angular/core';
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
}