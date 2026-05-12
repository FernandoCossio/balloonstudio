import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface LoginRequest {
    username: string;
    password: string;
}

export interface TokenResponse {
    accessToken: string;
    tokenType: string;
    expiresInSeconds: number;
    expiresAt: string;
}

export interface RegistrarClienteDto {
    username: string;
    email: string;
    nombreCompleto: string;
    telefono?: string;
}

export interface ResponseUsuarioDto {
    uuid: string;
    username: string;
    email: string;
    nombreCompleto: string;
    telefono: string;
    roles: string[];
}

export interface ActivarCuentaDto {
    token: string;
    password: string;
    confirmPassword: string;
}

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private http = inject(HttpClient);
    private readonly API_URL = 'http://localhost:8080'; // Base URL para todos los endpoints

    login(credentials: LoginRequest): Observable<TokenResponse> {
        return this.http.post<TokenResponse>(`${this.API_URL}/auth/login`, credentials).pipe(
            tap(response => {
                if (response.accessToken) {
                    localStorage.setItem('access_token', response.accessToken);
                }
            })
        );
    }

    register(clientData: RegistrarClienteDto): Observable<ResponseUsuarioDto> {
        return this.http.post<ResponseUsuarioDto>(`${this.API_URL}/auth/register`, clientData);
    }

    verifyToken(token: string): Observable<{message: string}> {
        return this.http.get<{message: string}>(`${this.API_URL}/auth-token/verify/${token}`);
    }

    activateAccount(data: ActivarCuentaDto): Observable<{message: string}> {
        return this.http.post<{message: string}>(`${this.API_URL}/auth-token/activate`, data);
    }

    resendActivationEmail(email: string): Observable<{message: string}> {
        return this.http.post<{message: string}>(`${this.API_URL}/auth-token/resend`, { email });
    }

    logout(): void {
        localStorage.removeItem('access_token');
    }

    isLoggedIn(): boolean {
        return !!localStorage.getItem('access_token');
    }

    getToken(): string | null {
        return localStorage.getItem('access_token');
    }
}
