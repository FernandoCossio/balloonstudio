import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { SkeletonModule } from 'primeng/skeleton';
import { MessageService } from 'primeng/api';

import { EmpleadoRequest, EmpleadoService } from '../../service/empleado.service';

type StepNum = 1 | 2 | 3;

@Component({
    selector: 'app-empleado-form',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        FormsModule,
        ButtonModule,
        InputTextModule,
        ToastModule,
        TooltipModule,
        SkeletonModule,
        RouterModule
    ],
    providers: [MessageService],
    templateUrl: './empleado-form.html',
    styleUrl: './empleado-form.scss'
})
export class EmpleadoForm implements OnInit {
    private fb = inject(FormBuilder);
    private svc = inject(EmpleadoService);
    private router = inject(Router);
    private route = inject(ActivatedRoute);
    private msgSvc = inject(MessageService);

    // ─── Signals for State ───────────────────────────────────────────────
    step = signal<StepNum>(1);
    isEdit = signal(false);
    editId = signal<number | null>(null);
    loading = signal(false);
    saving = signal(false);

    // Form definition
    form!: FormGroup;

    steps = [
        { num: 1, label: 'Datos Básicos' },
        { num: 2, label: 'Cuenta y Roles' },
        { num: 3, label: 'Confirmación' }
    ];

    ngOnInit() {
        this.initForm();

        const idParam = this.route.snapshot.paramMap.get('id');
        if (idParam) {
            const id = Number(idParam);
            if (!isNaN(id)) {
                this.isEdit.set(true);
                this.editId.set(id);
                this.loadForEdit(id);
            }
        }
    }

    private initForm() {
        this.form = this.fb.group({
            step1: this.fb.group({
                nombreCompleto: ['', [Validators.required, Validators.minLength(3)]],
                telefono: ['', [Validators.pattern('^[0-9+ ]*$')]]
            }),
            step2: this.fb.group({
                email: ['', [Validators.required, Validators.email]],
                username: ['']
            })
        });
    }

    private loadForEdit(id: number) {
        this.loading.set(true);
        this.svc.getById(id).subscribe({
            next: (emp) => {
                this.form.patchValue({
                    step1: {
                        nombreCompleto: emp.nombreCompleto,
                        telefono: emp.telefono || ''
                    },
                    step2: {
                        email: emp.email,
                        username: emp.username
                    }
                });
                this.loading.set(false);
            },
            error: (err) => {
                this.msgSvc.add({
                    severity: 'error',
                    summary: 'Error',
                    detail: 'No se pudo cargar la información del empleado.'
                });
                this.loading.set(false);
            }
        });
    }

    // ─── Form Accessors for Templates ────────────────────────────────────────
    get step1Group(): FormGroup {
        return this.form?.get('step1') as FormGroup;
    }

    get step2Group(): FormGroup {
        return this.form?.get('step2') as FormGroup;
    }

    // ─── Wizard Navigation ───────────────────────────────────────────────────
    canGoNext(): boolean {
        if (!this.form) return false;
        const s1 = this.step1Group;
        const s2 = this.step2Group;
        if (this.step() === 1) return s1 ? s1.valid : false;
        if (this.step() === 2) return s2 ? s2.valid : false;
        return true;
    }

    next() {
        if (!this.canGoNext()) {
            this.form.get(`step${this.step()}`)!.markAllAsTouched();
            return;
        }
        if (this.step() < 3) {
            this.step.update(s => (s + 1) as StepNum);
        }
    }

    prev() {
        if (this.step() > 1) {
            this.step.update(s => (s - 1) as StepNum);
        }
    }

    canGoToStep(n: number): boolean {
        if (n === this.step()) return true;
        if (n === 1) return true;
        if (n === 2) return this.step1Group.valid;
        if (n === 3) return this.step1Group.valid && this.step2Group.valid;
        return false;
    }

    goToStep(n: number) {
        if (this.canGoToStep(n)) {
            this.step.set(n as StepNum);
        } else {
            if (n > this.step()) {
                this.form.get(`step${this.step()}`)?.markAllAsTouched();
            }
        }
    }

    // ─── Save Action ─────────────────────────────────────────────────────────
    save() {
        if (this.isEdit()) {
            if (this.form.invalid) {
                this.form.markAllAsTouched();
                return;
            }
        } else {
            if (!this.step1Group.valid || !this.step2Group.valid) {
                this.msgSvc.add({
                    severity: 'warn',
                    summary: 'Formulario Incompleto',
                    detail: 'Por favor, complete todos los campos requeridos en cada paso.'
                });
                return;
            }
        }

        this.saving.set(true);
        const s1 = this.step1Group.value;
        const s2 = this.step2Group.value;

        const request: EmpleadoRequest = {
            nombreCompleto: s1.nombreCompleto,
            email: s2.email,
            telefono: s1.telefono || undefined,
            username: s2.username || undefined
        };

        const op$ = this.isEdit()
            ? this.svc.update(this.editId()!, request)
            : this.svc.create(request);

        op$.subscribe({
            next: (saved) => {
                this.saving.set(false);
                this.msgSvc.add({
                    severity: 'success',
                    summary: this.isEdit() ? 'Empleado Actualizado' : 'Empleado Creado',
                    detail: `El empleado "${saved.nombreCompleto}" ha sido guardado exitosamente.`
                });
                setTimeout(() => {
                    this.router.navigate(['/empleados']);
                }, 1500);
            },
            error: (err) => {
                this.saving.set(false);
                let detailMsg = 'Ocurrió un error al guardar el empleado.';
                if (err?.error?.message) {
                    detailMsg = err.error.message;
                }
                this.msgSvc.add({
                    severity: 'error',
                    summary: 'Error al Guardar',
                    detail: detailMsg
                });
            }
        });
    }

    cancel() {
        this.router.navigate(['/empleados']);
    }

    onlyPhoneChars(event: KeyboardEvent) {
        const allowed = /^[0-9+ ]$/;
        if (!allowed.test(event.key) && event.key !== 'Enter' && event.key !== 'Backspace' && event.key !== 'Tab') {
            event.preventDefault();
        }
    }

    onPhoneInput(event: Event, controlName: string) {
        const input = event.target as HTMLInputElement;
        const rawValue = input.value;
        const filteredValue = rawValue.replace(/[^0-9+ ]/g, '');
        if (rawValue !== filteredValue) {
            input.value = filteredValue;
            this.form.get(controlName)?.setValue(filteredValue, { emitEvent: false });
        }
    }
}

