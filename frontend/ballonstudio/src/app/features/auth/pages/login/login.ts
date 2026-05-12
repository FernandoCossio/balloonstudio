import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { AuthService } from '../../service/auth';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        RouterModule,
        ButtonModule,
        CheckboxModule,
        InputGroupModule,
        InputGroupAddonModule,
        InputTextModule,
        RippleModule,
        ToastModule
    ],
    providers: [MessageService],
    templateUrl: './login.html',
    styleUrl: './login.scss'
})
export class Login {
    username = '';
    password = '';
    rememberMe = false;
    passwordVisible = false;
    isLoading = false;

    private authService = inject(AuthService);
    private router = inject(Router);
    private messageService = inject(MessageService);

    togglePasswordVisible() {
        this.passwordVisible = !this.passwordVisible;
    }

    onLogin() {
        if (!this.username || !this.password) {
            this.messageService.add({
                severity: 'error',
                summary: 'Error',
                detail: 'Por favor, ingrese sus credenciales'
            });
            return;
        }

        this.isLoading = true;
        this.authService.login({ username: this.username, password: this.password }).subscribe({
            next: () => {
                this.messageService.add({
                    severity: 'success',
                    summary: 'Éxito',
                    detail: 'Sesión iniciada correctamente'
                });
                setTimeout(() => {
                    this.router.navigate(['/']);
                }, 1000);
            },
            error: (err) => {
                this.isLoading = false;
                this.messageService.add({
                    severity: 'error',
                    summary: 'Error de Autenticación',
                    detail: 'Usuario o contraseña incorrectos'
                });
                console.error('Login error:', err);
            }
        });
    }
}
