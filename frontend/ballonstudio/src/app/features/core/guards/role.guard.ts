import { inject } from '@angular/core';
import type { CanActivateFn } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService, type AppRole } from '@/app/features/auth/service/auth.service';

export function roleGuard(allowed: AppRole | AppRole[]): CanActivateFn {
    const allowedRoles = Array.isArray(allowed) ? allowed : [allowed];

    return () => {
        const auth = inject(AuthService);
        const router = inject(Router);

        if (!auth.isLoggedIn()) {
            return router.createUrlTree(['/auth/login']);
        }

        if (auth.hasAnyRole(allowedRoles)) return true;

        return router.createUrlTree(['/pages/access']);
    };
}
