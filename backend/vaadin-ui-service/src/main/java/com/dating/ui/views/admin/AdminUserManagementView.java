package com.dating.ui.views.admin;

import com.dating.ui.components.admin.ServiceStatusBadge;
import com.dating.ui.dto.admin.AdminUserDTO;
import com.dating.ui.dto.admin.AdminUserSearchCriteria;
import com.dating.ui.service.admin.AdminAuditService;
import com.dating.ui.service.admin.AdminUserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Admin view for user management
 */
@Route(value = "admin/users", layout = AdminLayout.class)
@PageTitle("User Management | Admin")
@RolesAllowed({"ADMIN", "MODERATOR"})
public class AdminUserManagementView extends VerticalLayout {

    private final AdminUserService userService;
    private final AdminAuditService auditService;
    private final Grid<AdminUserDTO> userGrid;
    private final TextField searchField;
    private final ComboBox<String> statusFilter;
    private AdminUserSearchCriteria currentCriteria;
    private final Set<AdminUserDTO> selectedUsers = new HashSet<>();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    public AdminUserManagementView(AdminUserService userService, AdminAuditService auditService) {
        this.userService = userService;
        this.auditService = auditService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Initialize components
        searchField = createSearchField();
        statusFilter = createStatusFilter();
        userGrid = createUserGrid();
        currentCriteria = new AdminUserSearchCriteria();

        // Layout
        add(
                createHeader(),
                createFiltersBar(),
                createBulkActionsBar(),
                userGrid
        );

        // Load initial data
        refreshGrid();
    }

