import { Routes } from '@angular/router';
import { roleGuard } from '@/app/features/core/guards/role.guard';
import { ROLE_ADMINISTRADOR } from '@/app/features/core/constants/role.constant';

export default [
    {
        path: '',
        canActivate: [roleGuard(ROLE_ADMINISTRADOR)],
        loadComponent: () =>
            import('./pages/articulo-inventario/articulo-inventario').then(m => m.ArticuloInventario)
    },
    {
        path: 'nuevo',
        canActivate: [roleGuard(ROLE_ADMINISTRADOR)],
        loadComponent: () =>
            import('./pages/articulo-inventario-form/articulo-inventario-form').then(m => m.ArticuloInventarioForm)
    },
    {
        path: 'editar/:id',
        canActivate: [roleGuard(ROLE_ADMINISTRADOR)],
        loadComponent: () =>
            import('./pages/articulo-inventario-form/articulo-inventario-form').then(m => m.ArticuloInventarioForm)
    }
] as Routes;
