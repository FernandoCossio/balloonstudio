import { Routes } from '@angular/router';
import { roleGuard } from '@/app/features/core/guards/role.guard';
import { ROLE_ADMINISTRADOR } from '@/app/features/core/constants/role.constant';

export default [
    {
        path: '',
        canActivate: [roleGuard(ROLE_ADMINISTRADOR)],
        loadComponent: () =>
            import('./pages/empleado-list/empleado-list').then(m => m.EmpleadoList)
    },
    {
        path: 'nuevo',
        canActivate: [roleGuard(ROLE_ADMINISTRADOR)],
        loadComponent: () =>
            import('./pages/empleado-form/empleado-form').then(m => m.EmpleadoForm)
    },
    {
        path: 'editar/:id',
        canActivate: [roleGuard(ROLE_ADMINISTRADOR)],
        loadComponent: () =>
            import('./pages/empleado-form/empleado-form').then(m => m.EmpleadoForm)
    }
] as Routes;
