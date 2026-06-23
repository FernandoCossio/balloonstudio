// catalogo.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '@/app/shared/interfaces/core/api-response.interface';
import { ArticuloInventarioDto } from '../interfaces/articulo-inventario-dto.interface';
import { API_URL } from '@/enviroment/enviroment';

import { CategoriaResponse } from '@/app/features/articulo-inventario/service/articulo-inventario.service';

@Injectable({
  providedIn: 'root'
})
export class CatalogoService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/inventario`;

  getCatalogo(tipo?: string, estado?: string, categoriaId?: number): Observable<ArticuloInventarioDto[]> {
    let params = new HttpParams();
    if (tipo)   params = params.set('tipo', tipo);
    if (estado) params = params.set('estado', estado);
    if (categoriaId) params = params.set('categoriaId', categoriaId.toString());

    return this.http
      .get<ApiResponse<ArticuloInventarioDto[]>>(`${this.apiUrl}/catalogo`, { params })
      .pipe(map(response => response.data));
  }

  getCategorias(): Observable<CategoriaResponse[]> {
    return this.http
      .get<ApiResponse<CategoriaResponse[]>>(`${API_URL}/categorias`)
      .pipe(map(response => response.data));
  }

  recomendarPorTexto(prompt: string, limit?: number, categoriaId?: number): Observable<ArticuloInventarioDto[]> {
    const payload = {
      prompt,
      limit: limit || 5,
      categoriaId: categoriaId || null
    };
    return this.http
      .post<ApiResponse<ArticuloInventarioDto[]>>(`${API_URL}/recomendaciones/texto`, payload)
      .pipe(map(response => response.data));
  }

  recomendarPorImagen(file: File, limit?: number, categoriaId?: number): Observable<ArticuloInventarioDto[]> {
    const formData = new FormData();
    formData.append('file', file);
    if (limit) formData.append('limit', limit.toString());
    if (categoriaId) formData.append('categoriaId', categoriaId.toString());

    return this.http
      .post<ApiResponse<ArticuloInventarioDto[]>>(`${API_URL}/recomendaciones/imagen`, formData)
      .pipe(map(response => response.data));
  }
}