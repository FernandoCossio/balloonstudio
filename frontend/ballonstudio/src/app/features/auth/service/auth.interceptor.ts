import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Interceptor funcional que adjunta automáticamente el JWT
 * en el header Authorization de cada petición HTTP saliente.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const token = localStorage.getItem('access_token');

    console.log(`[authInterceptor] ${req.method} ${req.url}`);
    console.log('[authInterceptor] token presente:', token ? `${token.substring(0, 20)}...` : 'NULL ❌');

    if (!token) {
        console.warn('[authInterceptor] ⚠️ Sin token JWT – la petición irá sin Authorization (espera 401).');
        return next(req);
    }

    const authReq = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
    });

    console.log('[authInterceptor] ✅ Authorization header inyectado correctamente.');
    return next(authReq);
};
