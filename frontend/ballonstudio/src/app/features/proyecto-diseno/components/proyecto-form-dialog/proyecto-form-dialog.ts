import { Component, inject, signal, OnInit, AfterViewInit, NgZone, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';
import { ProyectoDisenoService } from '../../services/proyecto-diseno.service';
import { ProyectoDisenoRequest, ProyectoDisenoResponse } from '../../interfaces/proyecto-diseno.interface';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { ConfiguracionService } from '@/app/features/configuracion/service/configuracion.service';
import * as L from 'leaflet';

@Component({
  selector: 'app-proyecto-form-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, InputTextModule, TextareaModule, ButtonModule, DatePickerModule],
  templateUrl: './proyecto-form-dialog.html',
  styleUrl: './proyecto-form-dialog.scss'
})
export class ProyectoFormDialogComponent implements OnInit, AfterViewInit {

  private ref             = inject(DynamicDialogRef);
  private config          = inject(DynamicDialogConfig);
  private proyectoService = inject(ProyectoDisenoService);
  private configService   = inject(ConfiguracionService);
  private http            = inject(HttpClient);
  private ngZone          = inject(NgZone);
  private cdr             = inject(ChangeDetectorRef);

  readonly cargando     = signal(false);
  readonly nombre       = signal('');
  readonly descripcion  = signal('');
  readonly lugarEvento  = signal('');
  readonly fechaEvento  = signal<Date | null>(null);
  readonly latitud      = signal<number | null>(null);
  readonly longitud     = signal<number | null>(null);
  readonly distanciaKm  = signal<number | null>(null);

  readonly minFecha = (() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return today;
  })();

  private map?: L.Map;
  private marker?: L.Marker;
  private defaultLat = -17.7818;
  private defaultLng = -63.1804;

  protected get modoEdicion(): boolean {
    return !!this.config.data?.proyecto;
  }

  protected get proyectoExistente(): ProyectoDisenoResponse | null {
    return this.config.data?.proyecto ?? null;
  }

  ngOnInit(): void {
    const p = this.proyectoExistente;
    if (!p) return;
    this.nombre.set(p.nombre);
    this.descripcion.set(p.descripcion ?? '');
    this.lugarEvento.set(p.lugarEvento ?? '');
    this.fechaEvento.set(p.fechaEvento ? new Date(p.fechaEvento) : null);
    this.latitud.set(p.latitud);
    this.longitud.set(p.longitud);
    this.distanciaKm.set(p.distanciaKm);
  }

  ngAfterViewInit(): void {
    this.cargarUbicacionEmpresaYInicializarMapa();
  }

  private cargarUbicacionEmpresaYInicializarMapa() {
    this.configService.getConfiguraciones().subscribe({
      next: (configs) => {
        const latConfig = configs.find(c => c.clave === 'EMPRESA_LATITUD')?.valor;
        const lngConfig = configs.find(c => c.clave === 'EMPRESA_LONGITUD')?.valor;
        if (latConfig && lngConfig) {
          this.defaultLat = parseFloat(latConfig);
          this.defaultLng = parseFloat(lngConfig);
        }
        this.inicializarMapa();
      },
      error: () => {
        this.inicializarMapa();
      }
    });
  }

  private inicializarMapa() {
    const p = this.proyectoExistente;
    const initialLat = p?.latitud ?? this.defaultLat;
    const initialLng = p?.longitud ?? this.defaultLng;

    const iconSVG = `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="36" height="36"><path fill="%23d946ef" d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>`;
    const customMarkerIcon = L.icon({
      iconUrl: iconSVG,
      iconSize: [36, 36],
      iconAnchor: [18, 36]
    });

    this.map = L.map('map-dialog').setView([initialLat, initialLng], 14);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    if (p?.latitud && p?.longitud) {
      this.marker = L.marker([p.latitud, p.longitud], { icon: customMarkerIcon }).addTo(this.map);
    }

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.ngZone.run(() => {
        const { lat, lng } = e.latlng;
        this.latitud.set(lat);
        this.longitud.set(lng);
        this.cdr.detectChanges();

        if (this.marker) {
          this.marker.setLatLng(e.latlng);
        } else {
          this.marker = L.marker(e.latlng, { icon: customMarkerIcon }).addTo(this.map!);
        }

        this.geocodificarCoordenadas(lat, lng);
      });
    });

    setTimeout(() => {
      this.map?.invalidateSize();
    }, 400);
  }

  private geocodificarCoordenadas(lat: number, lng: number) {
    const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`;
    fetch(url)
      .then(res => res.json())
      .then(data => {
        if (data && data.display_name) {
          const parts = data.display_name.split(',');
          const formattedAddress = parts.slice(0, 3).map((p: string) => p.trim()).join(', ');
          this.lugarEvento.set(formattedAddress);
          this.cdr.detectChanges();
        }
      })
      .catch((err) => console.warn('Error en reverse geocoding', err));
  }

  guardar(): void {
    if (!this.nombre().trim()) return;
    this.cargando.set(true);

    const request: ProyectoDisenoRequest = {
      nombre:      this.nombre().trim(),
      descripcion: this.descripcion().trim() || undefined,
      lugarEvento: this.lugarEvento().trim() || undefined,
      fechaEvento: this.fechaEvento()?.toISOString().split('T')[0] ?? undefined,
      latitud:     this.latitud() ?? undefined,
      longitud:    this.longitud() ?? undefined,
      distanciaKm: this.distanciaKm() ?? undefined,
      estado:      'borrador'
    };

    const op$ = this.modoEdicion
      ? this.proyectoService.update(this.proyectoExistente!.id, request)
      : this.proyectoService.create(request);

    op$.subscribe({
      next:  (res) => { this.cargando.set(false); this.ref.close(res); },
      error: ()    => this.cargando.set(false)
    });
  }

  cancelar(): void {
    this.ref.close();
  }
}