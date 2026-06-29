import { Routes } from '@angular/router';
import { roleGuard } from '@/app/features/core/guards/role.guard';
import { ROLE_ADMINISTRADOR, ROLE_EMPLEADO } from '@/app/features/core/constants/role.constant';

export default [
    {
        path: '',
        canActivate: [roleGuard([ROLE_ADMINISTRADOR, ROLE_EMPLEADO])],
        loadComponent: () =>
            import('./pages/reportes-dashboard/reportes-dashboard').then(m => m.ReportesDashboard)
    },
    {
        path: 'clientes',
        canActivate: [roleGuard([ROLE_ADMINISTRADOR, ROLE_EMPLEADO])],
        loadComponent: () =>
            import('./pages/reporte-clientes/reporte-clientes').then(m => m.ReporteClientes)
    }
] as Routes;
