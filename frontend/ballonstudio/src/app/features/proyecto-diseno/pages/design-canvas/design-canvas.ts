// features/proyecto-diseno/pages/design-canvas/design-canvas.ts
import {
  Component, inject, signal, ViewChild,
  AfterViewInit, NgZone
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CdkDropList, CdkDragDrop } from '@angular/cdk/drag-drop';
import { StageComponent, CoreShapeComponent, NgKonvaEventObject } from 'ng2-konva';
import { StageConfig } from 'konva/lib/Stage';
import { ImageConfig } from 'konva/lib/shapes/Image';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import Konva from 'konva';

import { CanvasStateService } from '@/app/features/proyecto-diseno/services/canvas-state.service';
import { ProyectoDisenoService } from '@/app/features/proyecto-diseno/services/proyecto-diseno.service';
import { EscenarioTabsComponent } from '../../components/escenario-tabs/escenario-tabs';
import { ArticuloInventarioDto } from '@/app/features/proyecto-diseno/interfaces/articulo-inventario-dto.interface';
import { EscenarioBaseResponse } from '@/app/features/proyecto-diseno/interfaces/proyecto-diseno.interface';
import { CatalogoSidebar } from './components/catalogo-sidebar/catalogo-sidebar';
import { PricingPanel } from './components/pricing-panel/pricing-panel';

@Component({
  selector: 'app-design-canvas',
  imports: [
    CdkDropList,
    StageComponent,
    CoreShapeComponent,
    CatalogoSidebar,
    PricingPanel,
    EscenarioTabsComponent,
    ToastModule,
    ButtonModule
  ],
  providers: [MessageService],
  templateUrl: './design-canvas.html',
  styleUrl: './design-canvas.scss'
})
export class DesignCanvas implements AfterViewInit {

  readonly canvasState    = inject(CanvasStateService);
  private ngZone          = inject(NgZone);
  private route           = inject(ActivatedRoute);
  private proyectoService = inject(ProyectoDisenoService);
  private messageService  = inject(MessageService);

  @ViewChild('stageRef')       stageRef!: any;
  @ViewChild('transformerRef') transformerRef!: any;

  readonly stageConfig = signal<StageConfig>({ width: 1100, height: 620 });

  // backgroundConfig ahora puede tener image (HTMLImageElement) o fill (color)
  readonly backgroundConfig = signal<any>({
    x: 0, y: 0, width: 1100, height: 620, fill: '#f5f0eb'
  });

  readonly imageElements = signal<Map<string, HTMLImageElement>>(new Map());
  readonly guardando     = signal(false);

  ngAfterViewInit(): void {
    const wrapper = document.querySelector('.canvas-wrapper') as HTMLElement;
    if (wrapper) {
      this.stageConfig.update(c => ({ ...c, width: wrapper.clientWidth }));
      this.backgroundConfig.update(c => ({ ...c, width: wrapper.clientWidth }));
    }

    const proyectoId = Number(this.route.snapshot.paramMap.get('proyectoId'));
    if (proyectoId) {
      this.proyectoService.getById(proyectoId).subscribe(proyecto => {
        this.canvasState.cargarProyecto(proyecto);
        const escenario = this.canvasState.escenarioActual();
        if (escenario) {
          this.precargarImagenesEscenario(escenario);
          this.actualizarFondo(escenario.imagenUrl);
        }
      });
    }
  }

  // ── Imagen de fondo del escenario ─────────────────────────────────────────

