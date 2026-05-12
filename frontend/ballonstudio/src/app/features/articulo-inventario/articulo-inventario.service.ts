import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthService } from '../auth/service/auth';

// ─── Interfaces (match Java DTOs) ────────────────────────────────────────────

export interface CategoriaResponse {
    id: number;
    nombre: string;
    descripcion: string;
}

export interface ArticuloInventarioRequest {
    nombre: string;
    descripcion?: string;
    tipoArticulo: 'CONSUMIBLE' | 'REUTILIZABLE';
    estado: 'DISPONIBLE' | 'STOCK_BAJO' | 'EN_MANTENIMIENTO' | 'INACTIVO';
    costoAdquisicion?: number;
    porcentajeGanancia?: number;
    valorResidual?: number;
    vidaUtilAnos?: number;
    vidaUtilUsos?: number;
    stockTotal?: number;
    pesoKg?: number;
    volumenM3?: number;
    tiempoArmadoMin?: number;
    diasPreparacionPrevios?: number;
    diasLimpiezaPosteriores?: number;
    mantenimientoPromedioBs?: number;
    nivelComplejidad?: 'FACIL' | 'MEDIO' | 'PROFESIONAL';
    embeddingVisual?: number[];
    categoriaIds?: number[];
}

export interface ArticuloInventarioResponse {
    id: number;
    nombre: string;
    descripcion?: string;
    tipoArticulo: 'CONSUMIBLE' | 'REUTILIZABLE';
    estado: 'DISPONIBLE' | 'STOCK_BAJO' | 'EN_MANTENIMIENTO' | 'INACTIVO';
    costoAdquisicion?: number;
    porcentajeGanancia?: number;
    valorResidual?: number;
    vidaUtilAnos?: number;
    vidaUtilUsos?: number;
    stockTotal?: number;
    pesoKg?: number;
    volumenM3?: number;
    tiempoArmadoMin?: number;
    diasPreparacionPrevios?: number;
    diasLimpiezaPosteriores?: number;
    mantenimientoPromedioBs?: number;
    nivelComplejidad?: 'FACIL' | 'MEDIO' | 'PROFESIONAL';
    embeddingVisual?: number[];
    categorias: CategoriaResponse[];
}

// ─── Service ─────────────────────────────────────────────────────────────────

const API_BASE = 'http://localhost:8080/api/inventario';

@Injectable({ providedIn: 'root' })
export class ArticuloInventarioService {
    private http = inject(HttpClient);
    private auth = inject(AuthService);

    /** Construye headers con el JWT actual y loguea el estado del token */
    private authHeaders(): HttpHeaders {
        const token = this.auth.getToken();
        console.log('[ArticuloInventarioService] isLoggedIn:', this.auth.isLoggedIn());
        console.log('[ArticuloInventarioService] token en localStorage:', token ? `${token.substring(0, 20)}...` : 'NULL ❌ (no hay token)');
        if (!token) {
            console.warn('[ArticuloInventarioService] ⚠️  No hay token JWT – el backend responderá 401. Verifica que el usuario esté autenticado.');
        }
        return new HttpHeaders({
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {})
        });
    }

    getAll(): Observable<ArticuloInventarioResponse[]> {
        const headers = this.authHeaders();
        console.log('[ArticuloInventarioService] GET', API_BASE, '| Authorization header presente:', headers.has('Authorization'));
        return this.http.get<ArticuloInventarioResponse[]>(API_BASE, { headers }).pipe(
            tap({
                next: data => console.log('[ArticuloInventarioService] ✅ getAll OK – registros:', data.length),
                error: err  => console.error('[ArticuloInventarioService] ❌ getAll ERROR', err.status, err.message)
            })
        );
    }

    getById(id: number): Observable<ArticuloInventarioResponse> {
        const headers = this.authHeaders();
        console.log('[ArticuloInventarioService] GET', `${API_BASE}/${id}`);
        return this.http.get<ArticuloInventarioResponse>(`${API_BASE}/${id}`, { headers }).pipe(
            tap({
                next: data => console.log('[ArticuloInventarioService] ✅ getById OK', data),
                error: err  => console.error('[ArticuloInventarioService] ❌ getById ERROR', err.status, err.message)
            })
        );
    }

    create(request: ArticuloInventarioRequest): Observable<ArticuloInventarioResponse> {
        const headers = this.authHeaders();
        console.log('[ArticuloInventarioService] POST', API_BASE, request);
        return this.http.post<ArticuloInventarioResponse>(API_BASE, request, { headers }).pipe(
            tap({
                next: data => console.log('[ArticuloInventarioService] ✅ create OK', data),
                error: err  => console.error('[ArticuloInventarioService] ❌ create ERROR', err.status, err.message)
            })
        );
    }

    update(id: number, request: ArticuloInventarioRequest): Observable<ArticuloInventarioResponse> {
        const headers = this.authHeaders();
        console.log('[ArticuloInventarioService] PUT', `${API_BASE}/${id}`, request);
        return this.http.put<ArticuloInventarioResponse>(`${API_BASE}/${id}`, request, { headers }).pipe(
            tap({
                next: data => console.log('[ArticuloInventarioService] ✅ update OK', data),
                error: err  => console.error('[ArticuloInventarioService] ❌ update ERROR', err.status, err.message)
            })
        );
    }

    delete(id: number): Observable<void> {
        const headers = this.authHeaders();
        console.log('[ArticuloInventarioService] DELETE', `${API_BASE}/${id}`);
        return this.http.delete<void>(`${API_BASE}/${id}`, { headers }).pipe(
            tap({
                next: ()  => console.log('[ArticuloInventarioService] ✅ delete OK id:', id),
                error: err => console.error('[ArticuloInventarioService] ❌ delete ERROR', err.status, err.message)
            })
        );
    }

    // ─── Helpers de UI ───────────────────────────────────────────────────────

    /** Precio sugerido basado en costo + porcentaje de ganancia */
    calcPrecioSugerido(costo: number, porcentaje: number): number {
        return costo * (1 + porcentaje / 100);
    }

    /** ROI simple en usos */
    calcRoi(costo: number, precio: number, usos: number): number | null {
        if (!precio || precio <= costo) return null;
        return Math.ceil(costo / (precio - costo));
    }
}
