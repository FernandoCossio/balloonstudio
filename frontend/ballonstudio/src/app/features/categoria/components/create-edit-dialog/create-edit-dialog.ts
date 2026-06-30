import { Component, EventEmitter, Input, Output, inject, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { Categoria } from '../../interface/categoria.interface';
import { CategoriaService } from '../../service/categoria.service';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-categoria-create-edit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DialogModule,
    ButtonModule,
    InputTextModule,
    TextareaModule
  ],
  templateUrl: './create-edit-dialog.html',
  styleUrl: './create-edit-dialog.scss'
})
export class CreateEditDialog implements OnChanges {
  @Input() visible = false;
  @Input() categoria: Categoria | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() saved = new EventEmitter<void>();

  private categoriaService = inject(CategoriaService);
  private messageService = inject(MessageService);
  private cdr = inject(ChangeDetectorRef);

  formValue: Categoria = {
    nombre: '',
    descripcion: ''
  };

  saving = false;

  ngOnChanges(changes: SimpleChanges) {
    if (changes['categoria']) {
      if (this.categoria) {
        this.formValue = { ...this.categoria };
      } else {
        this.formValue = { nombre: '', descripcion: '' };
      }
      this.cdr.markForCheck();
    }
  }

  close() {
    this.visibleChange.emit(false);
  }

  save() {
    if (!this.formValue.nombre || !this.formValue.nombre.trim()) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Campo Requerido',
        detail: 'El nombre de la categoría es obligatorio.'
      });
      return;
    }

    this.saving = true;
    const obs = this.categoria && this.categoria.id
      ? this.categoriaService.updateCategoria(this.categoria.id, this.formValue)
      : this.categoriaService.createCategoria(this.formValue);

    obs.subscribe({
      next: () => {
        this.saving = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Guardado',
          detail: 'Categoría guardada correctamente.'
        });
        this.saved.emit();
        this.close();
      },
      error: () => {
        this.saving = false;
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Hubo un error al intentar guardar la categoría.'
        });
        this.cdr.markForCheck();
      }
    });
  }
}
