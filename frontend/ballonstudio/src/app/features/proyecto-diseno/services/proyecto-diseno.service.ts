// features/proyecto-diseno/services/proyecto-diseno.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '@/app/shared/interfaces/core/api-response.interface';
import { API_URL } from '@/enviroment/enviroment';
import {
  ProyectoDisenoRequest, ProyectoDisenoResponse,
  EscenarioBaseRequest, EscenarioBaseResponse,
  ElementoLienzoRequest, ElementoLienzoResponse
} from '../interfaces/proyecto-diseno.interface';

@Injectable({ providedIn: 'root' })
export class ProyectoDisenoService {

  private http   = inject(HttpClient);
  private apiUrl = `${API_URL}/proyecto-diseno`;

  // ── Proyectos ─────────────────────────────────────────────────────────────

  getAll(): Observable<ProyectoDisenoResponse[]> {
    return this.http
      .get<ApiResponse<ProyectoDisenoResponse[]>>(this.apiUrl)
      .pipe(map(r => r.data));
  }

  getById(id: number): Observable<ProyectoDisenoResponse> {
    return this.http
      .get<ApiResponse<ProyectoDisenoResponse>>(`${this.apiUrl}/${id}`)
      .pipe(map(r => r.data));
  }

  create(request: ProyectoDisenoRequest): Observable<ProyectoDisenoResponse> {
    return this.http
      .post<ApiResponse<ProyectoDisenoResponse>>(this.apiUrl, request)
      .pipe(map(r => r.data));
  }

  update(id: number, request: ProyectoDisenoRequest): Observable<ProyectoDisenoResponse> {
    return this.http
      .put<ApiResponse<ProyectoDisenoResponse>>(`${this.apiUrl}/${id}`, request)
      .pipe(map(r => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.apiUrl}/${id}`)
      .pipe(map(() => void 0));
  }

  // ── Escenarios ────────────────────────────────────────────────────────────

  getEscenario(proyectoId: number, escenarioId: number): Observable<EscenarioBaseResponse> {
    return this.http
      .get<ApiResponse<EscenarioBaseResponse>>(
        `${this.apiUrl}/${proyectoId}/escenarios/${escenarioId}`
      ).pipe(map(r => r.data));
  }

  createEscenario(proyectoId: number, request: EscenarioBaseRequest): Observable<EscenarioBaseResponse> {
    return this.http
      .post<ApiResponse<EscenarioBaseResponse>>(
        `${this.apiUrl}/${proyectoId}/escenarios`, request
      ).pipe(map(r => r.data));
  }

  updateEscenario(proyectoId: number, escenarioId: number,
                  request: EscenarioBaseRequest): Observable<EscenarioBaseResponse> {
    return this.http
      .put<ApiResponse<EscenarioBaseResponse>>(
        `${this.apiUrl}/${proyectoId}/escenarios/${escenarioId}`, request
      ).pipe(map(r => r.data));
  }

  uploadImagenEscenario(proyectoId: number, escenarioId: number,
                        file: File): Observable<EscenarioBaseResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<ApiResponse<EscenarioBaseResponse>>(
        `${this.apiUrl}/${proyectoId}/escenarios/${escenarioId}/imagen`, formData
      ).pipe(map(r => r.data));
  }

  uploadDisenoEscenario(proyectoId: number, escenarioId: number,
                        file: File): Observable<EscenarioBaseResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<ApiResponse<EscenarioBaseResponse>>(
        `${this.apiUrl}/${proyectoId}/escenarios/${escenarioId}/upload-diseno`, formData
      ).pipe(map(r => r.data));
  }

  deleteEscenario(proyectoId: number, escenarioId: number): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(
        `${this.apiUrl}/${proyectoId}/escenarios/${escenarioId}`
      ).pipe(map(() => void 0));
  }

  // ── Elementos del lienzo ──────────────────────────────────────────────────

  getElementos(proyectoId: number, escenarioId: number): Observable<ElementoLienzoResponse[]> {
    return this.http
      .get<ApiResponse<ElementoLienzoResponse[]>>(
        `${this.apiUrl}/${proyectoId}/escenarios/${escenarioId}/elementos`
      ).pipe(map(r => r.data));
  }

  guardarElementos(proyectoId: number, escenarioId: number,
                   elementos: ElementoLienzoRequest[]): Observable<ElementoLienzoResponse[]> {
    return this.http
      .put<ApiResponse<ElementoLienzoResponse[]>>(
        `${this.apiUrl}/${proyectoId}/escenarios/${escenarioId}/elementos`, elementos
      ).pipe(map(r => r.data));
  }
}