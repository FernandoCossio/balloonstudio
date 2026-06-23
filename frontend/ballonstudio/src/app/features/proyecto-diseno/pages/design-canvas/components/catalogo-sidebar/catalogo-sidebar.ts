import { Component, inject, output, signal, computed, effect } from '@angular/core';
import { CdkDrag, CdkDragPreview, CdkDragPlaceholder } from '@angular/cdk/drag-drop';
import { CatalogoService } from '@/app/features/proyecto-diseno/services/catalogo.service';
import { ArticuloInventarioDto } from '@/app/features/proyecto-diseno/interfaces/articulo-inventario-dto.interface';
import { CategoriaResponse } from '@/app/features/articulo-inventario/service/articulo-inventario.service';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-catalogo-sidebar',
  imports: [CdkDrag, CdkDragPreview, CdkDragPlaceholder, DecimalPipe],
  templateUrl: './catalogo-sidebar.html',
  styleUrl: './catalogo-sidebar.scss'
})
export class CatalogoSidebar {

  private catalogoService = inject(CatalogoService);

  // Emite el artículo cuando el usuario hace click directo (sin drag)
  readonly articuloSeleccionado = output<ArticuloInventarioDto>();

  readonly categoriaSeleccionada = signal<string>('');
  readonly busqueda             = signal<string>('');

  readonly categorias = signal<CategoriaResponse[]>([]);
  readonly articulos = signal<ArticuloInventarioDto[]>([]);
  readonly articulosRecomendadosIa = signal<ArticuloInventarioDto[]>([]);

  constructor() {
    this.catalogoService.getCategorias().subscribe(cats => {
      this.categorias.set(cats);
    });

    effect(() => {
      const cat = this.categoriaSeleccionada();
      if (cat === 'IA_RECOMMENDATIONS') {
        this.articulos.set(this.articulosRecomendadosIa());
      } else {
        const catId = cat ? Number(cat) : undefined;
        this.catalogoService.getCatalogo(undefined, undefined, catId).subscribe(items => {
          this.articulos.set(items);
        });
      }
    }, { allowSignalWrites: true });
  }

  // Filtrado reactivo local para la barra de búsqueda
  readonly articulosFiltrados = computed(() => {
    const busq = this.busqueda().toLowerCase().trim();
    const list = this.articulos();
    if (!busq) return list;
    return list.filter(a => a.nombre.toLowerCase().includes(busq));
  });

  setRecomendacionesIa(articulos: ArticuloInventarioDto[]): void {
    this.articulosRecomendadosIa.set(articulos);
    this.categoriaSeleccionada.set('IA_RECOMMENDATIONS');
  }

  onClickArticulo(articulo: ArticuloInventarioDto): void {
    this.articuloSeleccionado.emit(articulo);
  }
}