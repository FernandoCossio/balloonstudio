// canvas-state.service.ts
import { Injectable, computed, signal } from '@angular/core';
import { CanvasItem } from '../interfaces/canvas-item.interface';
import { ArticuloInventarioDto } from '../interfaces/articulo-inventario-dto.interface';
import { CanvasItemConfig } from '../interfaces/canvas-item-config.interface';

@Injectable({
  providedIn: 'root'
})
export class CanvasStateService {

  // ── Estado principal ──────────────────────────────────────────────────────
  readonly items   = signal<CanvasItem[]>([]);
  readonly selectedId = signal<string | null>(null);

  // ── Selectores computados ─────────────────────────────────────────────────
  readonly midItems  = computed(() => this.items().filter(i => i.layer === 'mid'));
  readonly mainItems = computed(() => this.items().filter(i => i.layer === 'main'));

  readonly selectedItem = computed(() =>
    this.items().find(i => i.instanceId === this.selectedId()) ?? null
  );

  // Precio total reactivo: costo × (1 + porcentaje/100) × cantidad
  readonly precioTotal = computed(() =>
    this.items().reduce((acc, item) => {
      const precioUnitario = item.costo * (1 + item.porcentajeGanancia / 100);
      return acc + precioUnitario * item.cantidad;
    }, 0)
  );

  readonly resumenItems = computed(() =>
    this.items().map(item => ({
      instanceId:  item.instanceId,
      nombre:      item.nombre,
      cantidad:    item.cantidad,
      imagenUrl:   item.imagenUrl,
      subtotal:    item.costo * (1 + item.porcentajeGanancia / 100) * item.cantidad
    }))
  );

  // ── Mutaciones ────────────────────────────────────────────────────────────

  addItem(articulo: ArticuloInventarioDto, x: number, y: number): void {
    const newItem: CanvasItem = {
      instanceId:        crypto.randomUUID(),
      articuloId:        articulo.id,
      nombre:            articulo.nombre,
      imagenUrl:         articulo.imagenUrl ?? '',
      costo:             Number(articulo.costoAdquisicion),
      porcentajeGanancia: Number(articulo.porcentajeGanancia),
      cantidad:          1,
      layer:             'main',
      config: {
        x,
        y,
        width:    120,
        height:   120,
        draggable: true,
        visible:   true,
        opacity:   1,
        scaleX:    1,
        scaleY:    1,
        rotation:  0
      }
    };
    // Se agrega al final del array → z-index más alto (encima de todo)
    this.items.update(prev => [...prev, newItem]);
    this.selectedId.set(newItem.instanceId);
  }

  updatePosition(instanceId: string, x: number, y: number): void {
    this.items.update(prev => prev.map(i =>
      i.instanceId === instanceId
        ? { ...i, config: { ...i.config, x, y } }
        : i
    ));
  }

  updateConfig(instanceId: string, partial: Partial<CanvasItemConfig>): void {
    this.items.update(prev => prev.map(i =>
      i.instanceId === instanceId
        ? { ...i, config: { ...i.config, ...partial } }
        : i
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

  // ── Control de z-index (orden del array = profundidad) ────────────────────

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

  // Reordenamiento desde el panel de capas (drag-drop del sidebar)
  reorderItems(fromIndex: number, toIndex: number): void {
    this.items.update(prev => {
      const copy = [...prev];
      const [item] = copy.splice(fromIndex, 1);
      copy.splice(toIndex, 0, item);
      return copy;
    });
  }
}