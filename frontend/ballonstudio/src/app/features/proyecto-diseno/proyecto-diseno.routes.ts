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
] as Routes;