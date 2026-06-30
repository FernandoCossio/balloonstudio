export interface KpiMetrics {
  totalRevenue: number;
  totalReservations: number;
  activeClients: number;
  designProjects: number;
  totalArticles: number;
  lowStockAlerts: number;
}

export interface MonthlyRevenue {
  month: string;
  revenue: number;
}

export interface ReservationStatusCount {
  status: string;
  count: number;
}

export interface PopularArticle {
  id: number;
  nombre: string;
  usageCount: number;
}

export interface LowStockArticle {
  id: number;
  nombre: string;
  stockTotal: number;
}

export interface RecentReservation {
  id: number;
  cliente: string;
  fechaReserva: string;
  total: number;
  estado: string;
}

export interface DashboardMetrics {
  kpis: KpiMetrics;
  monthlyRevenue: MonthlyRevenue[];
  reservationStatus: ReservationStatusCount[];
  popularArticles: PopularArticle[];
  lowStockArticles: LowStockArticle[];
  recentReservations: RecentReservation[];
}
