import { Routes } from '@angular/router';

export default [
    {
        path: '',
        loadComponent: () =>
            import('./pages/articulo-inventario/articulo-inventario').then(m => m.ArticuloInventario)
    },
    {
        path: 'nuevo',
        loadComponent: () =>
            import('./pages/articulo-inventario-form/articulo-inventario-form').then(m => m.ArticuloInventarioForm)
    },
    {
        path: 'editar/:id',
        loadComponent: () =>
            import('./pages/articulo-inventario-form/articulo-inventario-form').then(m => m.ArticuloInventarioForm)
    }
] as Routes;
