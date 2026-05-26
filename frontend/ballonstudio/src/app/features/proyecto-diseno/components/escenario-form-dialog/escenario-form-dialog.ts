// features/proyecto-diseno/components/escenario-form-dialog/escenario-form-dialog.ts
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';
import { ProyectoDisenoService } from '../../services/proyecto-diseno.service';
import { EscenarioBaseRequest } from '../../interfaces/proyecto-diseno.interface';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { FileUploadModule } from 'primeng/fileupload';

// Dimensiones fijas del lienzo — deben coincidir con CANVAS_W/H en design-canvas.ts
const LIENZO_ANCHO = 1600;
const LIENZO_ALTO  = 900;

@Component({
  selector: 'app-escenario-form-dialog',
  imports: [FormsModule, InputTextModule, TextareaModule, FileUploadModule],
  templateUrl: './escenario-form-dialog.html',
  styleUrl: './escenario-form-dialog.scss'
})
export class EscenarioFormDialogComponent {

  private ref             = inject(DynamicDialogRef);
  private config          = inject(DynamicDialogConfig);
  private proyectoService = inject(ProyectoDisenoService);

  readonly cargando            = signal(false);
  readonly nombre              = signal('');
  readonly descripcion         = signal('');
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
      nombre:             this.nombre().trim(),
      descripcion:        this.descripcion().trim() || undefined,
      dimensionesAnchoPx: LIENZO_ANCHO,
      dimensionesAltoPx:  LIENZO_ALTO
    };

    this.proyectoService.createEscenario(this.proyectoId, request).subscribe({
      next: (escenario) => {
        const archivo = this.archivoSeleccionado();
        if (!archivo) {
          this.cargando.set(false);
          this.ref.close(escenario);
          return;
        }
        this.proyectoService
          .uploadImagenEscenario(this.proyectoId, escenario.id, archivo)
          .subscribe({
            next: (escenarioConImagen) => {
              this.cargando.set(false);
              this.ref.close(escenarioConImagen);
            },
            error: () => {
              this.cargando.set(false);
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