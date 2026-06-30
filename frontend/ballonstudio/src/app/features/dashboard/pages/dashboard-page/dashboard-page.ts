import { Component, OnInit, inject, signal, afterNextRender } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartModule } from 'primeng/chart';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { RouterModule } from '@angular/router';
import { DashboardService } from '../../services/dashboard.service';
import { DashboardMetrics } from '../../interfaces/dashboard.interface';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, ChartModule, TableModule, ButtonModule, RouterModule],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss'
})
export class DashboardPage implements OnInit {
  private dashboardService = inject(DashboardService);

  metrics = signal<DashboardMetrics | null>(null);
  loading = signal<boolean>(true);

  // Chart configuration
  revenueData = signal<any>(null);
  revenueOptions = signal<any>(null);
  
  statusData = signal<any>(null);
  statusOptions = signal<any>(null);

  constructor() {
    afterNextRender(() => {
      // Set up charts once browser elements are ready
      this.initChartOptions();
    });
  }

  ngOnInit(): void {
    this.loadMetrics();
  }

  loadMetrics(): void {
    this.loading.set(true);
    this.dashboardService.getMetrics().subscribe({
      next: (data) => {
        this.metrics.set(data);
        this.updateCharts(data);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error fetching dashboard metrics', err);
        this.loading.set(false);
      }
    });
  }

  private initChartOptions(): void {
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--text-color') || '#495057';
    const textColorSecondary = documentStyle.getPropertyValue('--text-color-secondary') || '#6c757d';
    const surfaceBorder = documentStyle.getPropertyValue('--surface-border') || '#dee2e6';

    this.revenueOptions.set({
      plugins: {
        legend: {
          labels: {
            color: textColor
          }
        }
      },
      scales: {
        x: {
          ticks: {
            color: textColorSecondary,
            font: {
              weight: 500
            }
          },
          grid: {
            color: surfaceBorder,
            drawBorder: false
          }
        },
        y: {
          ticks: {
            color: textColorSecondary
          },
          grid: {
            color: surfaceBorder,
            drawBorder: false
          }
        }
      }
    });

    this.statusOptions.set({
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            color: textColor
          }
        }
      },
      cutout: '60%'
    });
  }

  private updateCharts(data: DashboardMetrics): void {
    const documentStyle = getComputedStyle(document.documentElement);
    
    // 1. Revenue Chart (Bar & Line Combo)
    const months = data.monthlyRevenue.map(mr => {
      const parts = mr.month.split('-');
      if (parts.length === 2) {
        const date = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, 1);
        return date.toLocaleDateString('es-ES', { month: 'short', year: '2-digit' });
      }
      return mr.month;
    });
    
    const revenues = data.monthlyRevenue.map(mr => mr.revenue);

    this.revenueData.set({
      labels: months,
      datasets: [
        {
          label: 'Ingresos Mensuales (Bs.)',
          data: revenues,
          backgroundColor: documentStyle.getPropertyValue('--p-primary-500') || '#C2185B',
          borderColor: documentStyle.getPropertyValue('--p-primary-500') || '#C2185B',
          borderWidth: 2,
          borderRadius: 6,
          tension: 0.4,
          type: 'bar'
        },
        {
          label: 'Tendencia',
          data: revenues,
          borderColor: '#42A5F5',
          borderWidth: 3,
          fill: false,
          tension: 0.4,
          type: 'line'
        }
      ]
    });

    // 2. Reservation Status Doughnut
    const statuses = data.reservationStatus.map(rs => rs.status);
    const counts = data.reservationStatus.map(rs => rs.count);

    // Map some nice colors for status types
    const statusColorsMap: { [key: string]: string } = {
      'PENDIENTE_PAGO': '#FFA726',
      'CONFIRMADA': '#66BB6A',
      'EXPIRADA': '#EF5350',
      'CANCELADA': '#78909C'
    };

    const bgColors = statuses.map(s => statusColorsMap[s] || '#AB47BC');

    this.statusData.set({
      labels: statuses,
      datasets: [
        {
          data: counts,
          backgroundColor: bgColors,
          hoverBackgroundColor: bgColors
        }
      ]
    });
  }
}
