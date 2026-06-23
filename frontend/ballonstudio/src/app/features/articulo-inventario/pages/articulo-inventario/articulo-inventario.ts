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
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ArticuloInventarioResponse, ArticuloInventarioService, ImagenArticuloResponse } from '../../service/articulo-inventario.service';
import { IncidenciaService, IncidenciaRequest } from '../../service/incidencia.service';
import { API_URL } from '@/enviroment/enviroment';
import { AuthService } from '@/app/features/auth/service/auth.service';
import { ROLES } from '@/app/features/core/constants/role.constant';

type FilterTab = 'TODOS' | 'CONSUMIBLE' | 'REUTILIZABLE';
type ComplejidadLevel = 'FACIL' | 'MEDIO' | 'PROFESIONAL';

@Component({
    selector: 'app-articulo-inventario',
    standalone: true,
    imports: [
        CommonModule, RouterModule, FormsModule,
        ButtonModule, TableModule, TagModule, BadgeModule,
        TooltipModule, ConfirmDialogModule, ToastModule,
        MenuModule, SkeletonModule, DialogModule, SelectModule,
        DatePickerModule, InputNumberModule, InputTextModule, TextareaModule
    ],
    providers: [ConfirmationService, MessageService],
    templateUrl: './articulo-inventario.html',
    styleUrl: './articulo-inventario.scss'
})
export class ArticuloInventario implements OnInit {
    private svc = inject(ArticuloInventarioService);
    private incidenciaSvc = inject(IncidenciaService);
    private confirmSvc = inject(ConfirmationService);
    private msgSvc = inject(MessageService);
    private router = inject(Router);
    private auth = inject(AuthService);

    isAdmin = computed(() => this.auth.hasRole(ROLES.ADMINISTRADOR));

    // ─── State ───────────────────────────────────────────────────────────────
    articulos = signal<ArticuloInventarioResponse[]>([]);
    loading = signal(true);
    activeFilter = signal<FilterTab>('TODOS');
    searchQuery = signal('');
    hoveredImageIndex = signal<Record<number, number>>({});

    showImageDialog = signal(false);
    selectedArticulo = signal<ArticuloInventarioResponse | null>(null);
    imagenes = signal<ImagenArticuloResponse[]>([]);
    isDragging = signal(false);
    uploadingImages = signal(false);

    // Incidencias State
    showIncidenciaDialog = signal(false);
    incidenciaArticulo = signal<ArticuloInventarioResponse | null>(null);
    incidenciaTipo = signal<'REPARACION' | 'MERMA_PERDIDA'>('REPARACION');
    incidenciaCantidad = signal<number>(1);
    incidenciaDescripcion = signal<string>('');
    incidenciaReservaId = signal<number | null>(null);
    incidenciaFechaRetorno = signal<Date | null>(null);
    submittingIncidencia = signal(false);

    incidenciaTipoOptions = [
        { label: 'Reparación / Mantenimiento', value: 'REPARACION' },
        { label: 'Merma / Pérdida', value: 'MERMA_PERDIDA' }
    ];

    // ─── Computed ────────────────────────────────────────────────────────────
    nuevoDisponible = computed(() => {
        const art = this.incidenciaArticulo();
        if (!art) return 0;
        const total = art.stockDisponible ?? 0;
        const cant = this.incidenciaCantidad();
        return Math.max(0, total - cant);
    });

    readonly minFecha = (() => {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        return today;
    })();
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

    goToIncidencias() {
        this.router.navigate(['/inventario/incidencias']);
    }

    openIncidenciaDialog(art: ArticuloInventarioResponse) {
        this.incidenciaArticulo.set(art);
        this.incidenciaTipo.set('REPARACION');
        this.incidenciaCantidad.set(1);
        this.incidenciaDescripcion.set('');
        this.incidenciaReservaId.set(null);
        this.incidenciaFechaRetorno.set(null);
        this.submittingIncidencia.set(false);
        this.showIncidenciaDialog.set(true);
    }

