package com.dating.ui.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;

/**
 * DistanceBadge - Shows distance to another user
 * Displays location icon with distance in km or miles
 */
public class DistanceBadge extends Composite<FlexLayout> {

    private final Icon locationIcon;
    private final Span distanceText;

    public DistanceBadge() {
        FlexLayout layout = getContent();
        layout.addClassName("distance-badge");
        layout.setAlignItems(FlexLayout.Alignment.CENTER);
        layout.getStyle().set("gap", "2px");

        locationIcon = new Icon(VaadinIcon.MAP_MARKER);
        locationIcon.setSize("14px");
        locationIcon.getStyle().set("color", "#666");

        distanceText = new Span();
        distanceText.getStyle()
            .set("font-size", "0.8rem")
            .set("color", "#666");

        layout.add(locationIcon, distanceText);
        layout.setVisible(false); // Hidden by default
    }

    /**
     * Set distance in kilometers
     */
    public void setDistanceKm(Integer distanceKm) {
        if (distanceKm == null) {
            getContent().setVisible(false);
            return;
        }

        if (distanceKm < 1) {
            distanceText.setText("< 1 km away");
        } else if (distanceKm == 1) {
            distanceText.setText("1 km away");
        } else {
            distanceText.setText(distanceKm + " km away");
        }

        getContent().setVisible(true);
    }

    /**
     * Set distance in miles
     */
    public void setDistanceMiles(Integer distanceMiles) {
        if (distanceMiles == null) {
            getContent().setVisible(false);
            return;
        }

        if (distanceMiles < 1) {
            distanceText.setText("< 1 mi away");
        } else if (distanceMiles == 1) {
            distanceText.setText("1 mi away");
        } else {
            distanceText.setText(distanceMiles + " mi away");
        }

        getContent().setVisible(true);
    }

    /**
     * Set just the location name
     */
    public void setLocation(String location) {
        if (location == null || location.isEmpty()) {
            getContent().setVisible(false);
            return;
        }

        distanceText.setText(location);
        getContent().setVisible(true);
    }

    /**
     * Hide the badge
     */
    public void hide() {
        getContent().setVisible(false);
    }
}
