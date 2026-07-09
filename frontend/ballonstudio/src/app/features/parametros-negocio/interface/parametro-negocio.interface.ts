export interface ParametroNegocio {
    id?: number;
    calcularFactorEstacional: boolean;
    provisionSiniestroReutilizables: boolean;
    costoOverheadFijo: number;
    capacidadVolumetricaVehiculo: number;
    tarifaBaseViaje: number;
    tarifaKmLogistica: number;
    tarifaHoraComplejidadBaja: number;
    tarifaHoraComplejidadMedia: number;
    tarifaHoraComplejidadAlta: number;
    porcentajeSiniestralidad: number;
    fallbackPorcentajeGanancia: number;
    fallbackVidaUtilUsos: number;
    fallbackVidaUtilAnos: number;
    fallbackValorResidualPorcentaje: number;
    fallbackMantenimientoPorcentaje: number;
    fallbackDiasPreparacion: number;
    fallbackDiasLimpieza: number;
}
