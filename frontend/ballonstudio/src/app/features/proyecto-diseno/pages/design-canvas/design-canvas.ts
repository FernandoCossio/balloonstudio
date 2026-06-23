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
import { forkJoin } from 'rxjs';

import { DialogModule } from 'primeng/dialog';
import { IaForm } from './components/ia-form/ia-form';

import { CanvasStateService } from '@/app/features/proyecto-diseno/services/canvas-state.service';
import { ProyectoDisenoService } from '@/app/features/proyecto-diseno/services/proyecto-diseno.service';
import { EscenarioTabsComponent } from '../../components/escenario-tabs/escenario-tabs';
import { ArticuloInventarioDto } from '@/app/features/proyecto-diseno/interfaces/articulo-inventario-dto.interface';
import { EscenarioBaseResponse } from '@/app/features/proyecto-diseno/interfaces/proyecto-diseno.interface';
import { CatalogoSidebar } from './components/catalogo-sidebar/catalogo-sidebar';
import { PricingPanel } from './components/pricing-panel/pricing-panel';
import { Location } from '@angular/common';
import { API_URL } from '@/enviroment/enviroment';

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
    ButtonModule,
    DialogModule,
    IaForm
  ],
  providers: [MessageService],
  templateUrl: './design-canvas.html',
  styleUrl: './design-canvas.scss'
})
export class DesignCanvas implements AfterViewInit, OnDestroy {

  private location = inject(Location);
  readonly canvasState    = inject(CanvasStateService);
  private ngZone          = inject(NgZone);
  private route           = inject(ActivatedRoute);
  private proyectoService = inject(ProyectoDisenoService);
  private messageService  = inject(MessageService);

  @ViewChild('stageRef')         stageRef!: any;
  @ViewChild('transformerRef')   transformerRef!: any;
  @ViewChild('canvasWrapperRef') canvasWrapperRef!: ElementRef<HTMLElement>;
  @ViewChild('catalogoSidebarRef') catalogoSidebar!: CatalogoSidebar;

  readonly mostrarIaDialog = signal(false);

  onIaConsultada(articulos: ArticuloInventarioDto[]): void {
    if (this.catalogoSidebar) {
      this.catalogoSidebar.setRecomendacionesIa(articulos);
    }
    this.mostrarIaDialog.set(false);
    this.messageService.add({
      severity: 'success',
      summary: 'Recomendaciones cargadas',
      detail: `Se obtuvieron ${articulos.length} artículos sugeridos por la IA.`
    });
  }

  // El stage adopta el tamaño del wrapper — se inicializa con valores temporales
  // y se actualiza en initKonvaListeners() una vez que el DOM está listo.
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

  // ResizeObserver para mantener el stage al tamaño del wrapper
  private resizeObserver!: ResizeObserver;

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
    this.canvasState.reset();
    this.imageElements.set(new Map());
    this.limpiarFondo();

