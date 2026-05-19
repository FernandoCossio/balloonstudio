import {
    HttpInterceptorFn,
    HttpRequest,
    HttpHandlerFn,
    HttpErrorResponse
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

let isRefreshing = false;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const token = authService.getToken();

    const authReq = token ? addToken(req, token) : req;

    return next(authReq).pipe(
        catchError((error: HttpErrorResponse) => {
            const isAuthRoute = req.url.includes('/auth/refresh') || req.url.includes('/auth/login');

            if (error.status === 401 && !isAuthRoute && !isRefreshing) {
                isRefreshing = true;

                return authService.refresh().pipe(
                    switchMap(token => {
                        isRefreshing = false;
                        return next(addToken(req, token.accessToken));
                    }),
                    catchError(refreshError => {
                        isRefreshing = false;
                        authService.logout();
                        router.navigate(['/login']);
                        return throwError(() => refreshError);
                    })
                );
            }

            return throwError(() => error);
        })
    );
};

function addToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
    return req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
    });
}