import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '@/app/shared/interfaces/core/api-response.interface';
import { API_URL } from '@/enviroment/enviroment';
import { Categoria } from '../interface/categoria.interface';

@Injectable({
  providedIn: 'root'
})
export class CategoriaService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/categorias`;

  getCategorias(): Observable<Categoria[]> {
    return this.http
      .get<ApiResponse<Categoria[]>>(this.apiUrl)
      .pipe(map(r => r.data));
  }

  getCategoriaById(id: number): Observable<Categoria> {
    return this.http
      .get<ApiResponse<Categoria>>(`${this.apiUrl}/${id}`)
      .pipe(map(r => r.data));
  }

  createCategoria(categoria: Categoria): Observable<Categoria> {
    return this.http
      .post<ApiResponse<Categoria>>(this.apiUrl, categoria)
      .pipe(map(r => r.data));
  }

  updateCategoria(id: number, categoria: Categoria): Observable<Categoria> {
    return this.http
      .put<ApiResponse<Categoria>>(`${this.apiUrl}/${id}`, categoria)
      .pipe(map(r => r.data));
  }

  deleteCategoria(id: number): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.apiUrl}/${id}`)
      .pipe(map(() => void 0));
  }
}
