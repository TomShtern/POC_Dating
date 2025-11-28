package com.dating.ui.views;

import com.dating.ui.security.SecurityUtils;
import com.dating.ui.service.UserService;
import com.dating.ui.views.admin.AdminDashboardView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Main layout for the application
 * Provides navigation drawer and header
 */
@PageTitle("POC Dating")
public class MainLayout extends AppLayout {

    private final UserService userService;

    public MainLayout(UserService userService) {
        this.userService = userService;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("❤️ POC Dating");
        logo.addClassNames(
            LumoUtility.FontSize.LARGE,
            LumoUtility.Margin.MEDIUM
        );

        String userName = SecurityUtils.getCurrentUserName();
        Span userInfo = new Span("👤 " + (userName != null ? userName : "User"));

        Button logoutButton = new Button("Logout", e -> handleLogout());

        HorizontalLayout header = new HorizontalLayout(
            new DrawerToggle(),
            logo,
            userInfo,
            logoutButton
        );

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames(
            LumoUtility.Padding.Vertical.NONE,
            LumoUtility.Padding.Horizontal.MEDIUM
        );

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Discover", SwipeView.class, VaadinIcon.HEART.create()));
        nav.addItem(new SideNavItem("Matches", MatchesView.class, VaadinIcon.USERS.create()));
        nav.addItem(new SideNavItem("Messages", MessagesView.class, VaadinIcon.CHAT.create()));
        nav.addItem(new SideNavItem("Notifications", NotificationsView.class, VaadinIcon.BELL.create()));
        nav.addItem(new SideNavItem("Profile", ProfileView.class, VaadinIcon.USER.create()));
        nav.addItem(new SideNavItem("Preferences", PreferencesView.class, VaadinIcon.SLIDER.create()));
        nav.addItem(new SideNavItem("Settings", SettingsView.class, VaadinIcon.COG.create()));
        nav.addItem(new SideNavItem("Blocked Users", BlockedUsersView.class, VaadinIcon.BAN.create()));
        nav.addItem(new SideNavItem("About", AboutView.class, VaadinIcon.INFO_CIRCLE.create()));

        // Add admin link if user has admin access
        if (SecurityUtils.hasAdminAccess()) {
            SideNavItem adminItem = new SideNavItem("Admin Dashboard", AdminDashboardView.class,
                    VaadinIcon.DASHBOARD.create());
            nav.addItem(adminItem);
        }

        addToDrawer(nav);
    }

    private void handleLogout() {
        try {
            userService.logout();
            UI.getCurrent().navigate(LoginView.class);
            UI.getCurrent().getPage().reload();
        } catch (Exception ex) {
            Notification.show("Logout failed. Please try again.",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
