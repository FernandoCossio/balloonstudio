// features/proyecto-diseno/interfaces/proyecto-diseno.interface.ts

export interface ProyectoDisenoRequest {
  nombre: string;
  descripcion?: string;
  estado?: string;
  fechaEvento?: string;   // ISO date string 'YYYY-MM-DD'
  lugarEvento?: string;
  numeroMetadato?: string;
}

export interface ProyectoDisenoResponse {
  id: number;
  nombre: string;
  descripcion: string | null;
  estado: string;
  fechaEvento: string | null;
  lugarEvento: string | null;
  numeroMetadato: string | null;
  costoRealTotal: number | null;
  escenarioBaseId: number | null;   // escenario activo por defecto
  fechaCreacion: string;
  fechaUltimaModificacion: string;
  escenarios: EscenarioBaseResponse[];
}

export interface EscenarioBaseRequest {
  nombre: string;
  descripcion?: string;
  dimensionesAltoPx?: number;
  dimensionesAnchoPx?: number;
}

export interface EscenarioBaseResponse {
  id: number;
  nombre: string;
  descripcion: string | null;
  imagenUrl: string | null;
  dimensionesAltoPx: number | null;
  dimensionesAnchoPx: number | null;
  activo: boolean;
  elementos: ElementoLienzoResponse[];
}

export interface ElementoLienzoRequest {
  articuloId: number;
  cantidad: number;
  posX: number;
  posY: number;
  width: number;
  height: number;
  scaleX: number;
  scaleY: number;
  rotacionDeg: number;
  opacity: number;
  zIndex: number;
  layer: 'mid' | 'main';
}

export interface ElementoLienzoResponse {
  id: number;
  articuloId: number;
  nombreArticulo: string;
  imagenUrl: string | null;
  costoAdquisicion: number;
  porcentajeGanancia: number;
  cantidad: number;
  posX: number;
  posY: number;
  width: number;
  height: number;
  scaleX: number;
  scaleY: number;
  rotacionDeg: number;
  opacity: number;
  zIndex: number;
  layer: 'mid' | 'main';
}