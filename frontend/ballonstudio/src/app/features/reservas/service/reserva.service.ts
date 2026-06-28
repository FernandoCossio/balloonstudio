import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_URL } from '@/enviroment/enviroment';
import type { ApiResponse } from '@/app/shared/interfaces/core/api-response.interface';
import type { Page, ReservaResponse } from '../interface/reserva.interface';

const API_BASE = `${API_URL}/proyectos/reservas`;

@Injectable({
    providedIn: 'root'
})
export class ReservaService {
    private http = inject(HttpClient);

    private buildParams(
        nombreCliente?: string,
        estado?: string,
        fechaInicio?: string,
        fechaFin?: string,
        page: number = 0,
        size: number = 10,
        sort: string = 'fechaReserva,desc'
    ): HttpParams {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        if (nombreCliente && nombreCliente.trim() !== '') {
            params = params.set('nombreCliente', nombreCliente.trim());
        }
        if (estado && estado !== '') {
            params = params.set('estado', estado);
        }
        if (fechaInicio && fechaInicio !== '') {
            params = params.set('fechaInicio', fechaInicio);
        }
        if (fechaFin && fechaFin !== '') {
            params = params.set('fechaFin', fechaFin);
        }
        if (sort) {
            params = params.set('sort', sort);
        }
        return params;
    }

    findReservasAdmin(
        nombreCliente?: string,
        estado?: string,
        fechaInicio?: string,
        fechaFin?: string,
        page: number = 0,
        size: number = 10,
        sort: string = 'fechaReserva,desc'
    ): Observable<Page<ReservaResponse>> {
        const params = this.buildParams(nombreCliente, estado, fechaInicio, fechaFin, page, size, sort);
        return this.http.get<ApiResponse<Page<ReservaResponse>>>(`${API_BASE}/admin`, { params }).pipe(
            map(res => res.data)
        );
    }

    findReservasEmpleado(
        nombreCliente?: string,
        fechaInicio?: string,
        fechaFin?: string,
        page: number = 0,
        size: number = 10,
        sort: string = 'fechaReserva,desc'
    ): Observable<Page<ReservaResponse>> {
        const params = this.buildParams(nombreCliente, undefined, fechaInicio, fechaFin, page, size, sort);
        return this.http.get<ApiResponse<Page<ReservaResponse>>>(`${API_BASE}/empleado`, { params }).pipe(
            map(res => res.data)
        );
    }

    findReservasCliente(
        estado?: string,
        fechaInicio?: string,
        fechaFin?: string,
        page: number = 0,
        size: number = 10,
        sort: string = 'fechaReserva,desc'
    ): Observable<Page<ReservaResponse>> {
        const params = this.buildParams(undefined, estado, fechaInicio, fechaFin, page, size, sort);
        return this.http.get<ApiResponse<Page<ReservaResponse>>>(`${API_BASE}/cliente`, { params }).pipe(
            map(res => res.data)
        );
    }
}