    const proyectoId = Number(this.route.snapshot.paramMap.get('proyectoId'));
    if (proyectoId) {
      this.proyectoService.getById(proyectoId).subscribe(proyecto => {
        this.canvasState.cargarProyecto(proyecto);
        const escenario = this.canvasState.escenarioActual();
        if (escenario?.imagenUrl) {
          this.precargarImagenesEscenario(escenario);
          this.cargarFondoYFitScreen(escenario.imagenUrl);
        } else {
          this.imageElements.set(new Map());
          this.limpiarFondo();
        }
      });
    }
  }

  // Llamado desde el template una vez que el ko-stage está en el DOM
  private initKonvaListeners(): void {
    if (this.stage) return;   // ya inicializado
    this.stage = this.stageRef.getNode() as Konva.Stage;
    if (!this.stage) return;

    // ── Ajustar el stage al tamaño real del wrapper ────────────────────────
    // Esto es crítico: el canvas de Konva debe coincidir con el contenedor
    // para que el pan no muestre áreas recortadas fuera del canvas HTML.
    this.resizeStageToWrapper();
    this.resizeObserver = new ResizeObserver(() => this.resizeStageToWrapper());
    this.resizeObserver.observe(this.canvasWrapperRef.nativeElement);

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
    this.transformEndHandler = () => {
      const tr = this.transformerRef?.getNode() as Konva.Transformer;
      if (!tr) return;

      tr.nodes().forEach((node: Konva.Node) => {
        const instanceId = node.name();
        if (!instanceId) return;

        const newW  = Math.max(10, node.width()  * node.scaleX());
        const newH  = Math.max(10, node.height() * node.scaleY());

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
    this.resizeObserver?.disconnect();

    if (!this.stage) return;
    const container = this.stage.container();
    container.removeEventListener('wheel',     this.wheelListener);
    container.removeEventListener('mousedown', this.middleDownListener);
    window.removeEventListener(  'mousemove',  this.middleMoveListener);
    window.removeEventListener(  'mouseup',    this.middleUpListener);
    this.stage.off('transformend', this.transformEndHandler);
  }

  // ── Resize: sincroniza el canvas de Konva con el wrapper ─────────────────
  // El stage debe tener exactamente el tamaño del contenedor para que el pan
  // no deje áreas recortadas al mover el contenido fuera del canvas HTML.

  private resizeStageToWrapper(): void {
    const wrapper = this.canvasWrapperRef?.nativeElement;
    if (!this.stage || !wrapper) return;

    const w = wrapper.clientWidth;
    const h = wrapper.clientHeight;

    this.stage.width(w);
    this.stage.height(h);
    this.stage.batchDraw();

    // Actualizar el signal para que Angular conozca el nuevo tamaño
    // (evita que un re-render posterior revierta las dimensiones)
    this.ngZone.run(() => {
      this.stageConfig.set({ width: w, height: h });
    });
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

  private resolverUrlDeVista(item: any, vista: string): { url: string; mirror: boolean } {
    let mirror = false;
    let targetVista = vista;

    if (vista === 'DIAGONAL_IZQ') {
      targetVista = 'DIAGONAL';
      mirror = true;
    } else if (vista === 'LATERAL_IZQ') {
      targetVista = 'LATERAL';
      mirror = true;
    }

    const normalizeUrl = (url: string | null): string => {
      if (!url) return '';
      if (url.startsWith('http://') || url.startsWith('https://')) {
        return url;
      }
      return `${API_URL}/${url}`;
    };

    // Si tiene la lista de imágenes, buscamos por tipoVista
    if (item.imagenes && item.imagenes.length > 0) {
      const match = item.imagenes.find((img: any) => img.tipoVista === targetVista);
      if (match) {
        return { url: normalizeUrl(match.url), mirror };
      }
    }

    // Fallback: usar la imagen principal (imagenUrl) del item
    return { url: normalizeUrl(item.imagenUrl), mirror: false };
  }

  private precargarImagenesEscenario(escenario: EscenarioBaseResponse): void {
    this.canvasState.items().forEach(item => {
      this.cargarImagenDeVista(item, item.vistaActual);
    });
  }

  private cargarImagenDeVista(item: any, vista: string): void {
    const res = this.resolverUrlDeVista(item, vista);
    if (!res.url) return;

    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      this.ngZone.run(() => {
        this.imageElements.update(map => {
          const updated = new Map(map);
          updated.set(item.instanceId, img);
          return updated;
        });

        // Aplicar efecto espejo en scaleX según corresponda
        const scaleXAbs = Math.abs(item.config.scaleX || 1);
        const newScaleX = res.mirror ? -scaleXAbs : scaleXAbs;
        if (item.config.scaleX !== newScaleX) {
          this.canvasState.updateConfig(item.instanceId, { scaleX: newScaleX });
        }
      });
    };
    img.src = res.url;
  }

  rotarVistaSeleccionada(): void {
    const selected = this.canvasState.selectedItem();
    if (!selected) return;

    const vistasSecuencia = ['FRONTAL', 'DIAGONAL', 'LATERAL', 'TRASERO', 'LATERAL_IZQ', 'DIAGONAL_IZQ'];
    const indexActual = vistasSecuencia.indexOf(selected.vistaActual);
    const nextIndex = (indexActual + 1) % vistasSecuencia.length;
    const nuevaVista = vistasSecuencia[nextIndex];

    this.canvasState.updateConfig(selected.instanceId, { rotation: 0 }); // resetear rotación de Konva
    this.canvasState.updateVistaActual(selected.instanceId, nuevaVista);
    
    const updatedSelected = { ...selected, vistaActual: nuevaVista };
    this.cargarImagenDeVista(updatedSelected, nuevaVista);
  }

  // ── Guardar ───────────────────────────────────────────────────────────────

  guardarCanvas(): void {
    const proyecto  = this.canvasState.proyectoActual();
    const escenario = this.canvasState.escenarioActual();
    if (!proyecto || !escenario) return;

    this.guardando.set(true);
    
    // Guardar el estado del canvas actual en memoria para tener los datos más recientes
    this.canvasState.guardarEscenarioActualEnMemoria();

    const requests = this.canvasState.escenarios().map(esc => {
      const elRequests: any[] = esc.elementos.map(el => ({
        articuloId: el.articuloId,
        cantidad: el.cantidad,
        posX: el.posX,
        posY: el.posY,
        width: el.width,
        height: el.height,
        scaleX: el.scaleX,
        scaleY: el.scaleY,
        rotacionDeg: el.rotacionDeg,
        opacity: el.opacity,
        zIndex: el.zIndex,
        layer: el.layer,
        vistaActual: el.vistaActual
      }));
      return this.proyectoService.guardarElementos(proyecto.id, esc.id, elRequests);
    });

    if (requests.length === 0) {
      this.guardando.set(false);
      return;
    }

    forkJoin(requests).subscribe({
      next: () => {
        this.guardando.set(false);
        this.messageService.add({
          severity: 'success', summary: 'Guardado exitoso',
          detail: 'Todos los escenarios del proyecto se guardaron correctamente'
        });
      },
      error: (err) => {
        this.guardando.set(false);
        const errMsg = err?.error?.message || 'Verifica tu conexión e intenta de nuevo';
        this.messageService.add({
          severity: 'error', summary: 'Error al guardar',
          detail: errMsg
        });
      }
    });
  }

  // ── Config de imagen para ko-image ────────────────────────────────────────

  getImageConfig(item: any): ImageConfig {
    const el = this.imageElements().get(item.instanceId);
    const scaleX = item.config.scaleX ?? 1;
    const offsetX = scaleX < 0 ? (item.config.width ?? 120) : 0;
    return { ...item.config, image: el ?? undefined, name: item.instanceId, offsetX };
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
        const itemCreado = this.canvasState.items().at(-1)!;
        this.imageElements.update(map => {
          const updated = new Map(map);
          updated.set(itemCreado.instanceId, img);
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

  goBack(): void {
    this.location.back();
  }
}