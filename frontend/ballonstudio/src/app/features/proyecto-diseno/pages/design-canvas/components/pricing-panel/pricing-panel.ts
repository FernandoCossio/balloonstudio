import { Component, inject, signal, computed } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { CanvasStateService } from '../../../../services/canvas-state.service';
import { Router } from '@angular/router';
import { AuthService } from '@/app/features/auth/service/auth.service';
import { ReservaService } from '../../../../services/reserva.service';
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
  private router         = inject(Router);
  private authService    = inject(AuthService);
  private reservaService = inject(ReservaService);
  private messageService = inject(MessageService);

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

    const payload = this.authService.getAccessTokenPayload();
    const uid = payload?.uid || 1; // Fallback a 1 para desarrollo/pruebas locales

    this.reservaService.iniciarReserva(proyecto.id, uid).subscribe({
      next: () => {
        this.router.navigate(['/proyectos', proyecto.id, 'reserva']);
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error al reservar',
          detail: err?.error?.message || 'No se pudo iniciar el proceso de reserva. Revisa el stock.'
        });
      }
    });
  }
}