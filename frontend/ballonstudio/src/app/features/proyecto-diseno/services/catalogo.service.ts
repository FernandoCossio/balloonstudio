// catalogo.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '@/app/shared/interfaces/core/api-response.interface';
import { ArticuloInventarioDto } from '../interfaces/articulo-inventario-dto.interface';
import { API_URL } from '@/enviroment/enviroment';

@Injectable({
  providedIn: 'root'
})
export class CatalogoService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/inventario`;

  getCatalogo(tipo?: string, estado?: string): Observable<ArticuloInventarioDto[]> {
    let params = new HttpParams();
    if (tipo)   params = params.set('tipo', tipo);
    if (estado) params = params.set('estado', estado);

    return this.http
      .get<ApiResponse<ArticuloInventarioDto[]>>(`${this.apiUrl}/catalogo`, { params })
      .pipe(map(response => response.data));
  }
}