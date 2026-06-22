import { ImagenArticuloResponse } from "../../articulo-inventario/service/articulo-inventario.service";
import { CanvasItemConfig } from "./canvas-item-config.interface";

export interface CanvasItem {
  instanceId: string;         // uuid único por instancia en el canvas
  articuloId: number;
  nombre: string;
  imagenUrl: string;
  costo: number;
  porcentajeGanancia: number;
  cantidad: number;
  layer: 'mid' | 'main';
  config: CanvasItemConfig;
  imagenes?: ImagenArticuloResponse[];
  vistaActual: string;
}