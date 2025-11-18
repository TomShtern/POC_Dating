package com.dating.ui.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Custom error view for handling route not found errors
 * Displays a friendly error page with navigation options
 */
@Route("error")
@PageTitle("Page Not Found | POC Dating")
public class ErrorView extends VerticalLayout implements HasErrorParameter<NotFoundException> {

    public ErrorView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        createUI();
    }

    private void createUI() {
        // Error icon
        Icon errorIcon = new Icon(VaadinIcon.WARNING);
        errorIcon.setSize("64px");
        errorIcon.getStyle().set("color", "#f59e0b");

        // Title
        H1 title = new H1("Page Not Found");
        title.getStyle()
            .set("color", "#333")
            .set("margin", "1rem 0 0.5rem 0");

        // Message
        Paragraph message = new Paragraph(
            "Sorry, we couldn't find the page you're looking for."
        );
        message.getStyle()
            .set("color", "#666")
            .set("text-align", "center");

        // Navigation buttons
        Button homeButton = new Button("Go to Discover", e ->
            UI.getCurrent().navigate(SwipeView.class));
        homeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button backButton = new Button("Go Back", e ->
            UI.getCurrent().getPage().getHistory().back());
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        add(errorIcon, title, message, homeButton, backButton);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        return HttpServletResponse.SC_NOT_FOUND;
    }
}
