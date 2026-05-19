import { Routes } from '@angular/router';
import { Access } from './access/access';
import { Error } from './error/error';

export default [
    { path: '**', redirectTo: '/notfound' },
    { path: 'access', component: Access },
    { path: 'error', component: Error },
    
] as Routes;
