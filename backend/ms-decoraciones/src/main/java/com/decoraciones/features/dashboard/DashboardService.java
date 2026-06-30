package com.decoraciones.features.dashboard;

import com.decoraciones.features.reserva.ReservaRepository;
import com.decoraciones.features.pago.PagoRepository;
import com.decoraciones.features.usuario.UsuarioRepository;
import com.decoraciones.features.proyectodiseno.ProyectoDisenoRepository;
import com.decoraciones.features.inventario.ArticuloInventarioRepository;
import com.decoraciones.domain.models.Reserva;
import com.decoraciones.domain.models.Pago;
import com.decoraciones.domain.models.ArticuloInventario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProyectoDisenoRepository proyectoDisenoRepository;
    private final ArticuloInventarioRepository articuloInventarioRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public record KpiMetrics(
        BigDecimal totalRevenue,
        long totalReservations,
        long activeClients,
        long designProjects,
        long totalArticles,
        long lowStockAlerts
    ) {}

    public record MonthlyRevenue(
        String month,
        BigDecimal revenue
    ) {}

    public record ReservationStatusCount(
        String status,
        long count
    ) {}

    public record PopularArticle(
        Long id,
        String nombre,
        long usageCount
    ) {}

    public record LowStockArticle(
        Long id,
        String nombre,
        int stockTotal
    ) {}

    public record RecentReservation(
        Long id,
        String cliente,
        LocalDateTime fechaReserva,
        BigDecimal total,
        String estado
    ) {}

    public record DashboardMetrics(
        KpiMetrics kpis,
        List<MonthlyRevenue> monthlyRevenue,
        List<ReservationStatusCount> reservationStatus,
        List<PopularArticle> popularArticles,
        List<LowStockArticle> lowStockArticles,
        List<RecentReservation> recentReservations
    ) {}

    public DashboardMetrics getDashboardMetrics() {
        // 1. KPI - Total Revenue
        BigDecimal totalRevenue = entityManager.createQuery(
                "SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.estado = 'COMPLETADO'", BigDecimal.class)
                .getSingleResult();

        // KPI - Total Reservations
        long totalReservations = reservaRepository.count();

        // KPI - Active Clients (Active users count)
        long activeClients = entityManager.createQuery(
                "SELECT COUNT(u) FROM Usuario u WHERE u.activo = true", Long.class)
                .getSingleResult();

        // KPI - Design Projects
        long designProjects = proyectoDisenoRepository.count();

        // KPI - Total Articles
        long totalArticles = articuloInventarioRepository.count();

        // KPI - Low Stock Alerts (Stock < 10)
        long lowStockAlerts = entityManager.createQuery(
                "SELECT COUNT(a) FROM ArticuloInventario a WHERE a.stockTotal < 10", Long.class)
                .getSingleResult();

        KpiMetrics kpis = new KpiMetrics(
            totalRevenue,
            totalReservations,
            activeClients,
            designProjects,
            totalArticles,
            lowStockAlerts
        );

        // 2. Monthly Revenue for the last 6 months
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        List<Pago> recentPayments = entityManager.createQuery(
                "SELECT p FROM Pago p WHERE p.estado = 'COMPLETADO' AND p.fechaPago >= :startDate ORDER BY p.fechaPago ASC", Pago.class)
                .setParameter("startDate", sixMonthsAgo)
                .getResultList();

        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        // Initialize last 6 months with 0
        for (int i = 5; i >= 0; i--) {
            String monthKey = LocalDateTime.now().minusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            revenueByMonth.put(monthKey, BigDecimal.ZERO);
        }

        for (Pago pago : recentPayments) {
            String monthKey = pago.getFechaPago().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            if (revenueByMonth.containsKey(monthKey)) {
                revenueByMonth.put(monthKey, revenueByMonth.get(monthKey).add(pago.getMonto()));
            }
        }

        List<MonthlyRevenue> monthlyRevenueList = revenueByMonth.entrySet().stream()
                .map(entry -> new MonthlyRevenue(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        // 3. Reservation status count
        List<Object[]> statusCounts = entityManager.createQuery(
                "SELECT r.estado, COUNT(r) FROM Reserva r GROUP BY r.estado", Object[].class)
                .getResultList();

        List<ReservationStatusCount> reservationStatusList = statusCounts.stream()
                .map(row -> new ReservationStatusCount((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        // 4. Top 5 popular articles (based on ElementoLienzo usage)
        List<Object[]> popularQuery = entityManager.createQuery(
                "SELECT e.articuloInventario, SUM(e.cantidad) FROM ElementoLienzo e GROUP BY e.articuloInventario ORDER BY SUM(e.cantidad) DESC", Object[].class)
                .setMaxResults(5)
                .getResultList();

        List<PopularArticle> popularArticles = popularQuery.stream()
                .map(row -> {
                    ArticuloInventario art = (ArticuloInventario) row[0];
                    Long usage = (Long) row[1];
                    return new PopularArticle(art.getId(), art.getNombre(), usage);
                })
                .collect(Collectors.toList());

        // 5. Top 5 low stock articles
        List<ArticuloInventario> lowStockQuery = entityManager.createQuery(
                "SELECT a FROM ArticuloInventario a WHERE a.stockTotal < 10 ORDER BY a.stockTotal ASC", ArticuloInventario.class)
                .setMaxResults(5)
                .getResultList();

        List<LowStockArticle> lowStockArticles = lowStockQuery.stream()
                .map(art -> new LowStockArticle(art.getId(), art.getNombre(), art.getStockTotal()))
                .collect(Collectors.toList());

        // 6. Recent 5 reservations
        List<Reserva> recentReservationsQuery = entityManager.createQuery(
                "SELECT r FROM Reserva r JOIN FETCH r.usuario ORDER BY r.createdAt DESC", Reserva.class)
                .setMaxResults(5)
                .getResultList();

        List<RecentReservation> recentReservations = recentReservationsQuery.stream()
                .map(res -> new RecentReservation(
                    res.getId(),
                    res.getUsuario().getNombreCompleto(),
                    res.getFechaReserva(),
                    res.getCotizacion() != null ? res.getCotizacion().getTotal() : BigDecimal.ZERO,
                    res.getEstado()
                ))
                .collect(Collectors.toList());

        return new DashboardMetrics(
            kpis,
            monthlyRevenueList,
            reservationStatusList,
            popularArticles,
            lowStockArticles,
            recentReservations
        );
    }
}
