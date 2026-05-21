import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal, computed, effect } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { MessageService } from 'primeng/api';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { ArticuloInventarioRequest, ArticuloInventarioResponse, ArticuloInventarioService, ImagenArticuloResponse } from '../../service/articulo-inventario.service';
import { API_URL } from '@/enviroment/enviroment';

// ─── Moneda configurable ─────────────────────────────────────────────────────
/** Cambia aquí el símbolo que se muestra en inputs y etiquetas de moneda */
export const CURRENCY_SYMBOL = 'Bs';
/** Código ISO para el pipe currency de Angular */
export const CURRENCY_CODE = 'BOB';

type Step = 1 | 2 | 3 | 4;
type TipoArticulo = 'CONSUMIBLE' | 'REUTILIZABLE';
type NivelComplejidad = 'FACIL' | 'MEDIO' | 'PROFESIONAL';
type EstadoArticulo = 'DISPONIBLE' | 'STOCK_BAJO' | 'EN_MANTENIMIENTO' | 'INACTIVO';

@Component({
    selector: 'app-articulo-inventario-form',
    standalone: true,
    imports: [
        CommonModule, ReactiveFormsModule, FormsModule,
        ButtonModule, InputTextModule, InputNumberModule,
        TextareaModule, SelectModule, ToastModule, TooltipModule,
        ToggleSwitchModule, IconFieldModule, InputIconModule
    ],
    providers: [MessageService],
    templateUrl: './articulo-inventario-form.html',
    styleUrl: './articulo-inventario-form.scss'
})
export class ArticuloInventarioForm implements OnInit {
    private fb = inject(FormBuilder);
    private svc = inject(ArticuloInventarioService);
    private router = inject(Router);
    private route = inject(ActivatedRoute);
    private msgSvc = inject(MessageService);

    // ─── State ───────────────────────────────────────────────────────────────
    step = signal<Step>(1);
    isEdit = signal(false);
    editId = signal<number | null>(null);
    saving = signal(false);

    tipoSeleccionado = signal<TipoArticulo>('CONSUMIBLE');
    complejidadSeleccionada = signal<NivelComplejidad>('MEDIO');
    esReutilizable = signal(false);
    // ─── Moneda (accesible desde el template) ────────────────────────────────
    readonly currencySymbol = CURRENCY_SYMBOL;
    readonly currencyCode   = CURRENCY_CODE;

    // ─── Computed values ──────────────────────────────────────────────────────
    precioSugerido = computed(() => {
        const costo = this.form?.get('step3.costoAdquisicion')?.value ?? 0;
        const pct   = this.form?.get('step3.porcentajeGanancia')?.value ?? 0;
        return costo * (1 + pct / 100);
    });

    roiUsos = computed(() => {
        const costo  = this.form?.get('step3.costoAdquisicion')?.value ?? 0;
        const precio = this.precioSugerido();
        const usos   = this.form?.get('step3.vidaUtilUsos')?.value ?? 0;
        if (!precio || precio <= costo || !usos) return '--';
        const ganPorUso = precio - costo;
        return `${Math.ceil(costo / ganPorUso)} Usos`;
    });

    // ─── Steps meta ──────────────────────────────────────────────────────────
    steps = [
        { num: 1, label: 'General' },
        { num: 2, label: 'Logística' },
        { num: 3, label: 'Finanzas' },
        { num: 4, label: 'Revisión' }
    ];

    // ─── Options ─────────────────────────────────────────────────────────────
    estadoOptions = [
        { label: 'Disponible',        value: 'DISPONIBLE' },
        // { label: 'Stock Bajo',        value: 'STOCK_BAJO' },
        // { label: 'En Mantenimiento',  value: 'EN_MANTENIMIENTO' },
        { label: 'Inactivo',          value: 'INACTIVO' }
    ];

    vidaUtilTipoOptions = [
        { label: 'Usos',  value: 'usos' },
        { label: 'Años',  value: 'anos' }
    ];

    // ─── Form ─────────────────────────────────────────────────────────────────
    form!: FormGroup;

    ngOnInit() {
        this.buildForm();
        const id = this.route.snapshot.paramMap.get('id');
        if (id) {
            this.isEdit.set(true);
            this.editId.set(+id);
            this.loadForEdit(+id);
        }
    }

    private buildForm() {
        this.form = this.fb.group({
            step1: this.fb.group({
                nombre:      ['', [Validators.required, Validators.minLength(3)]],
                descripcion: [''],
                estado:      ['DISPONIBLE', Validators.required]
            }),
            step2: this.fb.group({
                stockTotal:             [null, [Validators.min(0)]],
                pesoKg:                 [null, [Validators.min(0)]],
                volumenM3:              [null, [Validators.min(0)]],
                tiempoArmadoMin:        [null, [Validators.min(0)]],
                diasPreparacionPrevios: [null, [Validators.min(0)]],
                diasLimpiezaPosteriores:[null, [Validators.min(0)]],
                mantenimientoPromedioBs:[null, [Validators.min(0)]]
            }),
            step3: this.fb.group({
                costoAdquisicion:    [null, [Validators.min(0)]],
                porcentajeGanancia:  [30,   [Validators.min(0), Validators.max(100)]],
                valorResidual:       [null, [Validators.min(0)]],
                vidaUtilAnos:        [null, [Validators.min(0)]],
                vidaUtilUsos:        [null, [Validators.min(0)]]
            })
        });
    }

