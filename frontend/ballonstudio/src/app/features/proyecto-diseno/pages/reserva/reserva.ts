import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { CanvasStateService } from '../../services/canvas-state.service';
import { ReservaService } from '../../services/reserva.service';

@Component({
  selector: 'app-reserva',
  imports: [CommonModule, DecimalPipe],
  templateUrl: './reserva.html',
  styleUrl: './reserva.scss'
})
export class Reserva implements OnInit, OnDestroy {

  readonly canvasState = inject(CanvasStateService);
  private reservaService = inject(ReservaService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  // Estado local
  readonly showArticulos = signal<boolean>(false);
  readonly selectedMethod = signal<string>('tarjeta');
  readonly timeRemainingSeconds = signal<number>(900); // 15 mins
  readonly isExpired = signal<boolean>(false);

  private timerIntervalId: any = null;

  // Selectores computados
  readonly totalOriginal = computed(() => {
    return this.reservaService.activeReserva()?.totalOriginal ?? 0;
  });

  readonly montoAnticipo = computed(() => {
    return this.reservaService.activeReserva()?.montoAnticipo ?? 0;
  });

  readonly costoArticulos = computed(() => {
    return this.reservaService.activeReserva()?.costoArticulos ?? 0;
  });

  readonly costoFlete = computed(() => {
    return this.reservaService.activeReserva()?.costoFlete ?? 0;
  });

  readonly costoArmado = computed(() => {
    return this.reservaService.activeReserva()?.costoArmado ?? 0;
  });

  readonly itemsCount = computed(() => {
    return this.canvasState.proyectoItemsCount();
  });

  readonly formattedTimeRemaining = computed(() => {
    const totalSecs = this.timeRemainingSeconds();
    if (totalSecs <= 0) return 'Expirado';
    const mins = Math.floor(totalSecs / 60);
    const secs = totalSecs % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  });

  ngOnInit(): void {
    // Si no hay una reserva activa en el servicio, intentar volver o redirigir
    const active = this.reservaService.activeReserva();
    if (!active) {
      const proyectoId = this.route.snapshot.paramMap.get('proyectoId');
      this.router.navigate(['/proyectos', proyectoId, 'canvas']);
      return;
    }

    // Calcular el tiempo restante inicial
    this.updateTimer();

    // Iniciar cuenta regresiva
    this.timerIntervalId = setInterval(() => {
      this.updateTimer();
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timerIntervalId) {
      clearInterval(this.timerIntervalId);
    }
  }

  private updateTimer(): void {
    const remaining = this.reservaService.getTiempoRestanteSegundos();
    this.timeRemainingSeconds.set(remaining);
    if (remaining <= 0) {
      this.isExpired.set(true);
      if (this.timerIntervalId) {
        clearInterval(this.timerIntervalId);
      }
    }
  }

  toggleArticulos(): void {
    this.showArticulos.update(v => !v);
  }

  selectMethod(method: string): void {
    if (method === 'tarjeta') {
      this.selectedMethod.set(method);
    }
  }

  procederAlPago(): void {
    if (this.isExpired()) return;
    const proyectoId = this.route.snapshot.paramMap.get('proyectoId');
    this.router.navigate(['/proyectos', proyectoId, 'reserva', 'pago']);
  }

  volverAlCanvas(): void {
    const proyectoId = this.route.snapshot.paramMap.get('proyectoId');
    this.router.navigate(['/proyectos', proyectoId, 'canvas']);
  }
}
