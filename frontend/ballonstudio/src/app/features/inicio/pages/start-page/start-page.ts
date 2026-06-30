import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../auth/service/auth.service';

@Component({
    selector: 'app-start-page',
    standalone: true,
    imports: [
        CommonModule,
        ButtonModule,
        CardModule,
        RouterModule
    ],
    templateUrl: './start-page.html',
    styleUrl: './start-page.scss'
})
export class StartPage {
    private authService = inject(AuthService);
    private router = inject(Router);

    startDesigning() {
        if (this.authService.isLoggedIn()) {
            this.router.navigate(['/proyectos']);
        } else {
            this.router.navigate(['/auth/login']);
        }
    }
}
