import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import type { ApiResponse } from '@/app/shared/interfaces/core/api-response.interface';
import { API_URL } from '@/enviroment/enviroment';

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

export interface ImagenArticuloResponse {
    id: number;
    url: string;
    esPrincipal: boolean;
    orden: number;
    procesadoIa: boolean;
    fechaSubida: string;
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
    imagenes?: ImagenArticuloResponse[];
}

// ─── Service ─────────────────────────────────────────────────────────────────

const API_BASE = `${API_URL}/inventario`;

@Injectable({ providedIn: 'root' })
export class ArticuloInventarioService {
    private http = inject(HttpClient);

    getAll(): Observable<ArticuloInventarioResponse[]> {
        return this.http.get<ApiResponse<ArticuloInventarioResponse[]>>(API_BASE).pipe(
            map(res => res.data),
            tap({
                next: data => console.log('[ArticuloInventarioService] ✅ getAll OK – registros:', data.length),
                error: err => console.error('[ArticuloInventarioService] ❌ getAll ERROR', err.status, err.message)
            })
        );
    }

    getById(id: number): Observable<ArticuloInventarioResponse> {
        return this.http.get<ApiResponse<ArticuloInventarioResponse>>(`${API_BASE}/${id}`).pipe(
            map(res => res.data),
            tap({
                next: data => console.log('[ArticuloInventarioService] ✅ getById OK', data),
                error: err  => console.error('[ArticuloInventarioService] ❌ getById ERROR', err.status, err.message)
            })
        );
    }

    create(request: ArticuloInventarioRequest): Observable<ArticuloInventarioResponse> {
        return this.http.post<ApiResponse<ArticuloInventarioResponse>>(API_BASE, request).pipe(
            map(res => res.data),
            tap({
                next: data => console.log('[ArticuloInventarioService] ✅ create OK', data),
                error: err  => console.error('[ArticuloInventarioService] ❌ create ERROR', err.status, err.message)
            })
        );
    }

    update(id: number, request: ArticuloInventarioRequest): Observable<ArticuloInventarioResponse> {
        return this.http.put<ApiResponse<ArticuloInventarioResponse>>(`${API_BASE}/${id}`, request).pipe(
            map(res => res.data),
            tap({
                next: data => console.log('[ArticuloInventarioService] ✅ update OK', data),
                error: err  => console.error('[ArticuloInventarioService] ❌ update ERROR', err.status, err.message)
            })
        );
    }

    delete(id: number): Observable<void> {
        return this.http.delete(`${API_BASE}/${id}`, { responseType: 'text' }).pipe(
            map(() => void 0),
            tap({
                next: ()  => console.log('[ArticuloInventarioService] ✅ delete OK id:', id),
                error: err => console.error('[ArticuloInventarioService] ❌ delete ERROR', err.status, err.message)
            })
        );
    }

    uploadImagenes(articuloId: number, files: File[]): Observable<ImagenArticuloResponse[]> {
        const formData = new FormData();
        for (const file of files) {
            formData.append('files', file);
        }
        return this.http.post<ApiResponse<ImagenArticuloResponse[]>>(`${API_BASE}/${articuloId}/imagenes`, formData).pipe(
            map(res => res.data),
            tap({
                next: data => console.log('[ArticuloInventarioService] ✅ uploadImagenes OK', data),
                error: err  => console.error('[ArticuloInventarioService] ❌ uploadImagenes ERROR', err)
            })
        );
    }

    setPrincipal(articuloId: number, imagenId: number): Observable<void> {
        return this.http.patch<ApiResponse<void>>(`${API_BASE}/${articuloId}/imagenes/${imagenId}/principal`, {}).pipe(
            map(() => void 0),
            tap({
                next: () => console.log('[ArticuloInventarioService] ✅ setPrincipal OK'),
                error: err => console.error('[ArticuloInventarioService] ❌ setPrincipal ERROR', err)
            })
        );
    }

    deleteImagen(articuloId: number, imagenId: number): Observable<void> {
        return this.http.delete<ApiResponse<void>>(`${API_BASE}/${articuloId}/imagenes/${imagenId}`).pipe(
            map(() => void 0),
            tap({
                next: () => console.log('[ArticuloInventarioService] ✅ deleteImagen OK'),
                error: err => console.error('[ArticuloInventarioService] ❌ deleteImagen ERROR', err)
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