    private loadForEdit(id: number) {
        this.svc.getById(id).subscribe({
            next: (a) => this.patchForm(a),
            error: () => this.msgSvc.add({ severity: 'error', summary: 'Error', detail: 'No se pudo cargar el artículo' })
        });
    }

    private patchForm(a: ArticuloInventarioResponse) {
        this.tipoSeleccionado.set(a.tipoArticulo);
        this.complejidadSeleccionada.set(a.nivelComplejidad ?? 'MEDIO');
        this.esReutilizable.set(a.tipoArticulo === 'REUTILIZABLE');

        this.form.patchValue({
            step1: { nombre: a.nombre, descripcion: a.descripcion ?? '', estado: a.estado },
            step2: {
                stockTotal:             a.stockTotal             ?? null,
                pesoKg:                 a.pesoKg                 ?? null,
                volumenM3:              a.volumenM3              ?? null,
                tiempoArmadoMin:        a.tiempoArmadoMin        ?? null,
                diasPreparacionPrevios: a.diasPreparacionPrevios  ?? null,
                diasLimpiezaPosteriores:a.diasLimpiezaPosteriores ?? null,
                mantenimientoPromedioBs:a.mantenimientoPromedioBs ?? null
            },
            step3: {
                costoAdquisicion:   a.costoAdquisicion   ?? null,
                porcentajeGanancia: a.porcentajeGanancia ?? 30,
                valorResidual:      a.valorResidual      ?? null,
                vidaUtilAnos:       a.vidaUtilAnos       ?? null,
                vidaUtilUsos:       a.vidaUtilUsos       ?? null
            }
        });
    }

    // ─── Navigation ──────────────────────────────────────────────────────────
    canGoNext(): boolean {
        if (this.step() === 1) return this.form.get('step1')!.valid;
        if (this.step() === 2) return this.form.get('step2')!.valid;
        if (this.step() === 3) return this.form.get('step3')!.valid;
        return true;
    }

    next() {
        if (!this.canGoNext()) { this.form.get(`step${this.step()}`)!.markAllAsTouched(); return; }
        if (this.step() < 4) this.step.update(s => (s + 1) as Step);
    }

    prev() {
        if (this.step() > 1) this.step.update(s => (s - 1) as Step);
    }

    goToStep(n: number) {
        if (n < this.step()) this.step.set(n as Step);
    }

    // ─── Type & Complexity selection ─────────────────────────────────────────
    selectTipo(tipo: TipoArticulo) {
        this.tipoSeleccionado.set(tipo);
        this.esReutilizable.set(tipo === 'REUTILIZABLE');
    }

    selectComplejidad(nivel: NivelComplejidad) {
        this.complejidadSeleccionada.set(nivel);
    }

    // ─── Save ─────────────────────────────────────────────────────────────────
    saveArticulo() {
        this.saving.set(true);
        const s1 = this.form.get('step1')!.value;
        const s2 = this.form.get('step2')!.value;
        const s3 = this.form.get('step3')!.value;

        const request: ArticuloInventarioRequest = {
            nombre:                  s1.nombre,
            descripcion:             s1.descripcion,
            tipoArticulo:            this.tipoSeleccionado(),
            estado:                  s1.estado as EstadoArticulo,
            nivelComplejidad:        this.complejidadSeleccionada(),
            stockTotal:              s2.stockTotal,
            pesoKg:                  s2.pesoKg,
            volumenM3:               s2.volumenM3,
            tiempoArmadoMin:         s2.tiempoArmadoMin,
            diasPreparacionPrevios:  s2.diasPreparacionPrevios,
            diasLimpiezaPosteriores: s2.diasLimpiezaPosteriores,
            mantenimientoPromedioBs: s2.mantenimientoPromedioBs,
            costoAdquisicion:        s3.costoAdquisicion,
            porcentajeGanancia:      s3.porcentajeGanancia,
            valorResidual:           s3.valorResidual,
            vidaUtilAnos:            s3.vidaUtilAnos,
            vidaUtilUsos:            s3.vidaUtilUsos
        };

        const op$ = this.isEdit()
            ? this.svc.update(this.editId()!, request)
            : this.svc.create(request);

        op$.subscribe({
            next: () => {
                this.saving.set(false);
                this.msgSvc.add({
                    severity: 'success',
                    summary: this.isEdit() ? 'Actualizado' : 'Creado',
                    detail: `Artículo ${this.isEdit() ? 'actualizado' : 'publicado'} correctamente`
                });
                setTimeout(() => this.router.navigate(['/inventario']), 1200);
            },
            error: (err) => {
                this.saving.set(false);
                this.msgSvc.add({ severity: 'error', summary: 'Error', detail: err?.error ?? 'No se pudo guardar el artículo' });
            }
        });
    }

    cancel() {
        this.router.navigate(['/inventario']);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    fieldInvalid(group: string, field: string): boolean {
        const ctrl = this.form.get(`${group}.${field}`);
        return !!(ctrl?.invalid && ctrl?.touched);
    }

    get s1() { return this.form.get('step1')!.value; }
    get s2() { return this.form.get('step2')!.value; }
    get s3() { return this.form.get('step3')!.value; }

    complejidadOptions: { label: string; value: NivelComplejidad; icon: string }[] = [
        { label: 'Fácil',         value: 'FACIL',        icon: 'pi pi-circle' },
        { label: 'Medio',         value: 'MEDIO',        icon: 'pi pi-star' },
        { label: 'Profesional',   value: 'PROFESIONAL',  icon: 'pi pi-bolt' }
    ];
}
