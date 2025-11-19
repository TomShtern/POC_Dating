package com.dating.ui.components.admin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Reusable statistics card component for admin dashboard
 */
public class StatCard extends Div {

    private final Span valueSpan;
    private final Span trendSpan;

    public StatCard(String title, String value, String trend, VaadinIcon icon) {
        addClassNames(
                LumoUtility.Padding.MEDIUM,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL
        );
        setWidth("200px");

        // Header with icon and title
        Div header = new Div();
        header.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.SMALL
        );

        Icon iconElement = icon.create();
        iconElement.addClassNames(LumoUtility.TextColor.PRIMARY);
        iconElement.setSize("20px");

        Span titleSpan = new Span(title);
        titleSpan.addClassNames(
                LumoUtility.FontSize.SMALL,
                LumoUtility.TextColor.SECONDARY
        );

        header.add(iconElement, titleSpan);

        // Value
        valueSpan = new Span(value);
        valueSpan.addClassNames(
                LumoUtility.FontSize.XXLARGE,
                LumoUtility.FontWeight.BOLD
        );

        // Trend
        trendSpan = new Span(trend);
        trendSpan.addClassNames(LumoUtility.FontSize.SMALL);
        updateTrendStyle(trend);

        add(header, valueSpan, trendSpan);
    }

    public void setValue(String value) {
        valueSpan.setText(value);
    }

    public void setTrend(String trend) {
        trendSpan.setText(trend);
        updateTrendStyle(trend);
    }

    private void updateTrendStyle(String trend) {
        trendSpan.removeClassNames(
                LumoUtility.TextColor.SUCCESS,
                LumoUtility.TextColor.ERROR,
                LumoUtility.TextColor.SECONDARY
        );

        if (trend != null) {
            if (trend.startsWith("+") || trend.contains("up") || trend.contains("increase")) {
                trendSpan.addClassName(LumoUtility.TextColor.SUCCESS);
            } else if (trend.startsWith("-") || trend.contains("down") || trend.contains("decrease")) {
                trendSpan.addClassName(LumoUtility.TextColor.ERROR);
            } else {
                trendSpan.addClassName(LumoUtility.TextColor.SECONDARY);
            }
        }
    }
}
