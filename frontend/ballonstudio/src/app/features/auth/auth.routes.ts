import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { RegisterClient } from './pages/register-client/register-client';
import { ActivacionCliente } from './pages/activacion-cliente/activacion-cliente';

export default [
    { path: 'login', component: Login },
    { path: 'register', component: RegisterClient },
    { path: 'activar-cuenta', component: ActivacionCliente }
] as Routes;
