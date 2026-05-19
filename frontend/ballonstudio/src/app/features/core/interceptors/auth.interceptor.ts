import {
    HttpInterceptorFn,
    HttpErrorResponse,
    HttpContext,
    HttpContextToken
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, shareReplay, switchMap, throwError } from 'rxjs';
import { AuthService } from '../../auth/service/auth.service';

const IS_RETRY = new HttpContextToken<boolean>(() => false);

let refreshInFlight: ReturnType<AuthService['refresh']> | null = null;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const isAuthRoute =
        req.url.includes('/auth/refresh') ||
        req.url.includes('/auth/login') ||
        req.url.includes('/auth/logout');

    const credReq = req.clone({ withCredentials: true });

    return next(credReq).pipe(
        catchError((error: HttpErrorResponse) => {
            if (error.status === 401 && !isAuthRoute && !req.context.get(IS_RETRY)) {
                return ensureRefresh(authService).pipe(
                    switchMap(() => {
                        const retryReq = req.clone({
                            withCredentials: true,
                            context: new HttpContext().set(IS_RETRY, true)
                        });
                        return next(retryReq);
                    }),
                    catchError((refreshError) => {
                        authService.logout();
                        router.navigate(['/auth/login']);
                        return throwError(() => refreshError);
                    })
                );
            }

            return throwError(() => error);
        })
    );
};

function ensureRefresh(authService: AuthService) {
    if (!refreshInFlight) {
        refreshInFlight = authService.refresh().pipe(
            shareReplay({ bufferSize: 1, refCount: false }),
            finalize(() => { refreshInFlight = null; })
        );
    }
    return refreshInFlight;
}
