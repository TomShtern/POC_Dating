package com.dating.ui.views.admin;

import com.dating.ui.security.SecurityUtils;
import com.dating.ui.service.UserService;
import com.dating.ui.views.LoginView;
import com.dating.ui.views.SwipeView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Main layout for admin dashboard views
 * Provides navigation and header for all admin pages
 */
@PageTitle("Admin | POC Dating")
public class AdminLayout extends AppLayout {

    private final UserService userService;

    public AdminLayout(UserService userService) {
        this.userService = userService;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Admin Dashboard");
        logo.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.MEDIUM
        );

        String userName = SecurityUtils.getCurrentUserName();
        Span userInfo = new Span("Admin: " + (userName != null ? userName : "Unknown"));
        userInfo.addClassNames(LumoUtility.Margin.Right.MEDIUM);

        Button backToApp = new Button("Back to App", VaadinIcon.ARROW_LEFT.create(),
                e -> UI.getCurrent().navigate(SwipeView.class));
        backToApp.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create(),
                e -> handleLogout());
        logoutButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout header = new HorizontalLayout(
                new DrawerToggle(),
                logo,
                userInfo,
                backToApp,
                logoutButton
        );

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames(
                LumoUtility.Padding.Vertical.NONE,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Background.CONTRAST_5
        );

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();

        // Main dashboard
        nav.addItem(new SideNavItem("Dashboard", AdminDashboardView.class,
                VaadinIcon.DASHBOARD.create()));

        // User management
        nav.addItem(new SideNavItem("Users", AdminUserManagementView.class,
                VaadinIcon.USERS.create()));

        // Analytics
        nav.addItem(new SideNavItem("Analytics", AdminAnalyticsView.class,
                VaadinIcon.CHART.create()));

        // System monitoring
        nav.addItem(new SideNavItem("System", AdminSystemView.class,
                VaadinIcon.COG.create()));

        // Configuration
        nav.addItem(new SideNavItem("Configuration", AdminConfigurationView.class,
                VaadinIcon.SLIDERS.create()));

        // Audit log
        nav.addItem(new SideNavItem("Audit Log", AdminAuditLogView.class,
                VaadinIcon.FILE_TEXT.create()));

        addToDrawer(nav);
    }

    private void handleLogout() {
        userService.logout();
        UI.getCurrent().navigate(LoginView.class);
        UI.getCurrent().getPage().reload();
    }
}
