import { Routes } from '@angular/router';
import { CategoriaListPage } from './pages/categoria-list-page/categoria-list-page';
import { roleGuard } from '@/app/features/core/guards/role.guard';
import { ROLE_ADMINISTRADOR, ROLE_EMPLEADO } from '@/app/features/core/constants/role.constant';

export const CATEGORIA_ROUTES: Routes = [
  {
    path: '',
    canActivate: [roleGuard([ROLE_ADMINISTRADOR, ROLE_EMPLEADO])],
    component: CategoriaListPage
  }
];
