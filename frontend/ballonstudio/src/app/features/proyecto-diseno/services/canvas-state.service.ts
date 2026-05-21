// features/proyecto-diseno/services/canvas-state.service.ts
import { Injectable, computed, signal } from '@angular/core';
import { CanvasItem } from '../interfaces/canvas-item.interface';
import { CanvasItemConfig } from '../interfaces/canvas-item-config.interface';
import { ArticuloInventarioDto } from '../interfaces/articulo-inventario-dto.interface';
import {
  ProyectoDisenoResponse,
  EscenarioBaseResponse,
  ElementoLienzoRequest,
  ElementoLienzoResponse
} from '../interfaces/proyecto-diseno.interface';

@Injectable({ providedIn: 'root' })
export class CanvasStateService {

  // ── Estado principal ──────────────────────────────────────────────────────
  readonly items      = signal<CanvasItem[]>([]);
  readonly selectedId = signal<string | null>(null);

  // ── Estado de proyecto y escenario ───────────────────────────────────────
  readonly proyectoActual    = signal<ProyectoDisenoResponse | null>(null);
  readonly escenarioActual   = signal<EscenarioBaseResponse | null>(null);
  readonly escenarios        = signal<EscenarioBaseResponse[]>([]);
  readonly imagenEscenarioUrl = signal<string | null>(null);
  readonly guardando          = signal<boolean>(false);

  // ── Selectores computados ─────────────────────────────────────────────────
  readonly midItems  = computed(() => this.items().filter(i => i.layer === 'mid'));
  readonly mainItems = computed(() => this.items().filter(i => i.layer === 'main'));

  readonly selectedItem = computed(() =>
    this.items().find(i => i.instanceId === this.selectedId()) ?? null
  );

  readonly precioTotal = computed(() =>
    this.items().reduce((acc, item) => {
      const precioUnitario = item.costo * (1 + item.porcentajeGanancia / 100);
      return acc + precioUnitario * item.cantidad;
    }, 0)
  );

  readonly resumenItems = computed(() =>
    this.items().map(item => ({
      instanceId: item.instanceId,
      nombre:     item.nombre,
      cantidad:   item.cantidad,
      imagenUrl:  item.imagenUrl,
      subtotal:   item.costo * (1 + item.porcentajeGanancia / 100) * item.cantidad
    }))
  );

  // ── Cargar proyecto completo ──────────────────────────────────────────────
  cargarProyecto(proyecto: ProyectoDisenoResponse): void {
    this.proyectoActual.set(proyecto);
    this.escenarios.set(proyecto.escenarios);

    // Abrir el escenario activo por defecto, o el primero disponible
    const escenarioInicial = proyecto.escenarios.find(
      e => e.id === proyecto.escenarioBaseId
    ) ?? proyecto.escenarios[0] ?? null;

    if (escenarioInicial) {
      this.cargarEscenario(escenarioInicial);
    }
  }

  // Reconstruye el canvas desde los ElementoLienzoResponse del backend
  cargarEscenario(escenario: EscenarioBaseResponse): void {
    this.escenarioActual.set(escenario);
    this.imagenEscenarioUrl.set(escenario.imagenUrl);
    this.selectedId.set(null);

    const itemsReconstruidos: CanvasItem[] = escenario.elementos.map((el, idx) => ({
      instanceId:        crypto.randomUUID(),
      articuloId:        el.articuloId,
      nombre:            el.nombreArticulo,
      imagenUrl:         el.imagenUrl ?? '',
      costo:             Number(el.costoAdquisicion),
      porcentajeGanancia: Number(el.porcentajeGanancia),
      cantidad:          el.cantidad,
      layer:             el.layer,
      config: {
        x:         el.posX,
        y:         el.posY,
        width:     el.width,
        height:    el.height,
        draggable: true,
        visible:   true,
        opacity:   el.opacity,
        scaleX:    el.scaleX,
        scaleY:    el.scaleY,
        rotation:  el.rotacionDeg
      }
    }));

    this.items.set(itemsReconstruidos);
  }

