import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TabsModule } from 'primeng/tabs';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { CardModule } from 'primeng/card';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { ParametroNegocioService } from '../../service/parametro-negocio.service';
import { ParametroNegocio } from '../../interface/parametro-negocio.interface';

@Component({
    selector: 'app-parametros-contabilidad-page',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        TabsModule,
        CardModule,
        InputNumberModule,
        ToggleSwitchModule,
        ButtonModule,
        ToastModule
    ],
    providers: [MessageService],
    templateUrl: './parametros-contabilidad-page.html',
    styleUrl: './parametros-contabilidad-page.scss'
})
export class ParametrosContabilidadPage implements OnInit {
    private service = inject(ParametroNegocioService);
    private messageService = inject(MessageService);

    parametros = signal<ParametroNegocio>({
        calcularFactorEstacional: true,
        provisionSiniestroReutilizables: true,
        costoOverheadFijo: 0,
        capacidadVolumetricaVehiculo: 0,
        tarifaBaseViaje: 0,
        tarifaKmLogistica: 0,
        tarifaHoraComplejidadBaja: 0,
        tarifaHoraComplejidadMedia: 0,
        tarifaHoraComplejidadAlta: 0,
        porcentajeSiniestralidad: 0,
        fallbackPorcentajeGanancia: 0,
        fallbackVidaUtilUsos: 0,
        fallbackVidaUtilAnos: 0,
        fallbackValorResidualPorcentaje: 0,
        fallbackMantenimientoPorcentaje: 0,
        fallbackDiasPreparacion: 0,
        fallbackDiasLimpieza: 0
    });

    cargando = signal<boolean>(false);
    guardando = signal<boolean>(false);

    ngOnInit() {
        this.cargarParametros();
    }

    cargarParametros() {
        this.cargando.set(true);
        this.service.getParametros().subscribe({
            next: (data) => {
                this.parametros.set(data);
                this.cargando.set(false);
            },
            error: (err) => {
                this.messageService.add({
                    severity: 'error',
                    summary: 'Error',
                    detail: 'No se pudieron cargar los parámetros contables del negocio.'
                });
                this.cargando.set(false);
            }
        });
    }

    guardarParametros() {
        this.guardando.set(true);
        this.service.updateParametros(this.parametros()).subscribe({
            next: (data) => {
                this.parametros.set(data);
                this.guardando.set(false);
                this.messageService.add({
                    severity: 'success',
                    summary: 'Guardado',
                    detail: 'Parámetros actualizados y caché invalidada correctamente.'
                });
            },
            error: (err) => {
                this.messageService.add({
                    severity: 'error',
                    summary: 'Error al Guardar',
                    detail: 'Ocurrió un error inesperado al intentar guardar los cambios.'
                });
                this.guardando.set(false);
            }
        });
    }
}
