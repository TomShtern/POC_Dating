package com.dating.ui.views.admin;

import com.dating.ui.dto.admin.TimeSeriesDataPoint;
import com.dating.ui.service.admin.AdminAnalyticsService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Admin analytics view with charts and data export
 */
@Route(value = "admin/analytics", layout = AdminLayout.class)
@PageTitle("Analytics | Admin")
@RolesAllowed({"ADMIN", "ANALYST"})
public class AdminAnalyticsView extends VerticalLayout {

    private final AdminAnalyticsService analyticsService;
    private int selectedDays = 30;

    public AdminAnalyticsView(AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                createHeader(),
                createControlsBar(),
                createChartsSection(),
                createEngagementMetricsSection(),
                createGeographicSection()
        );
    }

    private H2 createHeader() {
        H2 header = new H2("Analytics Dashboard");
        header.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);
        return header;
    }

    private HorizontalLayout createControlsBar() {
        HorizontalLayout controls = new HorizontalLayout();
        controls.setAlignItems(Alignment.END);
        controls.addClassNames(LumoUtility.Gap.MEDIUM);

        ComboBox<Integer> daysCombo = new ComboBox<>("Time Period");
        daysCombo.setItems(7, 14, 30, 60, 90);
        daysCombo.setItemLabelGenerator(days -> days + " days");
        daysCombo.setValue(30);
        daysCombo.addValueChangeListener(e -> {
            selectedDays = e.getValue();
            refreshCharts();
        });

        // Export buttons
        Button exportUsers = createExportButton("users", "Export User Growth");
        Button exportDau = createExportButton("dau", "Export DAU");
        Button exportMatches = createExportButton("matches", "Export Matches");
        Button exportMessages = createExportButton("messages", "Export Messages");

        controls.add(daysCombo, exportUsers, exportDau, exportMatches, exportMessages);
        return controls;
    }

    private Button createExportButton(String reportType, String label) {
        Button button = new Button(label, VaadinIcon.DOWNLOAD.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.addClickListener(e -> exportToCsv(reportType));
        return button;
    }

    private void exportToCsv(String reportType) {
        String csv = analyticsService.exportAnalyticsToCsv(reportType, selectedDays);

        StreamResource resource = new StreamResource(
                reportType + "_analytics.csv",
                () -> new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        );

        Anchor downloadLink = new Anchor(resource, "");
        downloadLink.getElement().setAttribute("download", true);
        downloadLink.getElement().getStyle().set("display", "none");
        add(downloadLink);

        downloadLink.getElement().callJsFunction("click");

        Notification.show("Downloading " + reportType + " analytics...", 2000,
                Notification.Position.TOP_END);
    }

    private VerticalLayout createChartsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);

        H3 title = new H3("Growth Metrics");

        FlexLayout chartsGrid = new FlexLayout();
        chartsGrid.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        chartsGrid.addClassNames(LumoUtility.Gap.LARGE);

        // User Growth Chart (simplified - would use Vaadin Charts in production)
        chartsGrid.add(createSimpleChart("New Users", analyticsService.getUserGrowthData(selectedDays)));
        chartsGrid.add(createSimpleChart("Daily Active Users", analyticsService.getDailyActiveUsersData(selectedDays)));
        chartsGrid.add(createSimpleChart("Matches", analyticsService.getMatchesData(selectedDays)));
        chartsGrid.add(createSimpleChart("Messages", analyticsService.getMessagesData(selectedDays)));

        section.add(title, chartsGrid);
        return section;
    }

    private VerticalLayout createSimpleChart(String title, List<TimeSeriesDataPoint> data) {
        VerticalLayout chart = new VerticalLayout();
        chart.addClassNames(
                LumoUtility.Padding.MEDIUM,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Background.CONTRAST_5
        );
        chart.setWidth("400px");
        chart.setPadding(true);

        H3 chartTitle = new H3(title);
        chartTitle.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.NONE);

        // Summary stats
        long total = data.stream().mapToLong(TimeSeriesDataPoint::getValue).sum();
        double avg = data.stream().mapToLong(TimeSeriesDataPoint::getValue).average().orElse(0);
        long max = data.stream().mapToLong(TimeSeriesDataPoint::getValue).max().orElse(0);

        Span totalSpan = new Span("Total: " + String.format("%,d", total));
        Span avgSpan = new Span("Avg: " + String.format("%.0f", avg));
        Span maxSpan = new Span("Max: " + String.format("%,d", max));

        HorizontalLayout stats = new HorizontalLayout(totalSpan, avgSpan, maxSpan);
        stats.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        // Simple bar visualization (text-based for POC)
        VerticalLayout bars = new VerticalLayout();
        bars.setPadding(false);
        bars.setSpacing(false);

        // Show last 7 data points as simple bars
        int startIndex = Math.max(0, data.size() - 7);
        for (int i = startIndex; i < data.size(); i++) {
            TimeSeriesDataPoint point = data.get(i);
            HorizontalLayout bar = new HorizontalLayout();
            bar.setWidthFull();
            bar.setAlignItems(Alignment.CENTER);

            Span date = new Span(point.getDate().toString().substring(5)); // MM-DD
            date.setWidth("60px");
            date.addClassNames(LumoUtility.FontSize.XSMALL);

            // Create a simple visual bar
            int barWidth = (int) (point.getValue() * 100 / (max > 0 ? max : 1));
            Span barSpan = new Span();
            barSpan.getStyle().set("background-color", "var(--lumo-primary-color)");
            barSpan.getStyle().set("height", "16px");
            barSpan.getStyle().set("width", barWidth + "%");
            barSpan.getStyle().set("border-radius", "2px");

            Span value = new Span(String.format("%,d", point.getValue()));
            value.setWidth("60px");
            value.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextAlign.RIGHT);

            bar.add(date, barSpan, value);
            bar.expand(barSpan);
            bars.add(bar);
        }

        chart.add(chartTitle, stats, bars);
        return chart;
    }

    private VerticalLayout createEngagementMetricsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);

        H3 title = new H3("Engagement Metrics");

        AdminAnalyticsService.EngagementMetrics metrics = analyticsService.getEngagementMetrics();

        FlexLayout metricsGrid = new FlexLayout();
        metricsGrid.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        metricsGrid.addClassNames(LumoUtility.Gap.MEDIUM);

        metricsGrid.add(createMetricCard("Avg Swipes/User", String.format("%.1f", metrics.avgSwipesPerUser())));
        metricsGrid.add(createMetricCard("Avg Matches/User", String.format("%.1f", metrics.avgMatchesPerUser())));
        metricsGrid.add(createMetricCard("Avg Messages/Match", String.format("%.1f", metrics.avgMessagesPerMatch())));
        metricsGrid.add(createMetricCard("Avg Session", String.format("%.0f min", metrics.avgSessionDuration())));
        metricsGrid.add(createMetricCard("Sessions/Day", String.format("%.1f", metrics.avgSessionsPerDay())));

        section.add(title, metricsGrid);
        return section;
    }

    private VerticalLayout createMetricCard(String label, String value) {
        VerticalLayout card = new VerticalLayout();
        card.addClassNames(
                LumoUtility.Padding.MEDIUM,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Background.CONTRAST_5
        );
        card.setWidth("150px");
        card.setPadding(true);
        card.setSpacing(false);
        card.setAlignItems(Alignment.CENTER);

        Span valueSpan = new Span(value);
        valueSpan.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);

        Span labelSpan = new Span(label);
        labelSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        card.add(valueSpan, labelSpan);
        return card;
    }

    private VerticalLayout createGeographicSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);

        H3 title = new H3("Geographic Distribution");

        Grid<AdminAnalyticsService.GeoDistribution> grid = new Grid<>();
        grid.setItems(analyticsService.getGeographicDistribution());
        grid.setHeight("300px");

        grid.addColumn(AdminAnalyticsService.GeoDistribution::city)
                .setHeader("City")
                .setSortable(true);

        grid.addColumn(geo -> String.format("%,d", geo.users()))
                .setHeader("Users")
                .setSortable(true);

        grid.addColumn(geo -> String.format("%.1f%%", geo.percentage()))
                .setHeader("Percentage")
                .setSortable(true);

        section.add(title, grid);
        return section;
    }

    private void refreshCharts() {
        // Remove and recreate charts section
        removeAll();
        add(
                createHeader(),
                createControlsBar(),
                createChartsSection(),
                createEngagementMetricsSection(),
                createGeographicSection()
        );
    }
}
