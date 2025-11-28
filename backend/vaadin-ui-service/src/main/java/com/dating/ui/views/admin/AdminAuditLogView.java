package com.dating.ui.views.admin;

import com.dating.ui.dto.admin.AdminAuditLogDTO;
import com.dating.ui.service.admin.AdminAuditService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * Audit log view showing all admin actions
 */
@Route(value = "admin/audit", layout = AdminLayout.class)
@PageTitle("Audit Log | Admin")
@RolesAllowed({"ADMIN"})
public class AdminAuditLogView extends VerticalLayout {

    private final AdminAuditService auditService;
    private final Grid<AdminAuditLogDTO> auditGrid;
    private String filterAction = "";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AdminAuditLogView(AdminAuditService auditService) {
        this.auditService = auditService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        auditGrid = createAuditGrid();

        add(
                createHeader(),
                createFiltersBar(),
                auditGrid
        );
    }

    private H2 createHeader() {
        H2 header = new H2("Audit Log");
        header.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);
        return header;
    }

    private HorizontalLayout createFiltersBar() {
        HorizontalLayout filters = new HorizontalLayout();
        filters.setAlignItems(Alignment.END);
        filters.addClassNames(LumoUtility.Gap.MEDIUM);

        TextField actionFilter = new TextField("Filter by Action");
        actionFilter.setPlaceholder("e.g., USER_STATUS_CHANGE");
        actionFilter.setClearButtonVisible(true);
        actionFilter.setWidth("250px");

        Button applyButton = new Button("Apply", e -> {
            filterAction = actionFilter.getValue();
            auditGrid.getDataProvider().refreshAll();
        });
        applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button clearButton = new Button("Clear", e -> {
            actionFilter.clear();
            filterAction = "";
            auditGrid.getDataProvider().refreshAll();
        });
        clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button refreshButton = new Button("Refresh", VaadinIcon.REFRESH.create(),
                e -> auditGrid.getDataProvider().refreshAll());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Span totalCount = new Span("Total: " + auditService.getTotalCount() + " entries");
        totalCount.addClassNames(LumoUtility.TextColor.SECONDARY);

        filters.add(actionFilter, applyButton, clearButton, refreshButton, totalCount);
        return filters;
    }

    private Grid<AdminAuditLogDTO> createAuditGrid() {
        Grid<AdminAuditLogDTO> grid = new Grid<>();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();

        grid.addColumn(log -> log.getCreatedAt().format(DATE_FORMATTER))
                .setHeader("Timestamp")
                .setSortable(true)
                .setWidth("180px")
                .setFlexGrow(0);

        grid.addColumn(AdminAuditLogDTO::getAdminName)
                .setHeader("Admin")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(AdminAuditLogDTO::getAction)
                .setHeader("Action")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(AdminAuditLogDTO::getTargetType)
                .setHeader("Target Type")
                .setAutoWidth(true);

        grid.addColumn(log -> log.getTargetId() != null ?
                        log.getTargetId().substring(0, Math.min(8, log.getTargetId().length())) + "..." : "-")
                .setHeader("Target ID")
                .setAutoWidth(true);

        grid.addColumn(AdminAuditLogDTO::getIpAddress)
                .setHeader("IP Address")
                .setAutoWidth(true);

        // Details button
        grid.addComponentColumn(log -> {
            Button detailsButton = new Button(VaadinIcon.INFO_CIRCLE.create(),
                    e -> showDetailsDialog(log));
            detailsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            detailsButton.getElement().setAttribute("title", "View Details");
            return detailsButton;
        }).setHeader("Details").setWidth("80px").setFlexGrow(0);

        // Configure lazy loading
        grid.setDataProvider(DataProvider.fromCallbacks(
                query -> fetchLogs(query.getOffset(), query.getLimit()),
                query -> {
                    if (filterAction.isEmpty()) {
                        return auditService.getTotalCount();
                    }
                    return auditService.getAuditLogsByAction(filterAction, Integer.MAX_VALUE).size();
                }
        ));

        return grid;
    }

    private Stream<AdminAuditLogDTO> fetchLogs(int offset, int limit) {
        if (filterAction.isEmpty()) {
            return auditService.getAuditLogs(offset, limit).stream();
        }
        return auditService.getAuditLogsByAction(filterAction, limit).stream().skip(offset);
    }

    private void showDetailsDialog(AdminAuditLogDTO log) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Audit Log Details");
        dialog.setWidth("600px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        addDetailRow(content, "ID", log.getId());
        addDetailRow(content, "Timestamp", log.getCreatedAt().format(DATE_FORMATTER));
        addDetailRow(content, "Admin", log.getAdminName() + " (" + log.getAdminId() + ")");
        addDetailRow(content, "Action", log.getAction());
        addDetailRow(content, "Target Type", log.getTargetType());
        addDetailRow(content, "Target ID", log.getTargetId());
        addDetailRow(content, "IP Address", log.getIpAddress());

        if (log.getUserAgent() != null) {
            Span userAgentLabel = new Span("User Agent:");
            userAgentLabel.addClassNames(LumoUtility.FontWeight.BOLD);
            Span userAgentValue = new Span(log.getUserAgent());
            userAgentValue.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
            content.add(userAgentLabel, userAgentValue);
        }

        if (log.getDetails() != null && !log.getDetails().isEmpty()) {
            Span detailsLabel = new Span("Details:");
            detailsLabel.addClassNames(LumoUtility.FontWeight.BOLD);

            Pre detailsJson = new Pre(formatJson(log.getDetails().toString()));
            detailsJson.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
            detailsJson.getStyle().set("padding", "var(--lumo-space-s)");
            detailsJson.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
            detailsJson.getStyle().set("overflow-x", "auto");
            detailsJson.getStyle().set("font-size", "var(--lumo-font-size-s)");

            content.add(detailsLabel, detailsJson);
        }

        dialog.add(content);

        Button closeButton = new Button("Close", e -> dialog.close());
        dialog.getFooter().add(closeButton);

        dialog.open();
    }

    private void addDetailRow(VerticalLayout container, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.addClassNames(LumoUtility.Gap.MEDIUM);

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassNames(LumoUtility.FontWeight.BOLD);
        labelSpan.setWidth("100px");

        Span valueSpan = new Span(value != null ? value : "-");

        row.add(labelSpan, valueSpan);
        container.add(row);
    }

    private String formatJson(String json) {
        // Simple formatting for display
        return json.replace("{", "{\n  ")
                .replace("}", "\n}")
                .replace(", ", ",\n  ");
    }
}
