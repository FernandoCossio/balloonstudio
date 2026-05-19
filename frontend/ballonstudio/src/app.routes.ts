import { Routes } from '@angular/router';
import { AppLayout } from './app/shared/layout/component/app.layout';
import { Dashboard } from './app/shared/pages/dashboard/dashboard';
import { Notfound } from './app/shared/pages/notfound/notfound';

export const appRoutes: Routes = [
    {
        path: '',
        component: AppLayout,
        children: [
            { path: '', component: Dashboard },
            { path: 'pages', loadChildren: () => import('./app/shared/pages/shared.routes') },
            { path: 'inventario', loadChildren: () => import('./app/features/articulo-inventario/articulo-inventario.routes') },
        ]
    },
    { path: 'notfound', component: Notfound },
    { path: 'auth', loadChildren: () => import('./app/features/auth/auth.routes') },
    { path: '**', redirectTo: '/notfound' }
];
