import { Routes } from '@angular/router';
import { AppLayout } from './app/layout/component/app.layout';
import { Dashboard } from './app/pages/dashboard/dashboard';
import { Notfound } from './app/pages/notfound/notfound';

export const appRoutes: Routes = [
    {
        path: '',
        component: AppLayout,
        children: [
            { path: '', component: Dashboard },
            { path: 'pages', loadChildren: () => import('./app/pages/pages.routes') },
            {
                path: 'inventario',
                loadComponent: () =>
                    import('./app/features/articulo-inventario/articulo-inventario').then(m => m.ArticuloInventario)
            },
            {
                path: 'inventario/nuevo',
                loadComponent: () =>
                    import('./app/features/articulo-inventario/articulo-inventario-form').then(m => m.ArticuloInventarioForm)
            },
            {
                path: 'inventario/editar/:id',
                loadComponent: () =>
                    import('./app/features/articulo-inventario/articulo-inventario-form').then(m => m.ArticuloInventarioForm)
            }
        ]
    },
    { path: 'notfound', component: Notfound },
    { path: 'auth', loadChildren: () => import('./app/pages/auth/auth.routes') },
    { path: '**', redirectTo: '/notfound' }
];
