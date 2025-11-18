package com.dating.ui.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;

import java.time.Duration;
import java.time.Instant;

/**
 * OnlineStatusBadge - Shows user's online/offline status
 * Displays a green dot for online users or last active time for offline users
 */
public class OnlineStatusBadge extends Composite<FlexLayout> {

    private final Div statusDot;
    private final Span statusText;

    public OnlineStatusBadge() {
        FlexLayout layout = getContent();
        layout.addClassName("online-status");
        layout.setAlignItems(FlexLayout.Alignment.CENTER);
        layout.getStyle().set("gap", "4px");

        statusDot = new Div();
        statusDot.addClassName("online-dot");

        statusText = new Span();
        statusText.getStyle()
            .set("font-size", "0.8rem")
            .set("color", "#666");

        layout.add(statusDot, statusText);
    }

    /**
     * Set online status
     */
    public void setOnline(boolean isOnline) {
        if (isOnline) {
            statusDot.removeClassName("offline");
            statusDot.addClassName("online");
            statusText.setText("Online");
            statusText.getStyle().set("color", "#10b981");
        } else {
            statusDot.removeClassName("online");
            statusDot.addClassName("offline");
            statusText.setText("Offline");
            statusText.getStyle().set("color", "#666");
        }
    }

    /**
     * Set last active time for offline users
     */
    public void setLastActive(Instant lastActiveAt) {
        if (lastActiveAt == null) {
            setOnline(false);
            return;
        }

        Duration duration = Duration.between(lastActiveAt, Instant.now());
        String timeAgo = formatDuration(duration);

        statusDot.removeClassName("online");
        statusDot.addClassName("offline");
        statusText.setText("Active " + timeAgo);
        statusText.getStyle().set("color", "#666");
    }

    /**
     * Set both online status and last active time
     */
    public void setStatus(Boolean isOnline, Instant lastActiveAt) {
        if (Boolean.TRUE.equals(isOnline)) {
            setOnline(true);
        } else if (lastActiveAt != null) {
            setLastActive(lastActiveAt);
        } else {
            setOnline(false);
        }
    }

    private String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        if (minutes < 1) {
            return "just now";
        } else if (minutes < 60) {
            return minutes + "m ago";
        } else if (minutes < 1440) {
            return (minutes / 60) + "h ago";
        } else {
            return (minutes / 1440) + "d ago";
        }
    }

    /**
     * Hide the badge completely
     */
    public void hide() {
        getContent().setVisible(false);
    }

    /**
     * Show the badge
     */
    public void show() {
        getContent().setVisible(true);
    }
}
