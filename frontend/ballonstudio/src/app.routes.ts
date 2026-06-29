import { Routes } from '@angular/router';
import { AppLayout } from './app/shared/layout/component/app.layout';
import { Dashboard } from './app/shared/pages/dashboard/dashboard';
import { Notfound } from './app/shared/pages/notfound/notfound';

export const appRoutes: Routes = [
    {
        path: '',
        loadComponent: () => import('./app/features/inicio/pages/start-page/start-page').then(m => m.StartPage),
        pathMatch: 'full'
    },
    {
        path: '',
        component: AppLayout,
        children: [
            { path: 'dashboard', component: Dashboard },
            { path: 'pages', loadChildren: () => import('./app/shared/pages/shared.routes') },
            { path: 'inventario', loadChildren: () => import('./app/features/articulo-inventario/articulo-inventario.routes') },
            { path: 'categorias', loadChildren: () => import('./app/features/categoria/categoria.routes').then(m => m.CATEGORIA_ROUTES) },
            { path: 'empleados', loadChildren: () => import('./app/features/empleado/empleado.routes') },
            { path: 'reportes', loadChildren: () => import('./app/features/reportes/reportes.routes') },
            { path: 'reservas', loadChildren: () => import('./app/features/reservas/reserva.routes') },
            { path: 'configuracion', loadChildren: () => import('./app/features/configuracion/configuracion.routes').then(m => m.CONFIGURACION_ROUTES) },
            { path: 'parametros-contabilidad', loadChildren: () => import('./app/features/parametros-negocio/parametros-negocio.route').then(m => m.PARAMETROS_NEGOCIO_ROUTES) },
            {
                path: 'proyectos',
                loadComponent: () =>
                import('./app/features/proyecto-diseno/pages/proyecto-list/proyecto-list')
                    .then(m => m.ProyectoListPage)
            },
        ]
    },
    {
        path: '',
        children: [
            { path: 'proyectos', loadChildren: () => import('./app/features/proyecto-diseno/proyecto-diseno.routes') },
        ]
    },
    { path: 'notfound', component: Notfound },
    { path: 'auth', loadChildren: () => import('./app/features/auth/auth.routes') },
    { path: '**', redirectTo: '/notfound' }
];
