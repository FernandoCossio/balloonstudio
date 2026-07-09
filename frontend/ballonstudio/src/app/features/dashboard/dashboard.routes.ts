import { Routes } from '@angular/router';
import { DashboardPage } from './pages/dashboard-page/dashboard-page';
import { roleGuard } from '@/app/features/core/guards/role.guard';
import { ROLE_ADMINISTRADOR } from '@/app/features/core/constants/role.constant';

export const DASHBOARD_ROUTES: Routes = [
  {
    path: '',
    canActivate: [roleGuard([ROLE_ADMINISTRADOR])],
    component: DashboardPage
  }
];
