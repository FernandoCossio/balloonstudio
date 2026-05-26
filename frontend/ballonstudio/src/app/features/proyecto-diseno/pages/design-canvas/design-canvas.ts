// features/proyecto-diseno/pages/design-canvas/design-canvas.ts
import {
  Component, inject, signal, computed, ViewChild,
  ElementRef, AfterViewInit, OnDestroy, NgZone
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

const CANVAS_W = 1600;
const CANVAS_H = 900;
const ZOOM_MIN = 0.15;
const ZOOM_MAX = 3;

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
export class DesignCanvas implements AfterViewInit, OnDestroy {

  readonly canvasState    = inject(CanvasStateService);
  private ngZone          = inject(NgZone);
  private route           = inject(ActivatedRoute);
  private proyectoService = inject(ProyectoDisenoService);
  private messageService  = inject(MessageService);

  @ViewChild('stageRef')         stageRef!: any;
  @ViewChild('transformerRef')   transformerRef!: any;
  @ViewChild('canvasWrapperRef') canvasWrapperRef!: ElementRef<HTMLElement>;

  // El stage siempre es 1600x900 — zoom y pan se aplican vía Konva directamente
  readonly stageConfig = signal<StageConfig>({
    width:  CANVAS_W,
    height: CANVAS_H,
  });

  readonly backgroundConfig = signal<any>({
    x: 0, y: 0, width: CANVAS_W, height: CANVAS_H,
    fill: '#1e1e2e', listening: false
  });

  readonly imageElements = signal<Map<string, HTMLImageElement>>(new Map());
  readonly guardando     = signal(false);

  // Zoom actual — solo para mostrar el label, se actualiza desde Konva
  private _currentScale = signal(1);
  readonly zoomLabel    = computed(() => `${Math.round(this._currentScale() * 100)}%`);

  // Referencia al stage de Konva — se obtiene una vez en ngAfterViewInit
  private stage!: Konva.Stage;

  // Listeners registrados fuera de Angular para no disparar CD
  private wheelListener!:       (e: WheelEvent) => void;
  private middleDownListener!:  (e: MouseEvent) => void;
  private middleMoveListener!:  (e: MouseEvent) => void;
  private middleUpListener!:    (e: MouseEvent) => void;
  // transformend: persiste el nuevo tamaño/posición tras redimensionar con el transformer
  private transformEndHandler!: (e: Konva.KonvaEventObject<Event>) => void;

  // Estado interno del pan con click medio
  private _isPanningMiddle = false;
  private _panStart        = { x: 0, y: 0 };
  private _stageStart      = { x: 0, y: 0 };

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  ngAfterViewInit(): void {
    const proyectoId = Number(this.route.snapshot.paramMap.get('proyectoId'));
    if (proyectoId) {
      this.proyectoService.getById(proyectoId).subscribe(proyecto => {
        this.canvasState.cargarProyecto(proyecto);
        const escenario = this.canvasState.escenarioActual();
        if (escenario?.imagenUrl) {
          this.precargarImagenesEscenario(escenario);
          this.cargarFondoYFitScreen(escenario.imagenUrl);
        }
      });
    }
  }

  // Llamado desde el template una vez que el ko-stage está en el DOM
  // (usamos (click) del stage como señal de que Konva ya inicializó)
  private initKonvaListeners(): void {
    if (this.stage) return;   // ya inicializado
    this.stage = this.stageRef.getNode() as Konva.Stage;
    if (!this.stage) return;

    const container = this.stage.container();

    // ── Zoom con rueda — registrado FUERA de Angular para no disparar CD ──
    this.wheelListener = (e: WheelEvent) => {
      e.preventDefault();
      const oldScale = this.stage.scaleX();
      const pointer  = this.stage.getPointerPosition()!;

      const mousePointTo = {
        x: (pointer.x - this.stage.x()) / oldScale,
        y: (pointer.y - this.stage.y()) / oldScale
      };

      const direction = e.deltaY < 0 ? 1 : -1;
      const factor    = e.ctrlKey ? 0.04 : 0.10;
      const newScale  = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, oldScale + direction * factor * oldScale));

      this.stage.scale({ x: newScale, y: newScale });
      this.stage.position({
        x: pointer.x - mousePointTo.x * newScale,
        y: pointer.y - mousePointTo.y * newScale
      });
      this.stage.batchDraw();

      // Solo actualizamos el signal del label (no stageConfig)
      this.ngZone.run(() => this._currentScale.set(newScale));
    };

    container.addEventListener('wheel', this.wheelListener, { passive: false });

    // ── Pan con click medio — también en el contenedor nativo ─────────────
    this.middleDownListener = (e: MouseEvent) => {
      if (e.button !== 1) return;
      e.preventDefault();
      this._isPanningMiddle = true;
      this._panStart   = { x: e.clientX, y: e.clientY };
      this._stageStart = { x: this.stage.x(), y: this.stage.y() };
      container.style.cursor = 'grabbing';
    };

    this.middleMoveListener = (e: MouseEvent) => {
      if (!this._isPanningMiddle) return;
      this.stage.x(this._stageStart.x + (e.clientX - this._panStart.x));
      this.stage.y(this._stageStart.y + (e.clientY - this._panStart.y));
      this.stage.batchDraw();
    };

    this.middleUpListener = (e: MouseEvent) => {
      if (e.button !== 1 && this._isPanningMiddle) return;
      this._isPanningMiddle = false;
      container.style.cursor = 'default';
    };

    container.addEventListener('mousedown',  this.middleDownListener);
    window.addEventListener(   'mousemove',  this.middleMoveListener);
    window.addEventListener(   'mouseup',    this.middleUpListener);

    // ── Persistir dimensiones tras redimensionar con el transformer ────────
    // Konva modifica scaleX/scaleY del nodo al redimensionar. Lo normalizamos
    // a width/height reales y reseteamos la escala a 1 para que Angular no
    // revierta al tamaño original al re-renderizar desde el signal.
    this.transformEndHandler = () => {
      const tr = this.transformerRef?.getNode() as Konva.Transformer;
      if (!tr) return;

      tr.nodes().forEach((node: Konva.Node) => {
        const instanceId = node.name();
        if (!instanceId) return;

        // Calcular el nuevo tamaño real absorbiendo la escala del transformer
        const newW  = Math.max(10, node.width()  * node.scaleX());
        const newH  = Math.max(10, node.height() * node.scaleY());

        // Resetear escala en el nodo — el tamaño ya está en width/height
        node.width(newW);
        node.height(newH);
        node.scaleX(1);
        node.scaleY(1);

        this.ngZone.run(() => {
          this.canvasState.updateConfig(instanceId, {
            x:       node.x(),
            y:       node.y(),
            width:   newW,
            height:  newH,
            scaleX:  1,
            scaleY:  1,
            rotation: node.rotation()
          });
        });
      });

      tr.getLayer()?.batchDraw();
    };

    this.stage.on('transformend', this.transformEndHandler);
  }

  ngOnDestroy(): void {
    if (!this.stage) return;
    const container = this.stage.container();
    container.removeEventListener('wheel',     this.wheelListener);
    container.removeEventListener('mousedown', this.middleDownListener);
    window.removeEventListener(  'mousemove',  this.middleMoveListener);
    window.removeEventListener(  'mouseup',    this.middleUpListener);
    this.stage.off('transformend', this.transformEndHandler);
  }

  // ── Fit to screen ─────────────────────────────────────────────────────────

  fitToScreen(): void {
    const wrapper = this.canvasWrapperRef?.nativeElement;
    if (!this.stage || !wrapper) return;

    const scaleX = wrapper.clientWidth  / CANVAS_W;
    const scaleY = wrapper.clientHeight / CANVAS_H;
    const scale  = Math.min(scaleX, scaleY) * 0.96;

    const x = (wrapper.clientWidth  - CANVAS_W * scale) / 2;
    const y = (wrapper.clientHeight - CANVAS_H * scale) / 2;

    this.stage.scale({ x: scale, y: scale });
    this.stage.position({ x, y });
    this.stage.batchDraw();
    this._currentScale.set(scale);
  }

  // ── Zoom desde botones de toolbar ─────────────────────────────────────────

  zoomStep(delta: number): void {
    if (!this.stage) return;

    const oldScale = this.stage.scaleX();
    const newScale = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, oldScale + delta));
    const wrapper  = this.canvasWrapperRef?.nativeElement;
    const cx = (wrapper?.clientWidth  ?? CANVAS_W) / 2;
    const cy = (wrapper?.clientHeight ?? CANVAS_H) / 2;

    const mousePointTo = {
      x: (cx - this.stage.x()) / oldScale,
      y: (cy - this.stage.y()) / oldScale
    };

    this.stage.scale({ x: newScale, y: newScale });
    this.stage.position({
      x: cx - mousePointTo.x * newScale,
      y: cy - mousePointTo.y * newScale
    });
    this.stage.batchDraw();
    this._currentScale.set(newScale);
  }

  // ── Imagen de fondo (cover 1600×900) ─────────────────────────────────────

  private cargarFondoYFitScreen(imagenUrl: string): void {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      const scale   = Math.max(CANVAS_W / img.width, CANVAS_H / img.height);
      const drawW   = img.width  * scale;
      const drawH   = img.height * scale;
      const offsetX = (CANVAS_W - drawW) / 2;
      const offsetY = (CANVAS_H - drawH) / 2;

      this.ngZone.run(() => {
        this.backgroundConfig.set({
          image: img,
          x: offsetX, y: offsetY,
          width: drawW, height: drawH,
          listening: false
        });
        setTimeout(() => this.fitToScreen(), 0);
      });
    };
    img.onerror = () => {
      this.ngZone.run(() => {
        this.backgroundConfig.set({
          x: 0, y: 0, width: CANVAS_W, height: CANVAS_H,
          fill: '#1e1e2e', listening: false
        });
        setTimeout(() => this.fitToScreen(), 0);
      });
    };
    img.src = imagenUrl;
  }

  private limpiarFondo(): void {
    this.backgroundConfig.set({
      x: 0, y: 0, width: CANVAS_W, height: CANVAS_H,
      fill: '#1e1e2e', listening: false
    });
  }

  // ── Cambio de escenario ───────────────────────────────────────────────────

  onEscenarioCambiado(escenario: EscenarioBaseResponse): void {
    this.imageElements.set(new Map());
    const tr = this.transformerRef?.getNode() as Konva.Transformer;
    tr?.nodes([]);
    tr?.getLayer()?.batchDraw();

    if (escenario.imagenUrl) {
      this.precargarImagenesEscenario(escenario);
      this.cargarFondoYFitScreen(escenario.imagenUrl);
    } else {
      this.limpiarFondo();
    }
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

  // ── Guardar ───────────────────────────────────────────────────────────────

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
    if (!this.stage) return;

    const rect     = this.stage.container().getBoundingClientRect();
    const screenX  = event.dropPoint.x - rect.left;
    const screenY  = event.dropPoint.y - rect.top;
    // Convertir de coordenadas de pantalla a coordenadas lógicas del canvas
    const scale    = this.stage.scaleX();
    const logicalX = (screenX - this.stage.x()) / scale;
    const logicalY = (screenY - this.stage.y()) / scale;

    this.cargarImagenYAgregar(articulo, logicalX, logicalY);
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
    // Ignorar si el click viene de un pan con botón medio (raro, pero posible)
    if (this._isPanningMiddle) return;

    this.canvasState.selectItem(instanceId);
    const tr = this.transformerRef.getNode() as Konva.Transformer;
    tr.nodes([event.event.target as unknown as Konva.Node]);
    tr.getLayer()?.batchDraw();
  }

  onStageClick(event: NgKonvaEventObject<MouseEvent>): void {
    // Inicializar listeners de Konva la primera vez que el stage recibe un evento
    this.initKonvaListeners();

    if (this._isPanningMiddle) return;

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
    // Persistir posición + escala + rotación actuales del nodo
    // para que el estado siempre refleje lo que Konva tiene
    this.canvasState.updateConfig(instanceId, {
      x:        node.x(),
      y:        node.y(),
      scaleX:   node.scaleX(),
      scaleY:   node.scaleY(),
      rotation: node.rotation()
    });
  }

  // ── Export ────────────────────────────────────────────────────────────────

  exportPNG(): void {
    if (!this.stage) return;
    const dataUrl = this.stage.toDataURL({
      pixelRatio: CANVAS_W / this.stage.width(),
      x: 0, y: 0, width: CANVAS_W, height: CANVAS_H
    });
    const link    = document.createElement('a');
    link.download = `diseno-${this.canvasState.escenarioActual()?.nombre ?? 'evento'}-${Date.now()}.png`;
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