import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { APP_INITIALIZER, ApplicationConfig, PLATFORM_ID, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, withEnabledBlockingInitialNavigation, withInMemoryScrolling } from '@angular/router';
import { definePreset, updateSurfacePalette } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';
import { providePrimeNG } from 'primeng/config';
import { catchError, firstValueFrom, of } from 'rxjs';
import { appRoutes } from './app.routes';
import { authInterceptor } from './app/features/core/interceptors/auth.interceptor';
import { AuthService } from './app/features/auth/service/auth.service';

type Rgb = { r: number; g: number; b: number };

function clampByte(value: number): number {
    return Math.max(0, Math.min(255, Math.round(value)));
}

function hexToRgb(hex: string): Rgb {
    const normalized = hex.replace('#', '');
    if (normalized.length !== 6) {
        throw new Error(`Invalid hex color: ${hex}`);
    }
    const r = parseInt(normalized.slice(0, 2), 16);
    const g = parseInt(normalized.slice(2, 4), 16);
    const b = parseInt(normalized.slice(4, 6), 16);
    return { r, g, b };
}

function rgbToHex(rgb: Rgb): string {
    const toHex = (n: number) => clampByte(n).toString(16).padStart(2, '0');
    return `#${toHex(rgb.r)}${toHex(rgb.g)}${toHex(rgb.b)}`.toUpperCase();
}

function mix(from: Rgb, to: Rgb, amount: number): Rgb {
    const t = Math.max(0, Math.min(1, amount));
    return {
        r: from.r + (to.r - from.r) * t,
        g: from.g + (to.g - from.g) * t,
        b: from.b + (to.b - from.b) * t
    };
}

function shade(hex: string, amount: number): string {
    const base = hexToRgb(hex);
    const target: Rgb = amount >= 0 ? { r: 255, g: 255, b: 255 } : { r: 0, g: 0, b: 0 };
    return rgbToHex(mix(base, target, Math.abs(amount)));
}

const BALLOON_PRIMARY_500 = '#C2185B';

const BalloonStudioPrimary = {
    50: shade(BALLOON_PRIMARY_500, 0.92),
    100: shade(BALLOON_PRIMARY_500, 0.84),
    200: shade(BALLOON_PRIMARY_500, 0.72),
    300: shade(BALLOON_PRIMARY_500, 0.58),
    400: shade(BALLOON_PRIMARY_500, 0.4),
    500: BALLOON_PRIMARY_500,
    600: shade(BALLOON_PRIMARY_500, -0.12),
    700: shade(BALLOON_PRIMARY_500, -0.24),
    800: shade(BALLOON_PRIMARY_500, -0.38),
    900: shade(BALLOON_PRIMARY_500, -0.52),
    950: shade(BALLOON_PRIMARY_500, -0.66)
};

const BalloonStudioPreset = definePreset(Aura, {
    semantic: {
        primary: BalloonStudioPrimary,
        colorScheme: {
            light: {
                primary: {
                    color: '{primary.500}',
                    contrastColor: '#ffffff',
                    hoverColor: '{primary.600}',
                    activeColor: '{primary.700}'
                },
                highlight: {
                    background: '{primary.50}',
                    focusBackground: '{primary.100}',
                    color: '{primary.700}',
                    focusColor: '{primary.800}'
                }
            },
            dark: {
                primary: {
                    color: '{primary.400}',
                    contrastColor: '{surface.900}',
                    hoverColor: '{primary.300}',
                    activeColor: '{primary.200}'
                },
                highlight: {
                    background: 'color-mix(in srgb, {primary.400}, transparent 84%)',
                    focusBackground: 'color-mix(in srgb, {primary.400}, transparent 76%)',
                    color: 'rgba(255,255,255,.87)',
                    focusColor: 'rgba(255,255,255,.87)'
                }
            }
        }
    }
});

const BalloonStudioSurfaceSlate = {
    0: '#ffffff',
    50: '#f8fafc',
    100: '#f1f5f9',
    200: '#e2e8f0',
    300: '#cbd5e1',
    400: '#94a3b8',
    500: '#64748b',
    600: '#475569',
    700: '#334155',
    800: '#1e293b',
    900: '#0f172a',
    950: '#020617'
};

function initBalloonStudioTheme(platformId: Object) {
    return () => {
        if (isPlatformBrowser(platformId)) {
            updateSurfacePalette(BalloonStudioSurfaceSlate);
        }
    };
}

function initAuthSession(platformId: Object, authService: AuthService) {
    return async () => {
        if (!isPlatformBrowser(platformId)) return;
        await firstValueFrom(authService.initFromSession().pipe(catchError(() => of(void 0))));
    };
}

export const appConfig: ApplicationConfig = {
    providers: [
        provideRouter(appRoutes, withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled' }), withEnabledBlockingInitialNavigation()),
        provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
        provideZonelessChangeDetection(),
        { provide: APP_INITIALIZER, multi: true, useFactory: initBalloonStudioTheme, deps: [PLATFORM_ID] },
        { provide: APP_INITIALIZER, multi: true, useFactory: initAuthSession, deps: [PLATFORM_ID, AuthService] },
        providePrimeNG({ theme: { preset: BalloonStudioPreset, options: { darkModeSelector: '.app-dark' } } })
    ]
};
