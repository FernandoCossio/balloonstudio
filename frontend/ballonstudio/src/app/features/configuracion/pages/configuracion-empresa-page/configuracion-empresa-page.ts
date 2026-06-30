import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { ConfiguracionService } from '../../service/configuracion.service';
import { forkJoin, Observable } from 'rxjs';

declare var L: any;

@Component({
    selector: 'app-configuracion-empresa-page',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        CardModule,
        ButtonModule,
        InputTextModule,
        TextareaModule,
        ToastModule
    ],
    providers: [MessageService],
    templateUrl: './configuracion-empresa-page.html',
    styleUrl: './configuracion-empresa-page.scss'
})
export class ConfiguracionEmpresaPage implements OnInit, OnDestroy {
    private configService = inject(ConfiguracionService);
    private messageService = inject(MessageService);
    private cdr = inject(ChangeDetectorRef);

    formValues: Record<string, string> = {
        EMPRESA_NOMBRE: '',
        EMPRESA_NIT: '',
        EMPRESA_DIRECCION: '',
        EMPRESA_TELEFONO: '',
        EMPRESA_EMAIL: '',
        EMPRESA_LATITUD: '-17.7818',
        EMPRESA_LONGITUD: '-63.1804',
        RECIBO_PI_PAGINA: ''
    };

    saving = false;
    private map: any;
    private marker: any;

    ngOnInit() {
        this.loadConfiguraciones();
    }

    ngOnDestroy() {
        if (this.map) {
            this.map.remove();
        }
    }

    private loadConfiguraciones() {
        this.configService.getConfiguraciones().subscribe({
            next: (configs) => {
                configs.forEach(c => {
                    if (c.clave in this.formValues) {
                        this.formValues[c.clave] = c.valor;
                    }
                });
                this.cdr.markForCheck();
                this.initMap();
            },
            error: (err) => {
                this.messageService.add({
                    severity: 'error',
                    summary: 'Error',
                    detail: 'No se pudieron cargar las configuraciones del sistema.'
                });
                this.initMap();
            }
        });
    }

    private initMap() {
        // Cargar Leaflet dinámicamente si no está presente
        if (typeof L === 'undefined') {
            const link = document.createElement('link');
            link.rel = 'stylesheet';
            link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
            document.head.appendChild(link);

            const script = document.createElement('script');
            script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
            script.onload = () => this.buildLeafletMap();
            document.body.appendChild(script);
        } else {
            this.buildLeafletMap();
        }
    }

    private buildLeafletMap() {
        const lat = parseFloat(this.formValues['EMPRESA_LATITUD'] || '-17.7818');
        const lng = parseFloat(this.formValues['EMPRESA_LONGITUD'] || '-63.1804');

        if (this.map) {
            this.map.setView([lat, lng], 14);
            if (this.marker) {
                this.marker.setLatLng([lat, lng]);
            }
            return;
        }

        setTimeout(() => {
            const mapContainer = document.getElementById('map');
            if (!mapContainer) return;
            
            this.map = L.map('map').setView([lat, lng], 14);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© OpenStreetMap contributors'
            }).addTo(this.map);

            // Icono rosa personalizado para hacer juego con la estética Premium de Balloon Studio
            const pinkIcon = L.icon({
                iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-violet.png',
                shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
                iconSize: [25, 41],
                iconAnchor: [12, 41],
                popupAnchor: [1, -34],
                shadowSize: [41, 41]
            });

            this.marker = L.marker([lat, lng], { draggable: true, icon: pinkIcon }).addTo(this.map);

            // Escuchar el arrastre del marcador
            this.marker.on('dragend', () => {
                const position = this.marker.getLatLng();
                this.updateCoords(position.lat, position.lng);
            });

            // Escuchar el clic en el mapa para mover el marcador
            this.map.on('click', (e: any) => {
                this.marker.setLatLng(e.latlng);
                this.updateCoords(e.latlng.lat, e.latlng.lng);
            });
        }, 200);
    }

    private updateCoords(lat: number, lng: number) {
        this.formValues['EMPRESA_LATITUD'] = lat.toFixed(6);
        this.formValues['EMPRESA_LONGITUD'] = lng.toFixed(6);
        
        // Geocodificación inversa simple usando OpenStreetMap Nominatim
        fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`)
            .then(res => res.json())
            .then(data => {
                if (data && data.display_name) {
                    this.formValues['EMPRESA_DIRECCION'] = data.display_name;
                    this.cdr.markForCheck();
                }
            })
            .catch(() => {});
        this.cdr.markForCheck();
    }

    saveAll() {
        this.saving = true;
        const requests: Observable<any>[] = [];

        Object.keys(this.formValues).forEach(clave => {
            const valor = this.formValues[clave];
            requests.push(this.configService.updateConfiguracion(clave, valor));
        });

        forkJoin(requests).subscribe({
            next: () => {
                this.saving = false;
                this.messageService.add({
                    severity: 'success',
                    summary: 'Configuración Guardada',
                    detail: 'Todos los datos de la empresa se actualizaron correctamente.'
                });
            },
            error: (err) => {
                this.saving = false;
                this.messageService.add({
                    severity: 'error',
                    summary: 'Error al Guardar',
                    detail: 'Ocurrió un error inesperado al intentar guardar los cambios.'
                });
            }
        });
    }
}
