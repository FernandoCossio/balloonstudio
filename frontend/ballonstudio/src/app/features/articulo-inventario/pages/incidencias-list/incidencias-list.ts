import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { DialogModule } from 'primeng/dialog';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { IncidenciaService, IncidenciaArticulo } from '../../service/incidencia.service';
import { API_URL } from '@/enviroment/enviroment';

type EstadoFilter = 'TODOS' | 'ACTIVA' | 'SOLUCIONADA';
type TipoFilter = 'TODOS' | 'REPARACION' | 'MERMA_PERDIDA';

@Component({
  selector: 'app-incidencias-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule,
    ButtonModule, InputTextModule, InputNumberModule,
    DialogModule, TagModule, ToastModule
  ],
  providers: [MessageService],
  templateUrl: './incidencias-list.html',
  styleUrl: './incidencias-list.scss'
})
export class IncidenciasList implements OnInit {
  private incidenciaSvc = inject(IncidenciaService);
  private msgSvc = inject(MessageService);
  private router = inject(Router);

  // ─── State ───────────────────────────────────────────────────────────────
  incidencias = signal<IncidenciaArticulo[]>([]);
  loading = signal(true);
  searchQuery = signal('');
  filterEstado = signal<EstadoFilter>('TODOS');
  filterTipo = signal<TipoFilter>('TODOS');

  // Solution Modal State
  showSolucionarDialog = signal(false);
  selectedIncidencia = signal<IncidenciaArticulo | null>(null);
  costoReparacion = signal<number | null>(null);
  submittingSolucion = signal(false);

  // ─── Computed ────────────────────────────────────────────────────────────
  incidenciasFiltradas = computed(() => {
    let list = this.incidencias();
    const q = this.searchQuery().toLowerCase();
    const estado = this.filterEstado();
    const tipo = this.filterTipo();

    if (q) {
      list = list.filter(i => i.articuloInventario.nombre.toLowerCase().includes(q));
    }
    if (estado !== 'TODOS') {
      list = list.filter(i => i.estado === estado);
    }
    if (tipo !== 'TODOS') {
      list = list.filter(i => i.tipo === tipo);
    }
    return list;
  });

  totalActivas = computed(() => this.incidencias().filter(i => i.estado === 'ACTIVA').length);
  totalMermas = computed(() => this.incidencias().filter(i => i.tipo === 'MERMA_PERDIDA').length);
  totalReparaciones = computed(() => this.incidencias().filter(i => i.tipo === 'REPARACION' && i.estado === 'ACTIVA').length);

  // ─── Lifecycle ───────────────────────────────────────────────────────────
  ngOnInit() {
    this.loadIncidencias();
  }

  loadIncidencias() {
    this.loading.set(true);
    this.incidenciaSvc.listarIncidencias().subscribe({
      next: (data) => {
        this.incidencias.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.msgSvc.add({ severity: 'error', summary: 'Error', detail: 'No se pudo cargar la lista de incidencias' });
        this.loading.set(false);
      }
    });
  }

  // ─── Actions ─────────────────────────────────────────────────────────────
  setFilterEstado(est: EstadoFilter) {
    this.filterEstado.set(est);
  }

  setFilterTipo(tp: TipoFilter) {
    this.filterTipo.set(tp);
  }

  openSolucionarDialog(inc: IncidenciaArticulo) {
    this.selectedIncidencia.set(inc);
    this.costoReparacion.set(null);
    this.submittingSolucion.set(false);
    this.showSolucionarDialog.set(true);
  }

  solucionarIncidencia() {
    const inc = this.selectedIncidencia();
    if (!inc) return;

    this.submittingSolucion.set(true);
    const request = {
      costoReparacion: this.costoReparacion() ?? undefined
    };

    this.incidenciaSvc.solucionarIncidencia(inc.id, request).subscribe({
      next: () => {
        this.msgSvc.add({ severity: 'success', summary: 'Solucionada', detail: 'La incidencia ha sido marcada como solucionada.' });
        this.showSolucionarDialog.set(false);
        this.loadIncidencias();
      },
      error: (err) => {
        this.submittingSolucion.set(false);
        this.msgSvc.add({ severity: 'error', summary: 'Error', detail: err?.error?.message || 'No se pudo solucionar la incidencia' });
      }
    });
  }

  volverAlInventario() {
    this.router.navigate(['/inventario']);
  }

  // ─── UI Helpers ──────────────────────────────────────────────────────────
  getPrincipalImageUrl(inc: IncidenciaArticulo): string | null {
    const art = inc.articuloInventario;
    if (!art.imagenes || art.imagenes.length === 0) return null;
    const principal = art.imagenes.find((img: any) => img.esPrincipal);
    const img = principal || art.imagenes[0];
    return `${API_URL}/${img.url}`;
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

  tipoLabel(tipo: string): string {
    return tipo === 'REPARACION' ? 'Reparación' : 'Merma / Pérdida';
  }

  tipoSeverity(tipo: string): 'warn' | 'danger' {
    return tipo === 'REPARACION' ? 'warn' : 'danger';
  }

  estadoLabel(estado: string): string {
    return estado === 'ACTIVA' ? 'Activa' : 'Solucionada';
  }

  estadoSeverity(estado: string): 'danger' | 'success' {
    return estado === 'ACTIVA' ? 'danger' : 'success';
  }
}