    registrarIncidencia() {
        const art = this.incidenciaArticulo();
        if (!art) return;

        if (!this.incidenciaDescripcion().trim()) {
            this.msgSvc.add({ severity: 'error', summary: 'Error', detail: 'La descripción es obligatoria' });
            return;
        }

        const qty = this.incidenciaCantidad();
        if (qty <= 0 || qty > (art.stockDisponible ?? 0)) {
            this.msgSvc.add({ severity: 'error', summary: 'Error', detail: `La cantidad debe ser mayor a 0 y no superar el stock disponible (${art.stockDisponible})` });
            return;
        }

        this.submittingIncidencia.set(true);

        let fechaRetornoStr: string | null = null;
        if (this.incidenciaTipo() === 'REPARACION' && this.incidenciaFechaRetorno()) {
            const date = this.incidenciaFechaRetorno()!;
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            fechaRetornoStr = `${year}-${month}-${day}`;
        }

        const request: IncidenciaRequest = {
            articuloId: art.id,
            reservaId: this.incidenciaReservaId(),
            descripcion: this.incidenciaDescripcion().trim(),
            tipo: this.incidenciaTipo(),
            cantidad: qty,
            fechaResolucionEstimada: fechaRetornoStr
        };

        this.incidenciaSvc.reportarIncidencia(request).subscribe({
            next: () => {
                this.msgSvc.add({ severity: 'success', summary: 'Reportada', detail: 'Incidencia reportada correctamente' });
                this.showIncidenciaDialog.set(false);
                this.loadArticulos();
            },
            error: (err) => {
                this.submittingIncidencia.set(false);
                this.msgSvc.add({ severity: 'error', summary: 'Error', detail: err?.error?.message || 'No se pudo reportar la incidencia' });
            }
        });
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

    getPrincipalImageUrl(art: ArticuloInventarioResponse): string | null {
        if (!art.imagenes || art.imagenes.length === 0) return null;
        const principal = art.imagenes.find(img => img.esPrincipal);
        const img = principal || art.imagenes[0];
        return `${API_URL}/${img.url}`;
    }

    getImageForArt(art: ArticuloInventarioResponse): string | null {
        if (!art.imagenes || art.imagenes.length === 0) return null;
        
        const hoverState = this.hoveredImageIndex();
        const hoveredIndex = hoverState[art.id];
        
        if (hoveredIndex !== undefined && hoveredIndex >= 0 && hoveredIndex < art.imagenes.length) {
            return `${API_URL}/${art.imagenes[hoveredIndex].url}`;
        }
        
        return this.getPrincipalImageUrl(art);
    }

    onMouseMove(event: MouseEvent, art: ArticuloInventarioResponse) {
        const imgs = art.imagenes;
        if (!imgs || imgs.length <= 1) return;
        
        const element = event.currentTarget as HTMLElement;
        const rect = element.getBoundingClientRect();
        const x = event.clientX - rect.left;
        const width = rect.width;
        
        const pct = Math.max(0, Math.min(1, x / width));
        const index = Math.floor(pct * imgs.length);
        
        this.hoveredImageIndex.update(state => ({
            ...state,
            [art.id]: Math.min(index, imgs.length - 1)
        }));
    }

    onMouseLeave(art: ArticuloInventarioResponse) {
        this.hoveredImageIndex.update(state => {
            const newState = { ...state };
            delete newState[art.id];
            return newState;
        });
    }

    openImageDialog(articulo: ArticuloInventarioResponse) {
        this.selectedArticulo.set(articulo);
        this.imagenes.set(articulo.imagenes ?? []);
        this.showImageDialog.set(true);
    }

    onFilesSelected(event: Event) {
        const input = event.target as HTMLInputElement;
        if (input.files && input.files.length > 0) {
            const files = Array.from(input.files);
            this.uploadImages(files);
        }
    }

    onDragOver(event: DragEvent) {
        event.preventDefault();
        this.isDragging.set(true);
    }

    onDragLeave(event: DragEvent) {
        event.preventDefault();
        this.isDragging.set(false);
    }

    onDrop(event: DragEvent) {
        event.preventDefault();
        this.isDragging.set(false);
        if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
            const files = Array.from(event.dataTransfer.files);
            this.uploadImages(files);
        }
    }

    tipoVistaOptions = [
        { label: 'Frontal (Por Defecto)', value: 'FRONTAL' },
        { label: 'Diagonal (Espejable)', value: 'DIAGONAL' },
        { label: 'Trasero', value: 'TRASERO' },
        { label: 'Lateral', value: 'LATERAL' }
    ];
    selectedUploadTipoVista = signal<string>('FRONTAL');

    uploadImages(files: File[]) {
        const art = this.selectedArticulo();
        if (!art) return;

        this.uploadingImages.set(true);
        this.svc.uploadImagenes(art.id, files, this.selectedUploadTipoVista()).subscribe({
            next: () => {
                this.uploadingImages.set(false);
                this.msgSvc.add({ severity: 'success', summary: 'Cargado', detail: 'Imágenes subidas correctamente' });
                this.reloadImages();
            },
            error: (err) => {
                this.uploadingImages.set(false);
                this.msgSvc.add({ severity: 'error', summary: 'Error', detail: err?.error ?? 'Error al subir imágenes' });
            }
        });
    }

    changeTipoVista(imagenId: number, tipoVista: string) {
        const art = this.selectedArticulo();
        if (!art) return;

        this.svc.setTipoVista(art.id, imagenId, tipoVista).subscribe({
            next: () => {
                this.msgSvc.add({ severity: 'success', summary: 'Vista Actualizada', detail: 'Tipo de vista actualizado correctamente' });
                this.reloadImages();
            },
            error: (err) => {
                this.msgSvc.add({ severity: 'error', summary: 'Error', detail: err?.error ?? 'Error al cambiar el tipo de vista' });
            }
        });
    }

    reloadImages() {
        const art = this.selectedArticulo();
        if (!art) return;
        this.svc.getById(art.id).subscribe({
            next: (updatedArt) => {
                this.imagenes.set(updatedArt.imagenes ?? []);
                this.articulos.update(list => list.map(a => a.id === updatedArt.id ? updatedArt : a));
                this.selectedArticulo.set(updatedArt);
            },
            error: () => console.error('Error al recargar imágenes')
        });
    }

    setAsPrincipal(imagenId: number) {
        const art = this.selectedArticulo();
        if (!art) return;

        this.svc.setPrincipal(art.id, imagenId).subscribe({
            next: () => {
                this.msgSvc.add({ severity: 'success', summary: 'Principal', detail: 'Imagen principal establecida' });
                this.reloadImages();
            },
            error: (err) => {
                this.msgSvc.add({ severity: 'error', summary: 'Error', detail: err?.error ?? 'Error al establecer imagen principal' });
            }
        });
    }

    deleteImagen(imagenId: number) {
        const art = this.selectedArticulo();
        if (!art) return;

        this.svc.deleteImagen(art.id, imagenId).subscribe({
            next: () => {
                this.msgSvc.add({ severity: 'success', summary: 'Eliminado', detail: 'Imagen eliminada correctamente' });
                this.reloadImages();
            },
            error: (err) => {
                this.msgSvc.add({ severity: 'error', summary: 'Error', detail: err?.error ?? 'Error al eliminar la imagen' });
            }
        });
    }

    getImageUrl(url: string): string {
        return `${API_URL}/${url}`;
    }
}
