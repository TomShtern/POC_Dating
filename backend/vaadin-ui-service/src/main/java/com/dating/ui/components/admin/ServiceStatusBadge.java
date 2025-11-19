package com.dating.ui.components.admin;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Badge component for displaying service status
 */
public class ServiceStatusBadge extends Span {

    public ServiceStatusBadge(String status) {
        setText(status);
        addClassNames(
                LumoUtility.Padding.Horizontal.SMALL,
                LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.FontSize.SMALL,
                LumoUtility.FontWeight.MEDIUM
        );
        updateStyle(status);
    }

    public void setStatus(String status) {
        setText(status);
        updateStyle(status);
    }

    private void updateStyle(String status) {
        // Remove previous styles
        removeClassNames(
                "badge-success",
                "badge-error",
                "badge-warning"
        );
        getStyle().remove("background-color");
        getStyle().remove("color");

        if ("UP".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status)) {
            getStyle().set("background-color", "var(--lumo-success-color-10pct)");
            getStyle().set("color", "var(--lumo-success-text-color)");
        } else if ("DOWN".equalsIgnoreCase(status) || "DELETED".equalsIgnoreCase(status)) {
            getStyle().set("background-color", "var(--lumo-error-color-10pct)");
            getStyle().set("color", "var(--lumo-error-text-color)");
        } else if ("SUSPENDED".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status)) {
            getStyle().set("background-color", "var(--lumo-contrast-10pct)");
            getStyle().set("color", "var(--lumo-contrast-80pct)");
        } else {
            getStyle().set("background-color", "var(--lumo-contrast-10pct)");
            getStyle().set("color", "var(--lumo-contrast-80pct)");
        }
    }
}
