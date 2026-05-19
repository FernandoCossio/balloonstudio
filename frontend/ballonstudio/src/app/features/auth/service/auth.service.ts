import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, finalize, map, tap } from 'rxjs';
import type { ApiResponse } from '@/app/shared/interfaces/core/api-response.interface';
import type { ActivarCuentaDto } from '../interface/activar-cuenta-dto.interface';
import type { LoginRequest } from '../interface/login-request.interface';
import type { RegistrarClienteDto } from '../interface/registrar-cliente-dto.interface';
import type { ResponseUsuarioDto } from '../interface/response-usuario-dto.interface';
import type { TokenResponse } from '../interface/token-response.interface';

export type { ActivarCuentaDto } from '../interface/activar-cuenta-dto.interface';
export type { LoginRequest } from '../interface/login-request.interface';
export type { RegistrarClienteDto } from '../interface/registrar-cliente-dto.interface';
export type { ResponseUsuarioDto } from '../interface/response-usuario-dto.interface';
export type { TokenResponse } from '../interface/token-response.interface';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private http = inject(HttpClient);
    private readonly API_URL = 'http://localhost:8080/api'; // Base URL para todos los endpoints

    login(credentials: LoginRequest): Observable<TokenResponse> {
        return this.http
            .post<ApiResponse<TokenResponse>>(`${this.API_URL}/auth/login`, credentials, { withCredentials: true })
            .pipe(
                map(res => res.data),
                tap(token => {
                    if (token?.accessToken) {
                        localStorage.setItem('access_token', token.accessToken);
                    }
                })
            );
    }

    refresh(): Observable<TokenResponse> {
        return this.http
            .post<ApiResponse<TokenResponse>>(`${this.API_URL}/auth/refresh`, {}, { withCredentials: true })
            .pipe(
                map(res => res.data),
                tap(token => {
                    if (token?.accessToken) {
                        localStorage.setItem('access_token', token.accessToken);
                    }
                })
        );
    }

    register(clientData: RegistrarClienteDto): Observable<ApiResponse<ResponseUsuarioDto>> {
        return this.http.post<ApiResponse<ResponseUsuarioDto>>(`${this.API_URL}/auth/register`, clientData);
    }

    verifyToken(token: string): Observable<ApiResponse<null>> {
        return this.http.get<ApiResponse<null>>(`${this.API_URL}/auth-token/verify/${token}`);
    }

    activateAccount(data: ActivarCuentaDto): Observable<ApiResponse<null>> {
        return this.http.post<ApiResponse<null>>(`${this.API_URL}/auth-token/activate`, data);
    }

    resendActivationEmail(email: string): Observable<ApiResponse<null>> {
        return this.http.post<ApiResponse<null>>(`${this.API_URL}/auth-token/resend`, { email });
    }

    logout(): void {
        localStorage.removeItem('access_token');
    }

    logoutServer(): Observable<void> {
        return this.http
            .post(`${this.API_URL}/auth/logout`, {}, { withCredentials: true, responseType: 'text' })
            .pipe(
                finalize(() => {
                    this.logout();
                }),
                map(() => void 0)
            );
    }

    isLoggedIn(): boolean {
        return !!localStorage.getItem('access_token');
    }

    getToken(): string | null {
        return localStorage.getItem('access_token');
    }
}
