import { Component, inject } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { LayoutService } from '../service/layout.service';

@Component({
    selector: 'app-configurator',
    standalone: true,
    imports: [ButtonModule],
    template: `
        <button
            type="button"
            class="layout-topbar-action"
            (click)="toggleDarkMode()"
            [title]="layoutService.isDarkTheme() ? 'Modo Claro' : 'Modo Oscuro'"
        >
            <i [class]="layoutService.isDarkTheme() ? 'pi pi-sun' : 'pi pi-moon'"></i>
        </button>
    `
})
export class AppConfigurator {
    layoutService = inject(LayoutService);

    toggleDarkMode() {
        this.layoutService.toggleDarkTheme();
    }
}
