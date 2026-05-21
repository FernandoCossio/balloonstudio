export interface ArticuloInventarioDto {
  id: number;
  nombre: string;
  descripcion: string | null;
  tipoArticulo: string;
  estado: string;
  costoAdquisicion: number;
  porcentajeGanancia: number;
  stockTotal: number;
  imagenUrl: string | null;
  imagenThumbnailUrl: string | null;
  categorias: string[];
}