package com.dating.ui.views.admin;

import com.dating.ui.components.admin.ServiceStatusBadge;
import com.dating.ui.components.admin.StatCard;
import com.dating.ui.dto.admin.DashboardStatsDTO;
import com.dating.ui.dto.admin.ServiceHealthDTO;
import com.dating.ui.service.admin.AdminAnalyticsService;
import com.dating.ui.service.admin.AdminAuditService;
import com.dating.ui.service.admin.AdminSystemService;
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

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Main admin dashboard view with statistics and system health
 */
@Route(value = "admin", layout = AdminLayout.class)
@PageTitle("Dashboard | Admin")
@RolesAllowed({"ADMIN", "MODERATOR", "ANALYST"})
public class AdminDashboardView extends VerticalLayout {

    private final AdminAnalyticsService analyticsService;
    private final AdminSystemService systemService;
    private final AdminAuditService auditService;
    private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

    public AdminDashboardView(AdminAnalyticsService analyticsService,
                              AdminSystemService systemService,
                              AdminAuditService auditService) {
        this.analyticsService = analyticsService;
        this.systemService = systemService;
        this.auditService = auditService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                createHeader(),
                createStatsCards(),
                createSystemHealthSection(),
                createRecentActivitySection()
        );
    }

    private H2 createHeader() {
        H2 header = new H2("Dashboard Overview");
        header.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);
        return header;
    }

    private FlexLayout createStatsCards() {
        DashboardStatsDTO stats = analyticsService.getDashboardStats();

        FlexLayout cardsLayout = new FlexLayout();
        cardsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        cardsLayout.addClassNames(LumoUtility.Gap.MEDIUM, LumoUtility.Margin.Bottom.LARGE);

        cardsLayout.add(
                new StatCard("Total Users", numberFormat.format(stats.getTotalUsers()),
                        "+5.2% this week", VaadinIcon.USERS),
                new StatCard("Active Today", numberFormat.format(stats.getActiveToday()),
                        "+12% vs yesterday", VaadinIcon.USER_CHECK),
                new StatCard("New This Week", numberFormat.format(stats.getNewThisWeek()),
                        "+8.3% vs last week", VaadinIcon.PLUS_CIRCLE),
                new StatCard("Matches Today", numberFormat.format(stats.getMatchesToday()),
                        "+15% vs yesterday", VaadinIcon.HEART),
                new StatCard("Messages Today", numberFormat.format(stats.getMessagesToday()),
                        "+7% vs yesterday", VaadinIcon.CHAT),
                new StatCard("Pending Reports", String.valueOf(stats.getPendingReports()),
                        "needs review", VaadinIcon.WARNING)
        );

        return cardsLayout;
    }

    private VerticalLayout createSystemHealthSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);

        H3 title = new H3("System Health");
        title.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        List<ServiceHealthDTO> services = systemService.getAllServiceHealth();

        FlexLayout servicesGrid = new FlexLayout();
        servicesGrid.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        servicesGrid.addClassNames(LumoUtility.Gap.MEDIUM);

        for (ServiceHealthDTO service : services) {
            servicesGrid.add(createServiceCard(service));
        }

        section.add(title, servicesGrid);
        return section;
    }

    private VerticalLayout createServiceCard(ServiceHealthDTO service) {
        VerticalLayout card = new VerticalLayout();
        card.addClassNames(
                LumoUtility.Padding.MEDIUM,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Background.CONTRAST_5
        );
        card.setWidth("180px");
        card.setPadding(true);
        card.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);

        Span name = new Span(service.getServiceName());
        name.addClassNames(LumoUtility.FontWeight.MEDIUM, LumoUtility.FontSize.SMALL);

        ServiceStatusBadge badge = new ServiceStatusBadge(service.getStatus());

        header.add(name, badge);

        Span responseTime = new Span(service.getResponseTimeMs() + "ms");
        responseTime.addClassNames(
                LumoUtility.FontSize.SMALL,
                LumoUtility.TextColor.SECONDARY
        );

        card.add(header, responseTime);
        return card;
    }

    private VerticalLayout createRecentActivitySection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);

        H3 title = new H3("Recent Admin Activity");
        title.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        VerticalLayout activityList = new VerticalLayout();
        activityList.setPadding(false);
        activityList.setSpacing(false);

        auditService.getRecentLogs(5).forEach(log -> {
            HorizontalLayout item = new HorizontalLayout();
            item.setWidthFull();
            item.addClassNames(
                    LumoUtility.Padding.Vertical.SMALL,
                    LumoUtility.Border.BOTTOM
            );

            Span action = new Span(log.getAction());
            action.addClassNames(LumoUtility.FontWeight.MEDIUM);

            Span admin = new Span("by " + log.getAdminName());
            admin.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

            Span time = new Span(log.getCreatedAt().toString());
            time.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

            VerticalLayout details = new VerticalLayout(action, admin);
            details.setPadding(false);
            details.setSpacing(false);

            item.add(details, time);
            item.expand(details);

            activityList.add(item);
        });

        if (auditService.getRecentLogs(1).isEmpty()) {
            Span noActivity = new Span("No recent activity");
            noActivity.addClassNames(LumoUtility.TextColor.SECONDARY);
            activityList.add(noActivity);
        }

        section.add(title, activityList);
        return section;
    }
}
