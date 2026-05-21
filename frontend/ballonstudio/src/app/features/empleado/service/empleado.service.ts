import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_URL } from '@/enviroment/enviroment';
import type { ApiResponse } from '@/app/shared/interfaces/core/api-response.interface';

export interface EmpleadoRequest {
    nombreCompleto: string;
    email: string;
    telefono?: string;
    username?: string;
}

export interface EmpleadoResponse {
    id: number;
    username: string;
    nombreCompleto: string;
    email: string;
    telefono: string;
    activo: boolean;
    roles: string[];
}

export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    numberOfElements: number;
    first: boolean;
    last: boolean;
    empty: boolean;
}

const API_BASE = `${API_URL}/empleados`;

@Injectable({
    providedIn: 'root'
})
export class EmpleadoService {
    private http = inject(HttpClient);

    findEmpleados(
        nombre?: string,
        rol?: string,
        page: number = 0,
        size: number = 10,
        sort: string = 'nombreCompleto,asc'
    ): Observable<Page<EmpleadoResponse>> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        if (nombre && nombre.trim() !== '') {
            params = params.set('nombre', nombre.trim());
        }
        if (rol && rol !== '') {
            params = params.set('rol', rol);
        }
        if (sort) {
            params = params.set('sort', sort);
        }

        return this.http.get<ApiResponse<Page<EmpleadoResponse>>>(API_BASE, { params }).pipe(
            map(res => res.data)
        );
    }

    getById(id: number): Observable<EmpleadoResponse> {
        return this.http.get<ApiResponse<EmpleadoResponse>>(`${API_BASE}/${id}`).pipe(
            map(res => res.data)
        );
    }

    create(request: EmpleadoRequest): Observable<EmpleadoResponse> {
        return this.http.post<ApiResponse<EmpleadoResponse>>(API_BASE, request).pipe(
            map(res => res.data)
        );
    }

    update(id: number, request: EmpleadoRequest): Observable<EmpleadoResponse> {
        return this.http.put<ApiResponse<EmpleadoResponse>>(`${API_BASE}/${id}`, request).pipe(
            map(res => res.data)
        );
    }

    deactivate(id: number): Observable<void> {
        return this.http.patch<ApiResponse<void>>(`${API_BASE}/${id}/desactivar`, {}).pipe(
            map(() => undefined)
        );
    }

    activate(id: number): Observable<void> {
        return this.http.patch<ApiResponse<void>>(`${API_BASE}/${id}/activar`, {}).pipe(
            map(() => undefined)
        );
    }
}
