import { Component, inject, signal, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CatalogoService } from '@/app/features/proyecto-diseno/services/catalogo.service';
import { ArticuloInventarioDto } from '@/app/features/proyecto-diseno/interfaces/articulo-inventario-dto.interface';
import { CategoriaResponse } from '@/app/features/articulo-inventario/service/articulo-inventario.service';

@Component({
  selector: 'app-ia-form',
  imports: [CommonModule, FormsModule],
  templateUrl: './ia-form.html',
  styleUrl: './ia-form.scss'
})
export class IaForm {

  private catalogoService = inject(CatalogoService);

  // Emite la lista de artículos recomendados
  readonly consultado = output<ArticuloInventarioDto[]>();

  // Form State
  readonly searchMode = signal<'TEXT' | 'IMAGE'>('TEXT');
  readonly textPrompt = signal<string>('');
  readonly imageFile = signal<File | null>(null);
  readonly imagePreviewUrl = signal<string | null>(null);
  readonly selectedCategoriaId = signal<string>(''); // string representation of number
  readonly limit = signal<number>(5);

  readonly loading = signal<boolean>(false);
  readonly errorMessage = signal<string>('');

  // Categories list
  readonly categorias = signal<CategoriaResponse[]>([]);

  constructor() {
    this.catalogoService.getCategorias().subscribe({
      next: (cats) => this.categorias.set(cats),
      error: () => this.errorMessage.set('No se pudieron cargar las categorías')
    });
  }

  setSearchMode(mode: 'TEXT' | 'IMAGE'): void {
    this.searchMode.set(mode);
    this.errorMessage.set('');
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.imageFile.set(file);

      // Create preview URL
      const reader = new FileReader();
      reader.onload = () => {
        this.imagePreviewUrl.set(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  }

  onSubmit(): void {
    this.errorMessage.set('');
    const mode = this.searchMode();
    const limitVal = this.limit();
    const catIdVal = this.selectedCategoriaId() ? Number(this.selectedCategoriaId()) : undefined;

    if (mode === 'TEXT') {
      const prompt = this.textPrompt().trim();
      if (!prompt) {
        this.errorMessage.set('Por favor, escribe una descripción.');
        return;
      }

      this.loading.set(true);
      this.catalogoService.recomendarPorTexto(prompt, limitVal, catIdVal).subscribe({
        next: (items) => {
          this.loading.set(false);
          this.consultado.emit(items);
        },
        error: (err) => {
          this.loading.set(false);
          this.errorMessage.set(err?.error?.message || 'Error al obtener recomendaciones.');
        }
      });
    } else {
      const file = this.imageFile();
      if (!file) {
        this.errorMessage.set('Por favor, selecciona una imagen de referencia.');
        return;
      }

      this.loading.set(true);
      this.catalogoService.recomendarPorImagen(file, limitVal, catIdVal).subscribe({
        next: (items) => {
          this.loading.set(false);
          this.consultado.emit(items);
        },
        error: (err) => {
          this.loading.set(false);
          this.errorMessage.set(err?.error?.message || 'Error al obtener recomendaciones.');
        }
      });
    }
  }
}
