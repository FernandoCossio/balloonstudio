import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { BadgeModule } from 'primeng/badge';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';
import { MenuModule } from 'primeng/menu';
import { SkeletonModule } from 'primeng/skeleton';
import { ArticuloInventarioResponse, ArticuloInventarioService } from '../../service/articulo-inventario.service';

type FilterTab = 'TODOS' | 'CONSUMIBLE' | 'REUTILIZABLE';
type ComplejidadLevel = 'FACIL' | 'MEDIO' | 'PROFESIONAL';

@Component({
    selector: 'app-articulo-inventario',
    standalone: true,
    imports: [
        CommonModule, RouterModule, FormsModule,
        ButtonModule, TableModule, TagModule, BadgeModule,
        TooltipModule, ConfirmDialogModule, ToastModule,
        MenuModule, SkeletonModule
    ],
    providers: [ConfirmationService, MessageService],
    templateUrl: './articulo-inventario.html',
    styleUrl: './articulo-inventario.scss'
})
export class ArticuloInventario implements OnInit {
    private svc = inject(ArticuloInventarioService);
    private confirmSvc = inject(ConfirmationService);
    private msgSvc = inject(MessageService);
    private router = inject(Router);

    // ─── State ───────────────────────────────────────────────────────────────
    articulos = signal<ArticuloInventarioResponse[]>([]);
    loading = signal(true);
    activeFilter = signal<FilterTab>('TODOS');
    searchQuery = signal('');

    // ─── Computed ────────────────────────────────────────────────────────────
    articulosFiltrados = computed(() => {
        let list = this.articulos();
        const q = this.searchQuery().toLowerCase();
        if (q) list = list.filter(a => a.nombre.toLowerCase().includes(q));
        if (this.activeFilter() !== 'TODOS') {
            list = list.filter(a => a.tipoArticulo === this.activeFilter());
        }
        return list;
    });

    totalArticulos = computed(() => this.articulos().length);
    stockBajoCount = computed(() => this.articulos().filter(a => a.estado === 'STOCK_BAJO').length);
    mantenimientoCount = computed(() => this.articulos().filter(a => a.estado === 'EN_MANTENIMIENTO').length);

    valorTotalStock = computed(() => {
        return this.articulos().reduce((acc, a) => {
            const precio = (a.costoAdquisicion ?? 0) * (1 + (a.porcentajeGanancia ?? 0) / 100);
            return acc + precio * (a.stockTotal ?? 0);
        }, 0);
    });

    // ─── Lifecycle ───────────────────────────────────────────────────────────
    ngOnInit() {
        this.loadArticulos();
    }

    loadArticulos() {
        this.loading.set(true);
        this.svc.getAll().subscribe({
            next: (data) => {
                this.articulos.set(data);
                this.loading.set(false);
            },
            error: () => {
                this.msgSvc.add({ severity: 'error', summary: 'Error', detail: 'No se pudo cargar el inventario' });
                this.loading.set(false);
            }
        });
    }

    // ─── Actions ─────────────────────────────────────────────────────────────
    setFilter(tab: FilterTab) {
        this.activeFilter.set(tab);
    }

    openNew() {
        this.router.navigate(['/inventario/nuevo']);
    }

    editArticulo(articulo: ArticuloInventarioResponse) {
        this.router.navigate(['/inventario/editar', articulo.id]);
    }

    confirmDelete(articulo: ArticuloInventarioResponse) {
        this.confirmSvc.confirm({
            message: `¿Eliminar "${articulo.nombre}"? Esta acción no se puede deshacer.`,
            header: 'Confirmar eliminación',
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Eliminar',
            rejectLabel: 'Cancelar',
            acceptButtonStyleClass: 'p-button-danger',
            accept: () => this.doDelete(articulo.id)
        });
    }

    private doDelete(id: number) {
        this.svc.delete(id).subscribe({
            next: () => {
                this.articulos.update(list => list.filter(a => a.id !== id));
                this.msgSvc.add({ severity: 'success', summary: 'Eliminado', detail: 'Artículo eliminado correctamente' });
            },
            error: () => {
                this.msgSvc.add({ severity: 'error', summary: 'Error', detail: 'No se pudo eliminar el artículo' });
            }
        });
    }

    // ─── UI helpers ──────────────────────────────────────────────────────────
    estadoSeverity(estado: string): 'success' | 'warn' | 'danger' | 'secondary' {
        switch (estado) {
            case 'DISPONIBLE': return 'success';
            case 'STOCK_BAJO': return 'warn';
            case 'EN_MANTENIMIENTO': return 'danger';
            default: return 'secondary';
        }
    }

    estadoLabel(estado: string): string {
        const map: Record<string, string> = {
            DISPONIBLE: 'Disponible',
            STOCK_BAJO: 'Stock Bajo',
            EN_MANTENIMIENTO: 'En Mantenimiento',
            INACTIVO: 'Inactivo'
        };
        return map[estado] ?? estado;
    }

    tipoLabel(tipo: string): string {
        return tipo === 'CONSUMIBLE' ? 'Consumible' : 'Reutilizable';
    }

    tipoClass(tipo: string): string {
        return tipo === 'CONSUMIBLE' ? 'badge-consumible' : 'badge-reutilizable';
    }

    complejidadDots(nivel: ComplejidadLevel | undefined): number[] {
        const map: Record<ComplejidadLevel, number> = { FACIL: 1, MEDIO: 2, PROFESIONAL: 3 };
        const count = nivel ? (map[nivel] ?? 1) : 1;
        return Array(3).fill(0).map((_, i) => i < count ? 1 : 0);
    }

    getAvatarLetter(nombre: string): string {
        return nombre?.charAt(0)?.toUpperCase() ?? '?';
    }

    getAvatarColor(nombre: string): string {
        const colors = ['#e91e8c', '#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];
        let hash = 0;
        for (let i = 0; i < nombre.length; i++) hash = nombre.charCodeAt(i) + ((hash << 5) - hash);
        return colors[Math.abs(hash) % colors.length];
    }
}
