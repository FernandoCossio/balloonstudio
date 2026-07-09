import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Categoria } from '../../interface/categoria.interface';
import { CategoriaService } from '../../service/categoria.service';
import { CreateEditDialog } from '../../components/create-edit-dialog/create-edit-dialog';

@Component({
  selector: 'app-categoria-list-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    ToastModule,
    ConfirmDialogModule,
    CreateEditDialog
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './categoria-list-page.html',
  styleUrl: './categoria-list-page.scss'
})
export class CategoriaListPage implements OnInit {
  private categoriaService = inject(CategoriaService);
  private confirmationService = inject(ConfirmationService);
  private messageService = inject(MessageService);
  private cdr = inject(ChangeDetectorRef);

  categorias: Categoria[] = [];
  loading = false;

  dialogVisible = false;
  selectedCategoria: Categoria | null = null;

  ngOnInit() {
    this.loadCategorias();
  }

  loadCategorias() {
    this.loading = true;
    this.categoriaService.getCategorias().subscribe({
      next: (data) => {
        this.categorias = data;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudieron cargar las categorías.'
        });
        this.cdr.markForCheck();
      }
    });
  }

  openNew() {
    this.selectedCategoria = null;
    this.dialogVisible = true;
  }

  editCategoria(categoria: Categoria) {
    this.selectedCategoria = { ...categoria };
    this.dialogVisible = true;
  }

  deleteCategoria(categoria: Categoria) {
    if (!categoria.id) return;

    this.confirmationService.confirm({
      message: `¿Está seguro de eliminar la categoría "${categoria.nombre}"?`,
      header: 'Confirmar Eliminación',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Sí, eliminar',
      rejectLabel: 'Cancelar',
      rejectButtonStyleClass: 'p-button-text',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.loading = true;
        this.categoriaService.deleteCategoria(categoria.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Eliminado',
              detail: 'Categoría eliminada con éxito.'
            });
            this.loadCategorias();
          },
          error: () => {
            this.loading = false;
            this.messageService.add({
              severity: 'error',
              summary: 'Error',
              detail: 'No se pudo eliminar la categoría.'
            });
            this.cdr.markForCheck();
          }
        });
      }
    });
  }
}
