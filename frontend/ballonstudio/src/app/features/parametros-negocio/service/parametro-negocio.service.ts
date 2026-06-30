import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '@/app/shared/interfaces/core/api-response.interface';
import { API_URL } from '@/enviroment/enviroment';
import { ParametroNegocio } from '../interface/parametro-negocio.interface';

@Injectable({ providedIn: 'root' })
export class ParametroNegocioService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/parametros-negocio`;

  getParametros(): Observable<ParametroNegocio> {
    return this.http
      .get<ApiResponse<ParametroNegocio>>(this.apiUrl)
      .pipe(map(r => r.data));
  }

  updateParametros(parametros: ParametroNegocio): Observable<ParametroNegocio> {
    return this.http
      .put<ApiResponse<ParametroNegocio>>(this.apiUrl, parametros)
      .pipe(map(r => r.data));
  }
}
