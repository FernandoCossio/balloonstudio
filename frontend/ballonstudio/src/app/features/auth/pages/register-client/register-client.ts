import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputTextModule } from 'primeng/inputtext';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { AuthService, RegistrarClienteDto } from '../../service/auth';

@Component({
    selector: 'app-register-client',
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
        ToastModule
    ],
    providers: [MessageService],
    templateUrl: './register-client.html',
    styleUrl: './register-client.scss'
})
export class RegisterClient {
    private authService = inject(AuthService);
    private router = inject(Router);
    private messageService = inject(MessageService);

    nombre = '';
    email = '';
    telefono = '';
    password = '';
    acceptTerms = false;
    passwordVisible = false;
    isLoading = false;

    togglePasswordVisible() {
        this.passwordVisible = !this.passwordVisible;
    }

    onRegister() {
        const nombre = this.nombre.trim();
        const email = this.email.trim();
        const telefono = this.telefono.trim();

        if (!nombre || !email || !telefono) {
            this.messageService.add({
                severity: 'error',
                summary: 'Error',
                detail: 'Por favor, completa todos los campos requeridos.'
            });
            return;
        }

        if (!this.acceptTerms) {
            this.messageService.add({
                severity: 'error',
                summary: 'Error',
                detail: 'Debes aceptar los términos y condiciones para continuar.'
            });
            return;
        }

        this.isLoading = true;
        const username = email.includes('@') ? email.split('@')[0] : email;
        const payload: RegistrarClienteDto = {
            username,
            email,
            nombreCompleto: nombre,
            telefono
        };

        this.authService.register(payload).subscribe({
            next: () => {
                this.isLoading = false;
                this.messageService.add({
                    severity: 'success',
                    summary: 'Registro Exitoso',
                    detail: 'Tu cuenta ha sido creada. Ahora puedes iniciar sesión.'
                });
                setTimeout(() => {
                    this.router.navigate(['/auth/login']);
                }, 1500);
            },
            error: (err) => {
                this.isLoading = false;
                this.messageService.add({
                    severity: 'error',
                    summary: 'Error en el Registro',
                    detail: err.error?.message || 'No se pudo completar el registro'
                });
            }
        });
    }
}
