// features/proyecto-diseno/components/escenario-tabs/escenario-tabs.ts
import { Component, inject, output } from '@angular/core';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { ConfirmationService } from 'primeng/api';
import { CanvasStateService } from '../../services/canvas-state.service';
import { ProyectoDisenoService } from '../../services/proyecto-diseno.service';
import { EscenarioBaseResponse } from '../../interfaces/proyecto-diseno.interface';
import { EscenarioFormDialogComponent } from '../escenario-form-dialog/escenario-form-dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';

@Component({
  selector: 'app-escenario-tabs',
  imports: [ConfirmDialogModule, TooltipModule, DynamicDialogModule],
  providers: [DialogService, ConfirmationService],
  templateUrl: './escenario-tabs.html',
  styleUrl: './escenario-tabs.scss'
})
export class EscenarioTabsComponent {

  readonly canvasState    = inject(CanvasStateService);
  private proyectoService = inject(ProyectoDisenoService);
  private dialogService   = inject(DialogService);
  private confirmService  = inject(ConfirmationService);

  // El canvas padre recarga imageElements cuando cambia el escenario
  readonly escenarioCambiado = output<EscenarioBaseResponse>();

  seleccionarEscenario(escenario: EscenarioBaseResponse): void {
    if (escenario.id === this.canvasState.escenarioActual()?.id) return;
    this.canvasState.guardarEscenarioActualEnMemoria();
    this.canvasState.cargarEscenario(escenario);
    this.escenarioCambiado.emit(escenario);
  }

  // Llamado desde el botón "+" del section-header en design-canvas.html
  abrirDialogNuevoEscenario(): void {
    const proyecto = this.canvasState.proyectoActual();
    if (!proyecto) return;

    const ref = this.dialogService.open(EscenarioFormDialogComponent, {
      header: 'Nuevo escenario',
      width:  '480px',
      data:   { proyectoId: proyecto.id }
    });

    ref?.onClose.subscribe((nuevoEscenario: EscenarioBaseResponse | undefined) => {
      if (!nuevoEscenario) return;
      this.canvasState.escenarios.update(prev => [...prev, nuevoEscenario]);
      this.seleccionarEscenario(nuevoEscenario);
    });
  }

  confirmarEliminar(escenario: EscenarioBaseResponse, event: Event): void {
    event.stopPropagation();
    this.confirmService.confirm({
      message: `¿Eliminar el escenario "${escenario.nombre}"? Esta acción no se puede deshacer.`,
      accept: () => this.eliminarEscenario(escenario)
    });
  }

  private eliminarEscenario(escenario: EscenarioBaseResponse): void {
    const proyectoId = this.canvasState.proyectoActual()!.id;
    this.proyectoService.deleteEscenario(proyectoId, escenario.id).subscribe(() => {
      this.canvasState.escenarios.update(prev => prev.filter(e => e.id !== escenario.id));
      if (this.canvasState.escenarioActual()?.id === escenario.id) {
        const primero = this.canvasState.escenarios()[0];
        if (primero) this.seleccionarEscenario(primero);
        else this.canvasState.clearCanvas();
      }
    });
  }
}