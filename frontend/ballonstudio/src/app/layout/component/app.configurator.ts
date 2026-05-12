import { Component, inject } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { LayoutService } from '@/app/layout/service/layout.service';

@Component({
    selector: 'app-configurator',
    standalone: true,
    imports: [ButtonModule],
    template: `
        <p-button
            type="button"
            (onClick)="toggleDarkMode()"
            [rounded]="true"
            [text]="true"
            [icon]="layoutService.isDarkTheme() ? 'pi pi-moon' : 'pi pi-sun'"
            styleClass="layout-topbar-action"
        />
    `
})
export class AppConfigurator {
    layoutService = inject(LayoutService);

    toggleDarkMode() {
        this.layoutService.toggleDarkTheme();
    }
}