    private H2 createHeader() {
        H2 header = new H2("User Management");
        header.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);
        return header;
    }

    private HorizontalLayout createFiltersBar() {
        HorizontalLayout filters = new HorizontalLayout();
        filters.setWidthFull();
        filters.setAlignItems(FlexComponent.Alignment.END);
        filters.addClassNames(LumoUtility.Gap.MEDIUM);

        Button searchButton = new Button("Search", VaadinIcon.SEARCH.create(), e -> applyFilters());
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button clearButton = new Button("Clear", e -> clearFilters());
        clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        filters.add(searchField, statusFilter, searchButton, clearButton);
        filters.expand(searchField);

        return filters;
    }

    private TextField createSearchField() {
        TextField field = new TextField("Search");
        field.setPlaceholder("Email, username, or name...");
        field.setPrefixComponent(VaadinIcon.SEARCH.create());
        field.setWidth("300px");
        field.setClearButtonVisible(true);
        return field;
    }

    private ComboBox<String> createStatusFilter() {
        ComboBox<String> combo = new ComboBox<>("Status");
        combo.setItems("", "ACTIVE", "SUSPENDED", "DELETED");
        combo.setItemLabelGenerator(item -> item.isEmpty() ? "All" : item);
        combo.setValue("");
        combo.setWidth("150px");
        return combo;
    }

    private HorizontalLayout createBulkActionsBar() {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setAlignItems(FlexComponent.Alignment.CENTER);
        actions.addClassNames(LumoUtility.Gap.SMALL);

        Span selectedCount = new Span("0 selected");
        selectedCount.setId("selected-count");

        Button suspendButton = new Button("Suspend Selected", e -> bulkAction("SUSPENDED"));
        suspendButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

        Button activateButton = new Button("Activate Selected", e -> bulkAction("ACTIVE"));
        activateButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);

        actions.add(selectedCount, suspendButton, activateButton);
        return actions;
    }

    private Grid<AdminUserDTO> createUserGrid() {
        Grid<AdminUserDTO> grid = new Grid<>();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.setHeight("100%");

        // Selection listener
        grid.addSelectionListener(event -> {
            selectedUsers.clear();
            selectedUsers.addAll(event.getAllSelectedItems());
            updateSelectedCount();
        });

        // Columns
        grid.addColumn(AdminUserDTO::getEmail)
                .setHeader("Email")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(AdminUserDTO::getUsername)
                .setHeader("Username")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(user -> user.getFirstName() + " " + user.getLastName())
                .setHeader("Name")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addComponentColumn(user -> new ServiceStatusBadge(user.getStatus()))
                .setHeader("Status")
                .setAutoWidth(true);

        grid.addColumn(user -> user.getCreatedAt() != null ?
                        user.getCreatedAt().format(DATE_FORMATTER) : "-")
                .setHeader("Registered")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(user -> user.getLastLogin() != null ?
                        user.getLastLogin().format(DATE_FORMATTER) : "Never")
                .setHeader("Last Login")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(AdminUserDTO::getMatchCount)
                .setHeader("Matches")
                .setSortable(true)
                .setAutoWidth(true);

        // Actions column
        grid.addComponentColumn(this::createActionsColumn)
                .setHeader("Actions")
                .setAutoWidth(true)
                .setFlexGrow(0);

        // Configure lazy loading
        grid.setDataProvider(DataProvider.fromCallbacks(
                query -> fetchUsers(query),
                query -> (int) userService.countUsers(currentCriteria)
        ));

        return grid;
    }

    private Stream<AdminUserDTO> fetchUsers(Query<AdminUserDTO, Void> query) {
        return userService.searchUsers(
                currentCriteria,
                query.getOffset(),
                query.getLimit()
        ).stream();
    }

    private HorizontalLayout createActionsColumn(AdminUserDTO user) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(false);
        actions.addClassNames(LumoUtility.Gap.XSMALL);

        Button viewButton = new Button(VaadinIcon.EYE.create(), e -> showUserDetails(user));
        viewButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        viewButton.getElement().setAttribute("title", "View Details");

        Button editButton = new Button(VaadinIcon.EDIT.create(), e -> showEditDialog(user));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        editButton.getElement().setAttribute("title", "Edit Status");

        actions.add(viewButton, editButton);
        return actions;
    }

    private void applyFilters() {
        currentCriteria = AdminUserSearchCriteria.builder()
                .searchText(searchField.getValue())
                .status(statusFilter.getValue().isEmpty() ? null : statusFilter.getValue())
                .build();
        refreshGrid();
    }

    private void clearFilters() {
        searchField.clear();
        statusFilter.setValue("");
        currentCriteria = new AdminUserSearchCriteria();
        refreshGrid();
    }

    private void refreshGrid() {
        userGrid.getDataProvider().refreshAll();
    }

    private void updateSelectedCount() {
        getElement().executeJs(
                "document.getElementById('selected-count').textContent = $0 + ' selected'",
                selectedUsers.size()
        );
    }

    private void showUserDetails(AdminUserDTO user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("User Details: " + user.getUsername());
        dialog.setWidth("600px");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        form.addFormItem(new Span(user.getId()), "ID");
        form.addFormItem(new Span(user.getEmail()), "Email");
        form.addFormItem(new Span(user.getUsername()), "Username");
        form.addFormItem(new Span(user.getFirstName() + " " + user.getLastName()), "Name");
        form.addFormItem(new Span(String.valueOf(user.getAge())), "Age");
        form.addFormItem(new Span(user.getGender()), "Gender");
        form.addFormItem(new ServiceStatusBadge(user.getStatus()), "Status");
        form.addFormItem(new Span(user.isVerified() ? "Yes" : "No"), "Verified");
        form.addFormItem(new Span(String.valueOf(user.getMatchCount())), "Matches");
        form.addFormItem(new Span(String.valueOf(user.getMessageCount())), "Messages");
        form.addFormItem(new Span(String.valueOf(user.getReportCount())), "Reports");

        if (user.getCreatedAt() != null) {
            form.addFormItem(new Span(user.getCreatedAt().format(DATE_FORMATTER)), "Registered");
        }
        if (user.getLastLogin() != null) {
            form.addFormItem(new Span(user.getLastLogin().format(DATE_FORMATTER)), "Last Login");
        }

        dialog.add(form);

        Button closeButton = new Button("Close", e -> dialog.close());
        dialog.getFooter().add(closeButton);

        dialog.open();
    }

    private void showEditDialog(AdminUserDTO user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Update User Status");
        dialog.setWidth("400px");

        ComboBox<String> statusCombo = new ComboBox<>("New Status");
        statusCombo.setItems("ACTIVE", "SUSPENDED", "DELETED");
        statusCombo.setValue(user.getStatus());
        statusCombo.setWidthFull();

        TextArea reasonField = new TextArea("Reason");
        reasonField.setPlaceholder("Enter reason for status change...");
        reasonField.setWidthFull();
        reasonField.setRequired(true);

        VerticalLayout content = new VerticalLayout(statusCombo, reasonField);
        content.setPadding(false);
        dialog.add(content);

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button saveButton = new Button("Save", e -> {
            if (reasonField.isEmpty()) {
                showError("Please provide a reason");
                return;
            }

            try {
                userService.updateUserStatus(user.getId(), statusCombo.getValue(), reasonField.getValue());
                showSuccess("User status updated successfully");
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                showError("Failed to update user: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void bulkAction(String newStatus) {
        if (selectedUsers.isEmpty()) {
            showError("No users selected");
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirm Bulk Action");
        dialog.setText("Are you sure you want to set " + selectedUsers.size() +
                " user(s) to " + newStatus + "?");

        dialog.setCancelable(true);
        dialog.setConfirmText("Confirm");
        dialog.setConfirmButtonTheme("primary");

        dialog.addConfirmListener(e -> {
            int updated = userService.bulkUpdateStatus(
                    selectedUsers.stream().map(AdminUserDTO::getId).toList(),
                    newStatus,
                    "Bulk action by admin"
            );
            showSuccess(updated + " users updated successfully");
            userGrid.deselectAll();
            refreshGrid();
        });

        dialog.open();
    }

    private void showSuccess(String message) {
        Notification.show(message, 3000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        Notification.show(message, 5000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
