import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import type { ApiResponse } from '@/app/shared/interfaces/core/api-response.interface';
import { API_URL } from '@/enviroment/enviroment';

export interface IncidenciaRequest {
  articuloId: number;
  reservaId?: number | null;
  descripcion: string;
  tipo: 'REPARACION' | 'MERMA_PERDIDA';
  cantidad: number;
  fechaResolucionEstimada?: string | null;
}

export interface SolucionarIncidenciaRequest {
  costoReparacion?: number | null;
}

export interface IncidenciaArticulo {
  id: number;
  articuloInventario: {
    id: number;
    nombre: string;
    imagenes?: { url: string; esPrincipal: boolean }[];
    stockTotal: number;
  };
  reserva?: {
    id: number;
  } | null;
  descripcion: string;
  tipo: 'REPARACION' | 'MERMA_PERDIDA';
  estado: 'ACTIVA' | 'SOLUCIONADA';
  cantidadAfectada: number;
  fechaIncidencia: string;
  fechaResolucionEstimada?: string | null;
  costoReparacion?: number | null;
}

const API_BASE = `${API_URL}/inventario/incidencias`;

@Injectable({ providedIn: 'root' })
export class IncidenciaService {
  private http = inject(HttpClient);

  reportarIncidencia(request: IncidenciaRequest): Observable<IncidenciaArticulo> {
    return this.http.post<ApiResponse<IncidenciaArticulo>>(API_BASE, request).pipe(
      map(res => res.data)
    );
  }

  solucionarIncidencia(id: number, request?: SolucionarIncidenciaRequest): Observable<IncidenciaArticulo> {
    return this.http.patch<ApiResponse<IncidenciaArticulo>>(`${API_BASE}/${id}/solucionar`, request ?? {}).pipe(
      map(res => res.data)
    );
  }

  listarIncidencias(): Observable<IncidenciaArticulo[]> {
    return this.http.get<ApiResponse<IncidenciaArticulo[]>>(API_BASE).pipe(
      map(res => res.data)
    );
  }
}
