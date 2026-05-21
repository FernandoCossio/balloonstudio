// catalogo-sidebar.ts
import { Component, inject, output, signal, computed } from '@angular/core';
import { CdkDrag } from '@angular/cdk/drag-drop';
import { toSignal } from '@angular/core/rxjs-interop';
import { CatalogoService } from '@/app/features/proyecto-diseno/services/catalogo.service';
import { ArticuloInventarioDto } from '@/app/features/proyecto-diseno/interfaces/articulo-inventario-dto.interface';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-catalogo-sidebar',
  imports: [CdkDrag, DecimalPipe],
  templateUrl: './catalogo-sidebar.html',
  styleUrl: './catalogo-sidebar.scss'
})
export class CatalogoSidebar {

  private catalogoService = inject(CatalogoService);

  // Emite el artículo cuando el usuario hace click directo (sin drag)
  readonly articuloSeleccionado = output<ArticuloInventarioDto>();

  readonly tipoFiltro  = signal<string>('');
  readonly estadoFiltro = signal<string>('');
  readonly busqueda    = signal<string>('');

  private todosLosArticulos = toSignal(
    this.catalogoService.getCatalogo(),
    { initialValue: [] }
  );

  // Filtrado reactivo local — no hace requests adicionales
  readonly articulosFiltrados = computed(() => {
    const tipo    = this.tipoFiltro().toLowerCase();
    const estado  = this.estadoFiltro().toLowerCase();
    const busq    = this.busqueda().toLowerCase();

    return this.todosLosArticulos().filter(a => {
      const matchTipo   = !tipo   || a.tipoArticulo?.toLowerCase().includes(tipo);
      const matchEstado = !estado || a.estado?.toLowerCase().includes(estado);
      const matchBusq   = !busq   || a.nombre.toLowerCase().includes(busq);
      return matchTipo && matchEstado && matchBusq;
    });
  });

  readonly tiposUnicos = computed(() =>
    [...new Set(this.todosLosArticulos().map(a => a.tipoArticulo).filter(Boolean))]
  );

  onClickArticulo(articulo: ArticuloInventarioDto): void {
    this.articuloSeleccionado.emit(articulo);
  }
}