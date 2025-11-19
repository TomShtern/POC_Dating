package com.dating.ui.views.admin;

import com.dating.ui.components.admin.ServiceStatusBadge;
import com.dating.ui.dto.admin.ServiceHealthDTO;
import com.dating.ui.service.admin.AdminSystemService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * System monitoring view showing service health and metrics
 */
@Route(value = "admin/system", layout = AdminLayout.class)
@PageTitle("System Monitoring | Admin")
@RolesAllowed({"ADMIN"})
public class AdminSystemView extends VerticalLayout {

    private final AdminSystemService systemService;
    private Grid<ServiceHealthDTO> healthGrid;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public AdminSystemView(AdminSystemService systemService) {
        this.systemService = systemService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                createHeader(),
                createServiceHealthSection(),
                createMetricsSection(),
                createErrorLogSection()
        );
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("System Monitoring");

        Button refreshButton = new Button("Refresh", VaadinIcon.REFRESH.create(), e -> refreshAll());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout header = new HorizontalLayout(title, refreshButton);
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        header.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);

        return header;
    }

    private VerticalLayout createServiceHealthSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);

        H3 title = new H3("Service Health");

        healthGrid = new Grid<>();
        healthGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        healthGrid.setHeight("300px");

        healthGrid.addColumn(ServiceHealthDTO::getServiceName)
                .setHeader("Service")
                .setAutoWidth(true);

        healthGrid.addComponentColumn(health -> new ServiceStatusBadge(health.getStatus()))
                .setHeader("Status")
                .setAutoWidth(true);

        healthGrid.addColumn(ServiceHealthDTO::getUrl)
                .setHeader("URL")
                .setAutoWidth(true);

        healthGrid.addColumn(health -> health.getResponseTimeMs() + " ms")
                .setHeader("Response Time")
                .setAutoWidth(true);

        healthGrid.addColumn(health -> health.getLastChecked() != null ?
                        health.getLastChecked().format(TIME_FORMATTER) : "-")
                .setHeader("Last Checked")
                .setAutoWidth(true);

        healthGrid.addColumn(ServiceHealthDTO::getVersion)
                .setHeader("Version")
                .setAutoWidth(true);

        healthGrid.setItems(systemService.getAllServiceHealth());

        section.add(title, healthGrid);
        return section;
    }

    private FlexLayout createMetricsSection() {
        FlexLayout section = new FlexLayout();
        section.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        section.addClassNames(LumoUtility.Gap.LARGE, LumoUtility.Margin.Vertical.MEDIUM);

        // Database metrics
        section.add(createDatabaseMetricsCard());

        // Cache metrics
        section.add(createCacheMetricsCard());

        // Queue metrics
        section.add(createQueueMetricsCard());

        return section;
    }

    private VerticalLayout createDatabaseMetricsCard() {
        VerticalLayout card = createMetricsCard("Database (PostgreSQL)");

        AdminSystemService.DatabaseMetrics metrics = systemService.getDatabaseMetrics();

        addMetricRow(card, "Active Connections",
                metrics.activeConnections() + " / " + metrics.maxConnections());
        addMetricRow(card, "Avg Query Time", String.format("%.1f ms", metrics.avgQueryTime()));
        addMetricRow(card, "Total Queries", String.format("%,d", metrics.totalQueries()));
        addMetricRow(card, "Database Size", metrics.databaseSize());
        addMetricRow(card, "Index Size", metrics.indexSize());

        return card;
    }

    private VerticalLayout createCacheMetricsCard() {
        VerticalLayout card = createMetricsCard("Cache (Redis)");

        AdminSystemService.CacheMetrics metrics = systemService.getCacheMetrics();

        addMetricRow(card, "Hit Rate", String.format("%.1f%%", metrics.hitRate()));
        addMetricRow(card, "Total Hits", String.format("%,d", metrics.totalHits()));
        addMetricRow(card, "Total Misses", String.format("%,d", metrics.totalMisses()));
        addMetricRow(card, "Keys in Cache", String.format("%,d", metrics.keysInCache()));
        addMetricRow(card, "Memory", metrics.memoryUsed() + " / " + metrics.maxMemory());
        addMetricRow(card, "Clients", String.valueOf(metrics.connectedClients()));

        return card;
    }

    private VerticalLayout createQueueMetricsCard() {
        VerticalLayout card = createMetricsCard("Message Queue (RabbitMQ)");

        AdminSystemService.QueueMetrics metrics = systemService.getQueueMetrics();

        addMetricRow(card, "Total Queues", String.valueOf(metrics.totalQueues()));
        addMetricRow(card, "Messages in Queue", String.valueOf(metrics.messagesInQueue()));
        addMetricRow(card, "Published (1h)", String.format("%,d", metrics.messagesPublished()));
        addMetricRow(card, "Consumed (1h)", String.format("%,d", metrics.messagesConsumed()));
        addMetricRow(card, "Active Consumers", String.valueOf(metrics.activeConsumers()));
        addMetricRow(card, "Avg Delivery", String.format("%.1f ms", metrics.avgDeliveryTime()));

        return card;
    }

    private VerticalLayout createMetricsCard(String title) {
        VerticalLayout card = new VerticalLayout();
        card.addClassNames(
                LumoUtility.Padding.MEDIUM,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Background.CONTRAST_5
        );
        card.setWidth("300px");
        card.setPadding(true);
        card.setSpacing(false);

        H3 cardTitle = new H3(title);
        cardTitle.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.Bottom.SMALL);
        card.add(cardTitle);

        return card;
    }

    private void addMetricRow(VerticalLayout card, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(JustifyContentMode.BETWEEN);
        row.addClassNames(LumoUtility.Padding.Vertical.XSMALL);

        Span labelSpan = new Span(label);
        labelSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        Span valueSpan = new Span(value);
        valueSpan.addClassNames(LumoUtility.FontWeight.MEDIUM, LumoUtility.FontSize.SMALL);

        row.add(labelSpan, valueSpan);
        card.add(row);
    }

    private VerticalLayout createErrorLogSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);

        H3 title = new H3("Recent Errors");

        Grid<AdminSystemService.ErrorLogEntry> errorGrid = new Grid<>();
        errorGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        errorGrid.setHeight("200px");

        errorGrid.addColumn(error -> error.timestamp().format(TIME_FORMATTER))
                .setHeader("Time")
                .setWidth("100px")
                .setFlexGrow(0);

        errorGrid.addComponentColumn(error -> {
            Span badge = new Span(error.level());
            badge.addClassNames(
                    LumoUtility.Padding.Horizontal.SMALL,
                    LumoUtility.BorderRadius.MEDIUM,
                    LumoUtility.FontSize.XSMALL
            );
            if ("ERROR".equals(error.level())) {
                badge.getStyle().set("background-color", "var(--lumo-error-color-10pct)");
                badge.getStyle().set("color", "var(--lumo-error-text-color)");
            } else {
                badge.getStyle().set("background-color", "var(--lumo-contrast-10pct)");
            }
            return badge;
        }).setHeader("Level").setWidth("80px").setFlexGrow(0);

        errorGrid.addColumn(AdminSystemService.ErrorLogEntry::service)
                .setHeader("Service")
                .setWidth("120px")
                .setFlexGrow(0);

        errorGrid.addColumn(AdminSystemService.ErrorLogEntry::message)
                .setHeader("Message")
                .setAutoWidth(true);

        List<AdminSystemService.ErrorLogEntry> errors = systemService.getRecentErrors(10);
        errorGrid.setItems(errors);

        section.add(title, errorGrid);
        return section;
    }

    private void refreshAll() {
        healthGrid.setItems(systemService.getAllServiceHealth());

        // Rebuild metrics section
        removeAll();
        add(
                createHeader(),
                createServiceHealthSection(),
                createMetricsSection(),
                createErrorLogSection()
        );
    }
}
