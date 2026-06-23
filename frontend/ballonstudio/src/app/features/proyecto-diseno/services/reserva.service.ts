import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { ApiResponse } from '@/app/shared/interfaces/core/api-response.interface';
import { API_URL } from '@/enviroment/enviroment';
import { ReservaResponse } from '../interfaces/proyecto-diseno.interface';

@Injectable({ providedIn: 'root' })
export class ReservaService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/proyectos`;

  // Estado compartido de la reserva activa y cuándo se inició
  readonly activeReserva = signal<ReservaResponse | null>(null);
  readonly sessionStartTime = signal<number | null>(null);

  iniciarReserva(proyectoId: number, usuarioId: number): Observable<ReservaResponse> {
    return this.http
      .post<ApiResponse<ReservaResponse>>(`${this.apiUrl}/${proyectoId}/reservar`, { usuarioId })
      .pipe(
        map(r => r.data),
        tap(reserva => {
          this.activeReserva.set(reserva);
          this.sessionStartTime.set(Date.now());
        })
      );
  }

  confirmarPago(reservaId: number, referenciaPago: string): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.apiUrl}/pagos/webhook`, null, {
        params: {
          reservaId: reservaId.toString(),
          referenciaPago
        }
      })
      .pipe(
        map(() => {
          // Limpiar sesión al finalizar exitosamente
          this.clearActiveReserva();
        })
      );
  }

  generarQrPago(reservaId: number, datosCliente: { nombreCliente: string; ciCliente: string; telefonoCliente?: string; correoCliente?: string }): Observable<any> {
    return this.http
      .post<ApiResponse<any>>(`${this.apiUrl}/reservas/${reservaId}/qr`, datosCliente)
      .pipe(map(r => r.data));
  }

  verificarEstadoPago(reservaId: number, transactionId: string): Observable<any> {
    return this.http
      .get<ApiResponse<any>>(`${this.apiUrl}/reservas/${reservaId}/pago-estado`, {
        params: { transactionId }
      })
      .pipe(map(r => r.data));
  }

  clearActiveReserva(): void {
    this.activeReserva.set(null);
    this.sessionStartTime.set(null);
  }


  getTiempoRestanteSegundos(): number {
    const start = this.sessionStartTime();
    const reserva = this.activeReserva();
    if (!start || !reserva) return 0;

    const totalSeconds = reserva.expiraEnMinutos * 60;
    const elapsedSeconds = Math.floor((Date.now() - start) / 1000);
    return Math.max(0, totalSeconds - elapsedSeconds);
  }

  previsualizarCotizacion(proyectoId: number, elementos: any[], distanciaKm?: number): Observable<any> {
    return this.http
      .post<ApiResponse<any>>(`${this.apiUrl}/${proyectoId}/cotizacion/previsualizar`, {
        elementos,
        distanciaKm
      })
      .pipe(map(r => r.data));
  }

  exportarPropuestaPdf(proyectoId: number, base64Canvas: string, elementos: any[]): Observable<Blob> {
    return this.http.post(`${this.apiUrl}/${proyectoId}/exportar-pdf`, {
      base64Canvas,
      elementos
    }, {
      responseType: 'blob'
    });
  }
}
