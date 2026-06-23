import { Routes } from "@angular/router";

export default [
//   {
//     path: '',
//     loadComponent: () =>
//       import('./pages/proyecto-list/proyecto-list')
//         .then(m => m.ProyectoListPage)
//   },
  {
    path: ':proyectoId/canvas',
    loadComponent: () =>
      import('./pages/design-canvas/design-canvas')
        .then(m => m.DesignCanvas)
  },
  {
    path: ':proyectoId/reserva',
    loadComponent: () =>
      import('./pages/reserva/reserva')
        .then(m => m.Reserva)
  },
  {
    path: ':proyectoId/reserva/pago',
    loadComponent: () =>
      import('./pages/metodo-tarjeta/metodo-tarjeta')
        .then(m => m.MetodoTarjeta)
  },
  {
    path: ':proyectoId/reserva/qr',
    loadComponent: () =>
      import('./pages/metodo-qr/metodo-qr')
        .then(m => m.MetodoQr)
  },
] as Routes;