  // Serializa el estado actual del canvas al formato que espera el backend
  toElementoLienzoRequests(): ElementoLienzoRequest[] {
    return this.items().map((item, idx) => ({
      articuloId:  item.articuloId,
      cantidad:    item.cantidad,
      posX:        item.config.x,
      posY:        item.config.y,
      width:       item.config.width,
      height:      item.config.height,
      scaleX:      item.config.scaleX,
      scaleY:      item.config.scaleY,
      rotacionDeg: item.config.rotation,
      opacity:     item.config.opacity,
      zIndex:      idx,             // el índice en el array = z-index real
      layer:       item.layer
    }));
  }

  // ── Mutaciones existentes (sin cambios) ───────────────────────────────────

  addItem(articulo: ArticuloInventarioDto, x: number, y: number): void {
    const newItem: CanvasItem = {
      instanceId:         crypto.randomUUID(),
      articuloId:         articulo.id,
      nombre:             articulo.nombre,
      imagenUrl:          articulo.imagenUrl ?? '',
      costo:              Number(articulo.costoAdquisicion),
      porcentajeGanancia: Number(articulo.porcentajeGanancia),
      cantidad:           1,
      layer:              'main',
      config: {
        x, y,
        width: 120, height: 120,
        draggable: true, visible: true,
        opacity: 1, scaleX: 1, scaleY: 1, rotation: 0
      }
    };
    this.items.update(prev => [...prev, newItem]);
    this.selectedId.set(newItem.instanceId);
  }

  updatePosition(instanceId: string, x: number, y: number): void {
    this.items.update(prev => prev.map(i =>
      i.instanceId === instanceId ? { ...i, config: { ...i.config, x, y } } : i
    ));
  }

  updateConfig(instanceId: string, partial: Partial<CanvasItemConfig>): void {
    this.items.update(prev => prev.map(i =>
      i.instanceId === instanceId ? { ...i, config: { ...i.config, ...partial } } : i
    ));
  }

  updateCantidad(instanceId: string, cantidad: number): void {
    this.items.update(prev => prev.map(i =>
      i.instanceId === instanceId ? { ...i, cantidad: Math.max(1, cantidad) } : i
    ));
  }

  selectItem(instanceId: string): void {
    this.selectedId.set(instanceId);
  }

  removeItem(instanceId: string): void {
    this.items.update(prev => prev.filter(i => i.instanceId !== instanceId));
    if (this.selectedId() === instanceId) this.selectedId.set(null);
  }

  clearCanvas(): void {
    this.items.set([]);
    this.selectedId.set(null);
  }

  bringToFront(instanceId: string): void {
    this.items.update(prev => {
      const copy = [...prev];
      const idx  = copy.findIndex(i => i.instanceId === instanceId);
      if (idx === -1 || idx === copy.length - 1) return prev;
      const [item] = copy.splice(idx, 1);
      return [...copy, item];
    });
  }

  sendToBack(instanceId: string): void {
    this.items.update(prev => {
      const copy = [...prev];
      const idx  = copy.findIndex(i => i.instanceId === instanceId);
      if (idx === -1 || idx === 0) return prev;
      const [item] = copy.splice(idx, 1);
      return [item, ...copy];
    });
  }

  moveUp(instanceId: string): void {
    this.items.update(prev => {
      const copy = [...prev];
      const idx  = copy.findIndex(i => i.instanceId === instanceId);
      if (idx === -1 || idx === copy.length - 1) return prev;
      [copy[idx], copy[idx + 1]] = [copy[idx + 1], copy[idx]];
      return [...copy];
    });
  }

  moveDown(instanceId: string): void {
    this.items.update(prev => {
      const copy = [...prev];
      const idx  = copy.findIndex(i => i.instanceId === instanceId);
      if (idx <= 0) return prev;
      [copy[idx], copy[idx - 1]] = [copy[idx - 1], copy[idx]];
      return [...copy];
    });
  }

  reorderItems(fromIndex: number, toIndex: number): void {
    this.items.update(prev => {
      const copy = [...prev];
      const [item] = copy.splice(fromIndex, 1);
      copy.splice(toIndex, 0, item);
      return copy;
    });
  }
}