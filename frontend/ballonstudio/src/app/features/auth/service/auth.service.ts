import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, finalize, map, of, tap } from 'rxjs';
import type { ApiResponse } from '@/app/shared/interfaces/core/api-response.interface';
import type { Role } from '@/app/features/core/constants/role.constant';
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

export type AppRole = Role;

export interface JwtPayload {
    iss?: string;
    sub?: string;
    iat?: number;
    exp?: number;
    roles?: string[];
    uid?: number;
}

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private http = inject(HttpClient);
    private readonly API_URL = 'http://localhost:8080/api'; // Base URL para todos los endpoints

    private _payload = signal<JwtPayload | null>(null);

    login(credentials: LoginRequest): Observable<TokenResponse> {
        return this.http
            .post<ApiResponse<TokenResponse>>(`${this.API_URL}/auth/login`, credentials, { withCredentials: true })
            .pipe(
                map(res => res.data),
                tap(token => {
                    if (token?.accessToken) {
                        this._payload.set(decodeJwtPayload(token.accessToken));
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
                        this._payload.set(decodeJwtPayload(token.accessToken));
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
        this._payload.set(null);
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
        return this._payload() !== null && !this.isAccessTokenExpired();
    }

    getAccessTokenPayload(): JwtPayload | null {
        return this._payload();
    }

    getRoles(): AppRole[] {
        const payload = this.getAccessTokenPayload();
        const roles = payload?.roles ?? [];
        const normalized = roles
            .map(r => normalizeRole(r))
            .filter((r): r is AppRole => r !== null);
        return Array.from(new Set(normalized));
    }

    hasRole(role: AppRole): boolean {
        return this.getRoles().includes(role);
    }

    hasAnyRole(roles: AppRole[]): boolean {
        const userRoles = new Set(this.getRoles());
        return roles.some(r => userRoles.has(r));
    }

    isAccessTokenExpired(): boolean {
        const exp = this._payload()?.exp;
        if (!exp) return true;
        return Date.now() >= exp * 1000;
    }

    initFromRefresh(): Observable<TokenResponse> {
        return this.refresh();
    }

    initFromSession(): Observable<void> {
        return this.http
            .get<ApiResponse<string>>(`${this.API_URL}/auth/me/token`, { withCredentials: true })
            .pipe(
                tap(res => {
                    if (res?.data) {
                        this._payload.set(decodeJwtPayload(res.data));
                    }
                }),
                map(() => void 0),
                catchError(() => {
                    this._payload.set(null);
                    return of(void 0);
                })
            );
    }
}

function normalizeRole(role: string): AppRole | null {
    const value = role.trim().toLowerCase();
    if (value === 'role_administrador' || value === 'role_cliente' || value === 'role_empleado') return value as AppRole;
    return null;
}

function decodeJwtPayload(token: string): JwtPayload | null {
    const parts = token.split('.');
    if (parts.length < 2) return null;

    const payload = parts[1];
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');

    try {
        const json = atob(padded);
        return JSON.parse(json) as JwtPayload;
    } catch {
        return null;
    }
}
