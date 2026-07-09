import { Routes } from '@angular/router';

export default [
    {
        path: '',
        loadComponent: () => import('./pages/reserva-list-page/reserva-list-page').then(m => m.ReservaListPage)
    }
] as Routes;