  private actualizarFondo(imagenUrl: string | null): void {
    if (!imagenUrl) {
      this.backgroundConfig.update(c => {
        const { image, ...rest } = c;   // quitar image si había
        return { ...rest, fill: '#f5f0eb' };
      });
      return;
    }
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      this.ngZone.run(() => {
        this.backgroundConfig.update(c => {
          const { fill, ...rest } = c;  // quitar fill si había
          return { ...rest, image: img };
        });
      });
    };
    img.src = imagenUrl;
  }

  // ── Cambio de escenario desde los tabs ────────────────────────────────────

  onEscenarioCambiado(escenario: EscenarioBaseResponse): void {
    this.imageElements.set(new Map());
    this.precargarImagenesEscenario(escenario);
    this.actualizarFondo(escenario.imagenUrl);
    // Deseleccionar transformer
    const tr = this.transformerRef?.getNode() as Konva.Transformer;
    tr?.nodes([]);
    tr?.getLayer()?.batchDraw();
  }

  private precargarImagenesEscenario(escenario: EscenarioBaseResponse): void {
    this.canvasState.items().forEach(item => {
      if (!item.imagenUrl) return;
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => {
        this.ngZone.run(() => {
          this.imageElements.update(map => {
            const updated = new Map(map);
            updated.set(item.instanceId, img);
            return updated;
          });
        });
      };
      img.src = item.imagenUrl;
    });
  }

  // ── Guardar canvas ────────────────────────────────────────────────────────

  guardarCanvas(): void {
    const proyecto  = this.canvasState.proyectoActual();
    const escenario = this.canvasState.escenarioActual();
    if (!proyecto || !escenario) return;

    this.guardando.set(true);
    const elementos = this.canvasState.toElementoLienzoRequests();

    this.proyectoService.guardarElementos(proyecto.id, escenario.id, elementos).subscribe({
      next: () => {
        this.guardando.set(false);
        this.messageService.add({
          severity: 'success', summary: 'Guardado',
          detail: `Escenario "${escenario.nombre}" guardado correctamente`
        });
      },
      error: () => {
        this.guardando.set(false);
        this.messageService.add({
          severity: 'error', summary: 'Error al guardar',
          detail: 'Verifica tu conexión e intenta de nuevo'
        });
      }
    });
  }

  // ── Config de imagen para ko-image ────────────────────────────────────────

  getImageConfig(item: any): ImageConfig {
    const el = this.imageElements().get(item.instanceId);
    return { ...item.config, image: el ?? undefined, name: item.instanceId };
  }

  // ── Drop desde el sidebar ─────────────────────────────────────────────────

  onArticuloDrop(event: CdkDragDrop<ArticuloInventarioDto[]>): void {
    const articulo: ArticuloInventarioDto = event.item.data;
    const stage = this.stageRef.getNode() as Konva.Stage;
    const rect  = stage.container().getBoundingClientRect();
    const x     = event.dropPoint.x - rect.left;
    const y     = event.dropPoint.y - rect.top;
    this.cargarImagenYAgregar(articulo, x, y);
  }

  private cargarImagenYAgregar(
    articulo: ArticuloInventarioDto,
    dropX: number,
    dropY: number
  ): void {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      const maxSize = 150;
      const ratio   = Math.min(maxSize / img.width, maxSize / img.height);
      const w = img.width  * ratio;
      const h = img.height * ratio;
      this.ngZone.run(() => {
        this.canvasState.addItem(articulo, dropX - w / 2, dropY - h / 2);
        const instanceId = this.canvasState.items().at(-1)!.instanceId;
        this.imageElements.update(map => {
          const updated = new Map(map);
          updated.set(instanceId, img);
          return updated;
        });
      });
    };
    img.onerror = () => {
      this.ngZone.run(() => this.canvasState.addItem(articulo, dropX, dropY));
    };
    img.src = articulo.imagenUrl ?? '';
  }

  // ── Selección, transformer, drag ─────────────────────────────────────────

  onItemClick(event: NgKonvaEventObject<MouseEvent>, instanceId: string): void {
    this.canvasState.selectItem(instanceId);
    this.canvasState.bringToFront(instanceId);
    const tr = this.transformerRef.getNode() as Konva.Transformer;
    tr.nodes([event.event.target as unknown as Konva.Node]);
    tr.getLayer()?.batchDraw();
  }

  onStageClick(event: NgKonvaEventObject<MouseEvent>): void {
    const target = event.event.target;
    if (target === (target as any).getStage()) {
      this.canvasState.selectedId.set(null);
      const tr = this.transformerRef.getNode() as Konva.Transformer;
      tr.nodes([]);
      tr.getLayer()?.batchDraw();
    }
  }

  onDragEnd(event: NgKonvaEventObject<MouseEvent>, instanceId: string): void {
    const node = event.event.target as unknown as Konva.Node;
    this.canvasState.updatePosition(instanceId, node.x(), node.y());
  }

  // ── Export ────────────────────────────────────────────────────────────────

  exportPNG(): void {
    const stage   = this.stageRef.getNode() as Konva.Stage;
    const dataUrl = stage.toDataURL({ pixelRatio: 2 });
    const link    = document.createElement('a');
    link.download = `diseno-evento-${Date.now()}.png`;
    link.href     = dataUrl;
    link.click();
  }

  onRemoveItem(instanceId: string): void {
    this.imageElements.update(map => {
      const updated = new Map(map);
      updated.delete(instanceId);
      return updated;
    });
    this.canvasState.removeItem(instanceId);
  }
}