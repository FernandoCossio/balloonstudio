// features/proyecto-diseno/components/escenario-form-dialog/escenario-form-dialog.ts
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';
import { ProyectoDisenoService } from '../../services/proyecto-diseno.service';
import { EscenarioBaseRequest } from '../../interfaces/proyecto-diseno.interface';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { FileUploadModule } from 'primeng/fileupload';

@Component({
  selector: 'app-escenario-form-dialog',
  imports: [FormsModule, InputTextModule, TextareaModule, ButtonModule, FileUploadModule],
  templateUrl: './escenario-form-dialog.html'
})
export class EscenarioFormDialogComponent {

  private ref            = inject(DynamicDialogRef);
  private config         = inject(DynamicDialogConfig);
  private proyectoService = inject(ProyectoDisenoService);

  readonly cargando = signal(false);
  readonly nombre   = signal('');
  readonly descripcion = signal('');
  readonly archivoSeleccionado = signal<File | null>(null);

  private get proyectoId(): number {
    return this.config.data.proyectoId;
  }

  onFileSelect(event: any): void {
    this.archivoSeleccionado.set(event.files[0] ?? null);
  }

  guardar(): void {
    if (!this.nombre().trim()) return;
    this.cargando.set(true);

    const request: EscenarioBaseRequest = {
      nombre:      this.nombre().trim(),
      descripcion: this.descripcion().trim() || undefined
    };

    this.proyectoService.createEscenario(this.proyectoId, request).subscribe({
      next: (escenario) => {
        const archivo = this.archivoSeleccionado();
        if (!archivo) {
          this.cargando.set(false);
          this.ref.close(escenario);
          return;
        }
        // Si hay imagen, subirla y luego cerrar
        this.proyectoService
          .uploadImagenEscenario(this.proyectoId, escenario.id, archivo)
          .subscribe({
            next: (escenarioConImagen) => {
              this.cargando.set(false);
              this.ref.close(escenarioConImagen);
            },
            error: () => {
              this.cargando.set(false);
              // Cerramos con el escenario sin imagen — no es crítico
              this.ref.close(escenario);
            }
          });
      },
      error: () => this.cargando.set(false)
    });
  }

  cancelar(): void {
    this.ref.close();
  }
}