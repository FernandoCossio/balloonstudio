import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_URL } from '@/enviroment/enviroment';
import type { ApiResponse } from '@/app/shared/interfaces/core/api-response.interface';

export interface VentasReporteData {
    id: number;
    clienteNombre: string;
    fechaReserva: string;
    total: number;
    estado: string;
}

export interface UsuariosReporteData {
    id: number;
    nombreCompleto: string;
    username: string;
    email: string;
    roles: string[];
    activo: boolean;
}

const API_BASE = `${API_URL}/reportes`;

@Injectable({
    providedIn: 'root'
})
export class ReportesService {
    private http = inject(HttpClient);

    getVentasDatos(fechaInicio?: string, fechaFin?: string, estado?: string): Observable<VentasReporteData[]> {
        let params = new HttpParams();
        if (fechaInicio) params = params.set('fechaInicio', fechaInicio);
        if (fechaFin) params = params.set('fechaFin', fechaFin);
        if (estado) params = params.set('estado', estado);

        return this.http.get<ApiResponse<VentasReporteData[]>>(`${API_BASE}/ventas/datos`, { params }).pipe(
            map(res => res.data)
        );
    }

    getUsuariosDatos(rol?: string, activo?: boolean): Observable<UsuariosReporteData[]> {
        let params = new HttpParams();
        if (rol) params = params.set('rol', rol);
        if (activo !== undefined && activo !== null) params = params.set('activo', activo.toString());

        return this.http.get<ApiResponse<UsuariosReporteData[]>>(`${API_BASE}/usuarios/datos`, { params }).pipe(
            map(res => res.data)
        );
    }

    descargarVentas(formato: 'pdf' | 'excel', fechaInicio?: string, fechaFin?: string, estado?: string): Observable<Blob> {
        let params = new HttpParams().set('format', formato);
        if (fechaInicio) params = params.set('fechaInicio', fechaInicio);
        if (fechaFin) params = params.set('fechaFin', fechaFin);
        if (estado) params = params.set('estado', estado);

        return this.http.get(`${API_BASE}/ventas`, {
            params,
            responseType: 'blob'
        });
    }

    descargarUsuarios(formato: 'pdf' | 'excel', rol?: string, activo?: boolean): Observable<Blob> {
        let params = new HttpParams().set('format', formato);
        if (rol) params = params.set('rol', rol);
        if (activo !== undefined && activo !== null) params = params.set('activo', activo.toString());

        return this.http.get(`${API_BASE}/usuarios`, {
            params,
            responseType: 'blob'
        });
    }
}
