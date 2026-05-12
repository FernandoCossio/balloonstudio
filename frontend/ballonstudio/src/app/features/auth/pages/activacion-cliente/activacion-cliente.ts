import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../service/auth';

import { ButtonModule } from 'primeng/button';
import { PasswordModule } from 'primeng/password';
import { RippleModule } from 'primeng/ripple';

@Component({
    selector: 'app-activacion-cliente',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterModule, ButtonModule, PasswordModule, RippleModule],
    templateUrl: './activacion-cliente.html',
    styleUrl: './activacion-cliente.scss'
})
export class ActivacionCliente implements OnInit {
    private cdr = inject(ChangeDetectorRef);
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private fb = inject(FormBuilder);
    private authService = inject(AuthService);

    token: string | null = null;
    isTokenValid = false;
    loading = true;
    error: string | null = null;
    successMessage: string | null = null;
    resending = false;
    currentYear = new Date().getFullYear();

    activationForm: FormGroup = this.fb.group({
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });

    get hasMinLength(): boolean {
        return (this.activationForm.get('password')?.value || '').length >= 8;
    }

    get hasUppercase(): boolean {
        return /[A-Z]/.test(this.activationForm.get('password')?.value || '');
    }

    get hasNumber(): boolean {
        return /[0-9]/.test(this.activationForm.get('password')?.value || '');
    }

    ngOnInit(): void {
        this.token = this.route.snapshot.queryParamMap.get('token');
        if (this.token) {
            this.verifyToken();
        } else {
            this.loading = false;
            this.error = 'No se proporcionó un token de activación.';
        }
    }

    private verifyToken(): void {
        this.authService.verifyToken(this.token!).subscribe({
            next: () => {
                this.isTokenValid = true;
                this.loading = false;
                this.cdr.markForCheck();
            },
            error: (err) => {
                this.error = err.error?.message || 'El enlace es inválido o ha expirado.';
                this.loading = false;
                this.cdr.markForCheck();
            }
        });
    }

    private passwordMatchValidator(g: FormGroup) {
        return g.get('password')?.value === g.get('confirmPassword')?.value
            ? null : { mismatch: true };
    }

    onSubmit(): void {
        if (this.activationForm.valid && this.token) {
            this.loading = true;
            const data = {
                token: this.token,
                password: this.activationForm.value.password,
                confirmPassword: this.activationForm.value.confirmPassword
            };

            this.authService.activateAccount(data).subscribe({
                next: (res) => {
                    this.successMessage = res.message;
                    this.loading = false;
                    this.cdr.markForCheck();
                    setTimeout(() => this.router.navigate(['/auth/login']), 3000);
                },
                error: (err) => {
                    this.error = err.error?.message || 'Error al activar la cuenta.';
                    this.loading = false;
                    this.cdr.markForCheck();
                }
            });
        }
    }

    onResendEmail(): void {
        const email = prompt('Por favor, ingresa tu correo electrónico para reenviar el enlace:');
        if (email) {
            this.resending = true;
            this.authService.resendActivationEmail(email).subscribe({
                next: (res) => {
                    alert(res.message);
                    this.resending = false;
                    this.cdr.markForCheck();
                },
                error: (err) => {
                    alert(err.error?.message || 'Error al reenviar el email.');
                    this.resending = false;
                    this.cdr.markForCheck();
                }
            });
        }
    }
}
