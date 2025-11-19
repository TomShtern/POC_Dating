package com.dating.ui.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;

/**
 * VerificationBadge - Shows if a user is verified
 * Displays a blue checkmark icon for verified users
 */
public class VerificationBadge extends Composite<FlexLayout> {

    private final Icon checkIcon;
    private final Span label;

    public VerificationBadge() {
        FlexLayout layout = getContent();
        layout.setAlignItems(FlexLayout.Alignment.CENTER);
        layout.getStyle().set("gap", "4px");

        checkIcon = new Icon(VaadinIcon.CHECK_CIRCLE);
        checkIcon.setSize("16px");
        checkIcon.getStyle().set("color", "#3b82f6");
        checkIcon.addClassName("verified-badge");

        label = new Span("Verified");
        label.getStyle()
            .set("font-size", "0.8rem")
            .set("color", "#3b82f6")
            .set("font-weight", "500");

        layout.add(checkIcon, label);
        layout.setVisible(false); // Hidden by default
    }

    /**
     * Set verification status
     */
    public void setVerified(Boolean isVerified) {
        getContent().setVisible(Boolean.TRUE.equals(isVerified));
    }

    /**
     * Show only icon without label
     */
    public void setCompact(boolean compact) {
        label.setVisible(!compact);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        // Simple component - no listeners to clean up
    }
}
