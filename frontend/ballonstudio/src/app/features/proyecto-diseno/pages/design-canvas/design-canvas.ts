import {
  Component, inject, signal, ViewChild,
  AfterViewInit, NgZone
} from '@angular/core';
import { CdkDropList, CdkDragDrop } from '@angular/cdk/drag-drop';
import { StageComponent, CoreShapeComponent, NgKonvaEventObject } from 'ng2-konva';
import { StageConfig } from 'konva/lib/Stage';
import { ImageConfig } from 'konva/lib/shapes/Image';
import Konva from 'konva';

import { CanvasStateService } from '@/app/features/proyecto-diseno/services/canvas-state.service';
import { CatalogoSidebar } from './components/catalogo-sidebar/catalogo-sidebar';
import { PricingPanel } from './components/pricing-panel/pricing-panel';
import { ArticuloInventarioDto } from '@/app/features/proyecto-diseno/interfaces/articulo-inventario-dto.interface';

@Component({
  selector: 'app-design-canvas',
  imports: [
    CdkDropList,
    StageComponent,
    CoreShapeComponent,
    CatalogoSidebar,
    PricingPanel
  ],
  templateUrl: './design-canvas.html',
  styleUrl: './design-canvas.scss'
})
export class DesignCanvas implements AfterViewInit {

  readonly canvasState = inject(CanvasStateService);
  private ngZone = inject(NgZone);

  // ── ViewChild: .getNode() es la API correcta en ng2-konva 12 ─────────────
  @ViewChild('stageRef')       stageRef!: any;
  @ViewChild('transformerRef') transformerRef!: any;

  readonly stageConfig = signal<StageConfig>({
    width:  1100,
    height: 620
  });

  readonly backgroundConfig = signal({
    x: 0, y: 0,
    width:  1100,
    height: 620,
    fill:   '#f5f0eb'
  });

  // Mapa instanceId → HTMLImageElement para Konva
  // Konva necesita el objeto Image nativo, no una URL string
  readonly imageElements = signal<Map<string, HTMLImageElement>>(new Map());

  ngAfterViewInit(): void {
    // Ajustar canvas al ancho real del contenedor
    const wrapper = document.querySelector('.canvas-wrapper') as HTMLElement;
    if (wrapper) {
      this.stageConfig.update(c => ({ ...c, width: wrapper.clientWidth }));
    }
  }

  // ── Construir config de imagen para ko-image ──────────────────────────────
  // Konva requiere el objeto Image nativo en config.image, no una URL
  getImageConfig(item: any): ImageConfig {
    const el = this.imageElements().get(item.instanceId);
    return {
      ...item.config,
      image: el ?? undefined,
      name:  item.instanceId    // usado para identificar el nodo en el stage
    };
  }

  // ── Drop desde el sidebar ─────────────────────────────────────────────────
  onArticuloDrop(event: CdkDragDrop<ArticuloInventarioDto[]>): void {
    const articulo: ArticuloInventarioDto = event.item.data;

    // .getNode() retorna el Konva.Stage
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
      // Dimensiones proporcionales, máx 150px
      const maxSize = 150;
      const ratio   = Math.min(maxSize / img.width, maxSize / img.height);
      const w = img.width  * ratio;
      const h = img.height * ratio;

      // ngZone porque el callback del Image no corre dentro de Angular
      this.ngZone.run(() => {
        this.canvasState.addItem(articulo, dropX - w / 2, dropY - h / 2);

        // Guardar el elemento Image nativo para pasarlo a ko-image
        const instanceId = this.canvasState.items().at(-1)!.instanceId;
        this.imageElements.update(map => {
          const updated = new Map(map);
          updated.set(instanceId, img);
          return updated;
        });
      });
    };

    img.onerror = () => {
      this.ngZone.run(() => {
        this.canvasState.addItem(articulo, dropX, dropY);
      });
    };

    img.src = articulo.imagenUrl ?? '';
  }

  // ── Selección y transformer ───────────────────────────────────────────────
  onItemClick(event: NgKonvaEventObject<MouseEvent>, instanceId: string): void {
    this.canvasState.selectItem(instanceId);
    this.canvasState.bringToFront(instanceId);

    const tr = this.transformerRef.getNode() as Konva.Transformer;
    // event.event.target es el nodo Konva, no el DOM element
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
    const stage = this.stageRef.getNode() as Konva.Stage;
    const dataUrl = stage.toDataURL({ pixelRatio: 2 });
    const link    = document.createElement('a');
    link.download = `diseno-evento-${Date.now()}.png`;
    link.href     = dataUrl;
    link.click();
  }

  // Limpia el imageElements cuando se elimina un item del canvas
  onRemoveItem(instanceId: string): void {
    this.imageElements.update(map => {
      const updated = new Map(map);
      updated.delete(instanceId);
      return updated;
    });
    this.canvasState.removeItem(instanceId);
  }
}