import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '@/app/shared/interfaces/core/api-response.interface';
import { API_URL } from '@/enviroment/enviroment';
import { Configuracion } from '../interface/configuracion.interface';

@Injectable({ providedIn: 'root' })
export class ConfiguracionService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/configuraciones`;

  getConfiguraciones(): Observable<Configuracion[]> {
    return this.http
      .get<ApiResponse<Configuracion[]>>(this.apiUrl)
      .pipe(map(r => r.data));
  }

  updateConfiguracion(clave: string, valor: string, descripcion?: string): Observable<Configuracion> {
    return this.http
      .put<ApiResponse<Configuracion>>(`${this.apiUrl}/${clave}`, null, {
        params: {
          valor,
          ...(descripcion ? { descripcion } : {})
        }
      })
      .pipe(map(r => r.data));
  }
}
