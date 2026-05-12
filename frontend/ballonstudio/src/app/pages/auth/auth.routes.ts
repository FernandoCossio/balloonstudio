import { Routes } from '@angular/router';
import { Access } from './access';
import { Login } from '../../features/auth/pages/login/login';
import { RegisterClient } from '../../features/auth/pages/register-client/register-client';
import { ActivacionCliente } from '../../features/auth/pages/activacion-cliente/activacion-cliente';
import { Error } from './error';

export default [
    { path: 'access', component: Access },
    { path: 'error', component: Error },
    { path: 'login', component: Login },
    { path: 'register', component: RegisterClient },
    { path: 'activar-cuenta', component: ActivacionCliente }
] as Routes;
