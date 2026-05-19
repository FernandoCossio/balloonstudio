import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, RouterModule, Router } from '@angular/router';
import { MenuItem } from 'primeng/api';
import { AppMenuitem } from './app.menuitem';
import { filter } from 'rxjs/operators';
import { AuthService } from '@/app/features/auth/service/auth.service';
import { ROLES } from '@/app/features/core/constants/role.constant';

@Component({
    selector: 'app-menu',
    standalone: true,
    imports: [CommonModule, AppMenuitem, RouterModule],
    template: `<ul class="layout-menu">
        @for (item of model; track item.label) {
            @if (!item.separator) {
                <li app-menuitem [item]="item" [root]="true"></li>
            } @else {
                <li class="menu-separator"></li>
            }
        }
    </ul> `,
})
export class AppMenu {
    model: MenuItem[] = [];

    private auth = inject(AuthService);
    private router = inject(Router);

    ngOnInit() {
        this.updateMenu();

        this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => {
            this.updateMenu();
        });
    }

    private updateMenu() {
        if (this.auth.hasRole(ROLES.ADMINISTRADOR)) {
            this.model = this.buildAdminMenu();
            return;
        }

        if (this.auth.hasRole(ROLES.EMPLEADO)) {
            this.model = this.buildEmpleadoMenu();
            return;
        }

        if (this.auth.hasRole(ROLES.CLIENTE)) {
            this.model = this.buildClienteMenu();
            return;
        }

        this.model = this.buildGuestMenu();
    }

    private buildGuestMenu(): MenuItem[] {
        return [
            {
                label: 'Home',
                items: [{ label: 'Dashboard', icon: 'pi pi-fw pi-home', routerLink: ['/'] }]
            }
        ];
    }

    private buildAdminMenu(): MenuItem[] {
        return [
            {
                label: 'Administrador',
                items: [{ label: 'Dashboard', icon: 'pi pi-fw pi-home', routerLink: ['/'] }]
            },
            {
                label: 'Inventario',
                icon: 'pi pi-fw pi-box',
                items: [
                    {
                        label: 'Artículos',
                        icon: 'pi pi-fw pi-box',
                        routerLink: ['/inventario']
                    }
                ]
            }
        ];
    }

    private buildEmpleadoMenu(): MenuItem[] {
        return [
            {
                label: 'Empleado',
                items: [{ label: 'Dashboard', icon: 'pi pi-fw pi-home', routerLink: ['/'] }]
            }
        ];
    }

    private buildClienteMenu(): MenuItem[] {
        return [
            {
                label: 'Cliente',
                items: [{ label: 'Dashboard', icon: 'pi pi-fw pi-home', routerLink: ['/'] }]
            }
        ];
    }
}
