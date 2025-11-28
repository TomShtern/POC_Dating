package com.dating.ui.views;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

/**
 * AboutView - Information about the POC Dating application
 * Displays version, features, and contact info
 */
@Route(value = "about", layout = MainLayout.class)
@PageTitle("About | POC Dating")
@PermitAll
public class AboutView extends VerticalLayout {

    public AboutView() {
        setSizeFull();
        setPadding(true);
        setAlignItems(Alignment.CENTER);

        createUI();
    }

    private void createUI() {
        // Header
        Div header = new Div();
        header.getStyle().set("text-align", "center");

        H2 title = new H2("POC Dating");
        title.getStyle()
            .set("color", "var(--dating-primary-color)")
            .set("margin-bottom", "0.5rem");

        Span version = new Span("Version 1.0.0-SNAPSHOT");
        version.getStyle()
            .set("color", "#666")
            .set("font-size", "0.9rem");

        header.add(title, version);

        // Description
        Paragraph description = new Paragraph(
            "A proof-of-concept dating application demonstrating enterprise-quality " +
            "Java microservices with a Vaadin 24.3 frontend."
        );
        description.getStyle()
            .set("text-align", "center")
            .set("max-width", "500px")
            .set("color", "#666");

        // Features section
        Div featuresSection = createSection("Features", VaadinIcon.CHECK);

        VerticalLayout featuresList = new VerticalLayout();
        featuresList.setPadding(false);
        featuresList.setSpacing(false);

        String[] features = {
            "Profile discovery with swipe gestures",
            "Real-time chat messaging",
            "Match notifications",
            "User preferences and filtering",
            "Secure JWT authentication",
            "Responsive Vaadin UI"
        };

        for (String feature : features) {
            featuresList.add(createFeatureItem(feature));
        }

        featuresSection.add(featuresList);

        // Technology section
        Div techSection = createSection("Technology Stack", VaadinIcon.COG);

        VerticalLayout techList = new VerticalLayout();
        techList.setPadding(false);
        techList.setSpacing(false);

        String[] technologies = {
            "Java 21 with Spring Boot 3.2",
            "Vaadin 24.3 (Server-side Java UI)",
            "PostgreSQL + Redis + RabbitMQ",
            "Microservices Architecture",
            "Docker Containerization"
        };

        for (String tech : technologies) {
            techList.add(createFeatureItem(tech));
        }

        techSection.add(techList);

        // Credits section
        Div creditsSection = createSection("Credits", VaadinIcon.USER);

        Paragraph credits = new Paragraph(
            "Developed as a proof-of-concept for demonstrating enterprise Java patterns " +
            "with 100% Java stack (no React/TypeScript)."
        );
        credits.getStyle()
            .set("color", "#666")
            .set("font-size", "0.9rem");

        creditsSection.add(credits);

        // Container
        Div container = new Div();
        container.setWidth("600px");
        container.getStyle()
            .set("background", "white")
            .set("border-radius", "12px")
            .set("padding", "2rem")
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");

        container.add(header, description, featuresSection, techSection, creditsSection);

        add(container);
    }

    private Div createSection(String title, VaadinIcon icon) {
        Div section = new Div();
        section.getStyle().set("margin-top", "1.5rem");

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(Alignment.CENTER);
        header.setSpacing(true);

        Icon sectionIcon = new Icon(icon);
        sectionIcon.setSize("20px");
        sectionIcon.getStyle().set("color", "var(--dating-primary-color)");

        H3 sectionTitle = new H3(title);
        sectionTitle.getStyle()
            .set("margin", "0")
            .set("font-size", "1.1rem");

        header.add(sectionIcon, sectionTitle);
        section.add(header);

        return section;
    }

    private HorizontalLayout createFeatureItem(String text) {
        HorizontalLayout item = new HorizontalLayout();
        item.setAlignItems(Alignment.CENTER);
        item.setSpacing(true);
        item.getStyle().set("margin", "0.3rem 0");

        Icon checkIcon = new Icon(VaadinIcon.CHECK_CIRCLE);
        checkIcon.setSize("16px");
        checkIcon.getStyle().set("color", "#10b981");

        Span label = new Span(text);
        label.getStyle()
            .set("font-size", "0.9rem")
            .set("color", "#444");

        item.add(checkIcon, label);
        return item;
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        // Simple view - no listeners to clean up
    }
}
