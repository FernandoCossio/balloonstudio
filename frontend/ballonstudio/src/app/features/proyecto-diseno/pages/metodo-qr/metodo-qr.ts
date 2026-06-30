import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { CanvasStateService } from '../../services/canvas-state.service';
import { ReservaService } from '../../services/reserva.service';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { AuthService } from '../../../auth/service/auth.service';

@Component({
  selector: 'app-metodo-qr',
  imports: [CommonModule, FormsModule, DecimalPipe, ToastModule],
  templateUrl: './metodo-qr.html',
  styleUrl: './metodo-qr.scss',
  providers: [MessageService]
})
export class MetodoQr implements OnInit, OnDestroy {

  readonly canvasState = inject(CanvasStateService);
  private reservaService = inject(ReservaService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private messageService = inject(MessageService);

  // Datos de cliente
  nombreCliente = '';
  ciCliente = '';
  telefonoCliente = '';
  correoCliente = '';

  // Estados de proceso
  readonly loadingQr = signal<boolean>(false);
  readonly verifying = signal<boolean>(false);
  readonly qrBase64 = signal<string>('');
  readonly transactionId = signal<string>('');
  readonly isConfirmed = signal<boolean>(false);
  readonly isExpired = signal<boolean>(false);
  readonly timeRemainingSeconds = signal<number>(900);
  readonly reservaId = signal<number>(0);

  private timerIntervalId: any = null;
  private pollIntervalId: any = null;

  // Montos y desgloses
  readonly totalOriginal = computed(() => {
    if (this.isConfirmed() && this.totalPagadoLocal > 0) {
      return this.totalPagadoLocal;
    }
    return this.reservaService.activeReserva()?.totalOriginal ?? 0;
  });

  private totalPagadoLocal = 0;

  readonly montoAnticipo = computed(() => {
    return this.reservaService.activeReserva()?.montoAnticipo ?? 0;
  });

  readonly formattedTimeRemaining = computed(() => {
    const totalSecs = this.timeRemainingSeconds();
    if (totalSecs <= 0) return 'Expirado';
    const mins = Math.floor(totalSecs / 60);
    const secs = totalSecs % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  });

  ngOnInit(): void {
    const active = this.reservaService.activeReserva();
    if (!active) {
      const proyectoId = this.route.snapshot.paramMap.get('proyectoId');
      this.router.navigate(['/proyectos', proyectoId, 'canvas']);
      return;
    }

    this.reservaId.set(active.reservaId);

    // Precompletar correo con datos de sesión si es un correo válido
    const payload = this.authService.getAccessTokenPayload();
    if (payload && payload.sub && payload.sub.includes('@')) {
      this.correoCliente = payload.sub;
    }

    // Calcular el tiempo restante inicial
    this.updateTimer();

    // Iniciar cuenta regresiva
    this.timerIntervalId = setInterval(() => {
      this.updateTimer();
    }, 1000);
  }

  ngOnDestroy(): void {
    this.clearAllIntervals();
  }

  private clearAllIntervals(): void {
    if (this.timerIntervalId) {
      clearInterval(this.timerIntervalId);
    }
    if (this.pollIntervalId) {
      clearInterval(this.pollIntervalId);
    }
  }

  private updateTimer(): void {
    const remaining = this.reservaService.getTiempoRestanteSegundos();
    this.timeRemainingSeconds.set(remaining);
    if (remaining <= 0) {
      this.isExpired.set(true);
      this.clearAllIntervals();
    }
  }

  isFormValid(): boolean {
    return this.nombreCliente.trim().length > 3 && this.ciCliente.trim().length >= 5;
  }

  generarQr(): void {
    if (this.isExpired() || !this.isFormValid()) return;

    this.loadingQr.set(true);

    const payload = {
      nombreCliente: this.nombreCliente,
      ciCliente: this.ciCliente,
      telefonoCliente: this.telefonoCliente,
      correoCliente: this.correoCliente
    };

    this.reservaService.generarQrPago(this.reservaId(), payload).subscribe({
      next: (res) => {
        this.loadingQr.set(false);
        if (res && res.qrBase64) {
          this.qrBase64.set(res.qrBase64);
          this.transactionId.set(res.transactionId);
          this.messageService.add({
            severity: 'success',
            summary: 'QR Generado',
            detail: 'Escanea el código QR para realizar el pago.'
          });

          // Iniciar el Polling automático cada 5 segundos
          this.iniciarPollingStatus();
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'No se pudo obtener la imagen del QR.'
          });
        }
      },
      error: (err) => {
        this.loadingQr.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Error al generar QR',
          detail: err?.error?.message || 'Error de conexión con la pasarela.'
        });
      }
    });
  }

  iniciarPollingStatus(): void {
    if (this.pollIntervalId) {
      clearInterval(this.pollIntervalId);
    }

    this.pollIntervalId = setInterval(() => {
      this.verificarPagoSilencioso();
    }, 5000);
  }

  verificarPagoSilencioso(): void {
    if (!this.transactionId() || this.isExpired()) return;

    this.reservaService.verificarEstadoPago(this.reservaId(), this.transactionId()).subscribe({
      next: (res) => {
        if (res && res.pagado) {
          this.confirmarExitoPago();
        }
      }
    });
  }

  verificarPagoManual(): void {
    if (!this.transactionId()) return;

    this.verifying.set(true);
    this.reservaService.verificarEstadoPago(this.reservaId(), this.transactionId()).subscribe({
      next: (res) => {
        this.verifying.set(false);
        if (res && res.pagado) {
          this.confirmarExitoPago();
        } else {
          this.messageService.add({
            severity: 'info',
            summary: 'Pago Pendiente',
            detail: 'El pago aún no ha sido registrado. Por favor, asegúrate de completar la transferencia.'
          });
        }
      },
      error: () => {
        this.verifying.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Error de verificación',
          detail: 'No se pudo comprobar el estado del pago.'
        });
      }
    });
  }

  private confirmarExitoPago(): void {
    this.clearAllIntervals();
    this.isConfirmed.set(true);
    // Guardamos el total antes de limpiar la reserva de sesión
    this.totalPagadoLocal = this.reservaService.activeReserva()?.totalOriginal ?? 0;
    this.reservaService.clearActiveReserva(); // Limpia la sesión de reserva
    this.messageService.add({
      severity: 'success',
      summary: 'Pago Confirmado',
      detail: 'Tu reserva ha sido confirmada exitosamente.'
    });
  }

  volverAReserva(): void {
    const proyectoId = this.route.snapshot.paramMap.get('proyectoId');
    this.router.navigate(['/proyectos', proyectoId, 'reserva']);
  }

  volverAMisDisenos(): void {
    this.router.navigate(['/proyectos']);
  }

  descargarComprobante(): void {
    const proyectoId = Number(this.route.snapshot.paramMap.get('proyectoId'));
    if (!proyectoId) return;

    this.messageService.add({
      severity: 'info',
      summary: 'Descargando',
      detail: 'Generando comprobante y propuesta PDF...'
    });

    const elementos = this.canvasState.toElementoLienzoRequests();

    this.reservaService.exportarPropuestaPdf(proyectoId, '', elementos).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `comprobante-propuesta-proyecto-${proyectoId}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

        this.messageService.add({
          severity: 'success',
          summary: 'Descarga completa',
          detail: 'El PDF fue descargado exitosamente.'
        });
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error al descargar',
          detail: 'No se pudo generar el documento PDF.'
        });
        console.error('Error al exportar PDF:', err);
      }
    });
  }
}
