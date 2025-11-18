package com.dating.ui.views;

import com.dating.ui.dto.Match;
import com.dating.ui.dto.User;
import com.dating.ui.service.MatchService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;

/**
 * UserDetailView - View match profile details
 * Shows full profile of a matched user
 */
@Route(value = "user", layout = MainLayout.class)
@PageTitle("User Profile | POC Dating")
@PermitAll
@Slf4j
public class UserDetailView extends VerticalLayout implements HasUrlParameter<String> {

    private final MatchService matchService;
    private String matchId;
    private Match match;

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMMM d, yyyy");

    public UserDetailView(MatchService matchService) {
        this.matchService = matchService;

        setSizeFull();
        setPadding(true);
        setAlignItems(Alignment.CENTER);
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        this.matchId = parameter;

        if (matchId != null && !matchId.isEmpty()) {
            loadMatch();
        } else {
            add(new Paragraph("Invalid match ID"));
        }
    }

    private void loadMatch() {
        try {
            match = matchService.getMatchDetails(matchId);

            if (match != null && match.getMatchedUser() != null) {
                createUI();
            } else {
                add(new Paragraph("Match not found"));
            }

        } catch (Exception ex) {
            log.error("Failed to load match: {}", matchId, ex);
            Notification.show("Failed to load profile",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void createUI() {
        User user = match.getMatchedUser();

        // Back button
        Button backButton = new Button(new Icon(VaadinIcon.ARROW_LEFT), e ->
            UI.getCurrent().navigate(MatchesView.class));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout topBar = new HorizontalLayout(backButton);
        topBar.setWidthFull();
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        // Profile card
        Div profileCard = new Div();
        profileCard.setWidth("400px");
        profileCard.getStyle()
            .set("background", "white")
            .set("border-radius", "16px")
            .set("box-shadow", "0 4px 12px rgba(0,0,0,0.1)")
            .set("overflow", "hidden");

        // Profile image
        Image profileImage = new Image();
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            profileImage.setSrc(user.getPhotoUrl());
        } else {
            profileImage.setSrc("https://via.placeholder.com/400x400?text=" +
                user.getFirstName().charAt(0));
        }
        profileImage.setWidth("100%");
        profileImage.setHeight("400px");
        profileImage.getStyle().set("object-fit", "cover");

        // User info section
        Div infoSection = new Div();
        infoSection.getStyle().set("padding", "1.5rem");

        // Name and age
        H2 nameAge = new H2(user.getFirstName() + ", " + user.getAge());
        nameAge.getStyle().set("margin", "0 0 0.5rem 0");

        // Location
        HorizontalLayout locationLayout = new HorizontalLayout();
        locationLayout.setSpacing(false);
        locationLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon locationIcon = new Icon(VaadinIcon.MAP_MARKER);
        locationIcon.setSize("16px");
        locationIcon.getStyle()
            .set("color", "#666")
            .set("margin-right", "4px");

        String location = user.getCity() != null ? user.getCity() : "";
        if (user.getCountry() != null && !user.getCountry().isEmpty()) {
            location += (location.isEmpty() ? "" : ", ") + user.getCountry();
        }
        Span locationText = new Span(location.isEmpty() ? "Location not set" : location);
        locationText.getStyle().set("color", "#666");

        locationLayout.add(locationIcon, locationText);

        // Match date
        HorizontalLayout matchDateLayout = new HorizontalLayout();
        matchDateLayout.setSpacing(false);
        matchDateLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        matchDateLayout.getStyle().set("margin-top", "0.5rem");

        Icon heartIcon = new Icon(VaadinIcon.HEART);
        heartIcon.setSize("16px");
        heartIcon.getStyle()
            .set("color", "#ef4444")
            .set("margin-right", "4px");

        String matchDate = "Matched";
        if (match.getCreatedAt() != null) {
            matchDate += " on " + DATE_FORMATTER.format(match.getCreatedAt());
        }
        Span matchDateText = new Span(matchDate);
        matchDateText.getStyle().set("color", "#666");

        matchDateLayout.add(heartIcon, matchDateText);

        // Bio section
        Div bioSection = new Div();
        bioSection.getStyle().set("margin-top", "1rem");

        H3 aboutTitle = new H3("About");
        aboutTitle.getStyle()
            .set("margin", "0 0 0.5rem 0")
            .set("font-size", "1rem")
            .set("color", "#333");

        Paragraph bio = new Paragraph(
            user.getBio() != null && !user.getBio().isEmpty()
                ? user.getBio()
                : "No bio yet"
        );
        bio.getStyle()
            .set("margin", "0")
            .set("color", "#666")
            .set("line-height", "1.5");

        bioSection.add(aboutTitle, bio);

        infoSection.add(nameAge, locationLayout, matchDateLayout, bioSection);
        profileCard.add(profileImage, infoSection);

        // Action buttons
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.getStyle().set("margin-top", "1rem");

        Button messageButton = new Button("Send Message", e -> {
            // Navigate to chat with this match
            // Find conversation ID from match
            if (match.getConversationId() != null) {
                UI.getCurrent().navigate(ChatView.class, match.getConversationId());
            } else {
                Notification.show("Chat not available yet",
                    3000, Notification.Position.TOP_CENTER);
            }
        });
        messageButton.setIcon(new Icon(VaadinIcon.CHAT));
        messageButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        actions.add(messageButton);

        add(topBar, profileCard, actions);
    }
}
