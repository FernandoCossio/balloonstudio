import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '@/app/shared/interfaces/core/api-response.interface';
import { API_URL } from '@/enviroment/enviroment';
import { DashboardMetrics } from '../interfaces/dashboard.interface';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/dashboard`;

  getMetrics(): Observable<DashboardMetrics> {
    return this.http
      .get<ApiResponse<DashboardMetrics>>(`${this.apiUrl}/metrics`)
      .pipe(map(response => response.data));
  }
}
