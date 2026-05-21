// features/proyecto-diseno/pages/proyecto-list/proyecto-list.ts
import { Component, inject, signal, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ProyectoDisenoService } from '../../services/proyecto-diseno.service';
import { ProyectoDisenoResponse } from '../../interfaces/proyecto-diseno.interface';
import { ProyectoFormDialogComponent } from '../../components/proyecto-form-dialog/proyecto-form-dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-proyecto-list',
  imports: [ConfirmDialogModule, ToastModule, ButtonModule, DatePipe],
  providers: [DialogService, ConfirmationService, MessageService],
  templateUrl: './proyecto-list.html',
  styleUrl:    './proyecto-list.scss'
})
export class ProyectoListPage implements OnInit {

  private router          = inject(Router);
  private proyectoService = inject(ProyectoDisenoService);
  private dialogService   = inject(DialogService);
  private confirmService  = inject(ConfirmationService);
  private messageService  = inject(MessageService);

  readonly proyectos = signal<ProyectoDisenoResponse[]>([]);
  readonly cargando  = signal(true);

  ngOnInit(): void {
    this.cargarProyectos();
  }

  private cargarProyectos(): void {
    this.cargando.set(true);
    this.proyectoService.getAll().subscribe({
      next:  (data) => { this.proyectos.set(data); this.cargando.set(false); },
      error: ()     => this.cargando.set(false)
    });
  }

  abrirProyecto(proyecto: ProyectoDisenoResponse): void {
    this.router.navigate(['/proyectos', proyecto.id, 'canvas']);
  }

  abrirDialogCrear(): void {
    const ref = this.dialogService.open(ProyectoFormDialogComponent, {
    header: 'Nuevo proyecto',
    width:  '520px'
    });
    ref?.onClose.subscribe((nuevo: ProyectoDisenoResponse | undefined) => {
    if (!nuevo) return;
    this.proyectos.update(prev => [nuevo, ...prev]);
    this.messageService.add({
        severity: 'success', summary: 'Proyecto creado', detail: nuevo.nombre
    });
    });
  }

  abrirDialogEditar(proyecto: ProyectoDisenoResponse, event: Event): void {
    event.stopPropagation();
    const ref = this.dialogService.open(ProyectoFormDialogComponent, {
        header: 'Editar proyecto',
        width:  '520px',
        data:   { proyecto }
    });
    ref?.onClose.subscribe((actualizado: ProyectoDisenoResponse | undefined) => {
        if (!actualizado) return;
        this.proyectos.update(prev =>
            prev.map(p => p.id === actualizado.id ? actualizado : p)
        );
    });
  }

  confirmarEliminar(proyecto: ProyectoDisenoResponse, event: Event): void {
    event.stopPropagation();
    this.confirmService.confirm({
      message: `¿Eliminar el proyecto "${proyecto.nombre}"? Esta acción no se puede deshacer.`,
      accept: () => {
        this.proyectoService.delete(proyecto.id).subscribe(() => {
          this.proyectos.update(prev => prev.filter(p => p.id !== proyecto.id));
          this.messageService.add({
            severity: 'info', summary: 'Proyecto eliminado'
          });
        });
      }
    });
  }
}