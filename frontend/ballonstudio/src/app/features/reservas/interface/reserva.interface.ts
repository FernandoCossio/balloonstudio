export interface ReservaResponse {
  id: number;
  usuarioId: number;
  nombreCliente: string;
  emailCliente: string;
  telefonoCliente: string;
  proyectoId: number;
  nombreProyecto: string;
  fechaEvento: string; // ISO date string (YYYY-MM-DD)
  lugarEvento: string;
  cotizacionId: number;
  costoArticulos: number;
  costoFlete: number;
  costoArmado: number;
  total: number;
  montoAnticipo: number;
  estado: string; // PENDIENTE_PAGO, CONFIRMADA, EXPIRADA, CANCELADA
  fechaReserva: string; // ISO datetime string
  fechaLimitePago: string; // ISO datetime string
  fechaConfirmacion?: string; // ISO datetime string
  empleadoAsignadoId?: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
