// features/proyecto-diseno/components/proyecto-form-dialog/proyecto-form-dialog.ts
import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';
import { ProyectoDisenoService } from '../../services/proyecto-diseno.service';
import { ProyectoDisenoRequest, ProyectoDisenoResponse } from '../../interfaces/proyecto-diseno.interface';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';

@Component({
  selector: 'app-proyecto-form-dialog',
  imports: [FormsModule, InputTextModule, TextareaModule, ButtonModule, DatePickerModule],
  templateUrl: './proyecto-form-dialog.html'
})
export class ProyectoFormDialogComponent implements OnInit {

  private ref             = inject(DynamicDialogRef);
  private config          = inject(DynamicDialogConfig);
  private proyectoService = inject(ProyectoDisenoService);

  readonly cargando     = signal(false);
  readonly nombre       = signal('');
  readonly descripcion  = signal('');
  readonly lugarEvento  = signal('');
  readonly fechaEvento  = signal<Date | null>(null);

  protected get modoEdicion(): boolean {
    return !!this.config.data?.proyecto;
  }

  protected get proyectoExistente(): ProyectoDisenoResponse | null {
    return this.config.data?.proyecto ?? null;
  }

  ngOnInit(): void {
    const p = this.proyectoExistente;
    if (!p) return;
    this.nombre.set(p.nombre);
    this.descripcion.set(p.descripcion ?? '');
    this.lugarEvento.set(p.lugarEvento ?? '');
    this.fechaEvento.set(p.fechaEvento ? new Date(p.fechaEvento) : null);
  }

  guardar(): void {
    if (!this.nombre().trim()) return;
    this.cargando.set(true);

    const request: ProyectoDisenoRequest = {
      nombre:      this.nombre().trim(),
      descripcion: this.descripcion().trim() || undefined,
      lugarEvento: this.lugarEvento().trim() || undefined,
      fechaEvento: this.fechaEvento()?.toISOString().split('T')[0] ?? undefined,
      estado:      'borrador'
    };

    const op$ = this.modoEdicion
      ? this.proyectoService.update(this.proyectoExistente!.id, request)
      : this.proyectoService.create(request);

    op$.subscribe({
      next:  (res) => { this.cargando.set(false); this.ref.close(res); },
      error: ()    => this.cargando.set(false)
    });
  }

  cancelar(): void {
    this.ref.close();
  }
}