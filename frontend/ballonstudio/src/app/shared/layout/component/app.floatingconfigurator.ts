import { Component, input } from '@angular/core';
import { AppConfigurator } from './app.configurator';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-floating-configurator',
    imports: [CommonModule, AppConfigurator],
    template: `
        <div class="flex gap-4 top-8 right-8" [ngClass]="{'fixed':float()}">
            <app-configurator />
        </div>
    `
})
export class AppFloatingConfigurator {
    float = input<boolean>(true);
}
