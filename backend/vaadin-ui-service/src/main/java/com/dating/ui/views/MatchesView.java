package com.dating.ui.views;

import com.dating.ui.dto.Match;
import com.dating.ui.service.MatchService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Matches view - Display all matches
 */
@Route(value = "matches", layout = MainLayout.class)
@PageTitle("My Matches | POC Dating")
@PermitAll
@Slf4j
public class MatchesView extends VerticalLayout {

    private final MatchService matchService;
    private Grid<Match> matchGrid;

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMM d, yyyy");

    public MatchesView(MatchService matchService) {
        this.matchService = matchService;

        setSizeFull();
        setPadding(true);

        createUI();
        loadMatches();
    }

    private void createUI() {
        H2 title = new H2("Your Matches");

        matchGrid = new Grid<>(Match.class, false);

        // Name column with photo icon
        matchGrid.addComponentColumn(match -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(Alignment.CENTER);
            row.setSpacing(true);

            Icon userIcon = new Icon(VaadinIcon.USER);
            userIcon.setSize("16px");
            userIcon.getStyle().set("color", "#667eea");

            String name = match.getOtherUser() != null
                ? match.getOtherUser().getFirstName()
                : "Unknown";
            if (match.getOtherUser() != null && match.getOtherUser().getLastName() != null) {
                name += " " + match.getOtherUser().getLastName();
            }
            Span nameSpan = new Span(name);
            nameSpan.getStyle().set("font-weight", "500");

            row.add(userIcon, nameSpan);
            return row;
        }).setHeader("Name").setFlexGrow(1);

        // Age column
        matchGrid.addColumn(match ->
            match.getOtherUser() != null ? match.getOtherUser().getAge() : null
        ).setHeader("Age").setWidth("80px").setFlexGrow(0);

        // Location column
        matchGrid.addColumn(match -> {
            if (match.getOtherUser() != null) {
                String city = match.getOtherUser().getCity();
                return city != null ? city : "—";
            }
            return "—";
        }).setHeader("Location").setWidth("120px").setFlexGrow(0);

        // Match date column
        matchGrid.addColumn(match -> {
            if (match.getCreatedAt() != null) {
                return DATE_FORMATTER.format(match.getCreatedAt());
            }
            return "—";
        }).setHeader("Matched On").setWidth("120px").setFlexGrow(0);

        // Unread indicator
        matchGrid.addComponentColumn(match -> {
            if (match.isHasUnreadMessages()) {
                Span badge = new Span("New");
                badge.getStyle()
                    .set("background", "#ef4444")
                    .set("color", "white")
                    .set("border-radius", "10px")
                    .set("padding", "2px 8px")
                    .set("font-size", "0.75rem");
                return badge;
            }
            return new Span("");
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        // Action buttons column
        matchGrid.addComponentColumn(match -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button viewButton = new Button(new Icon(VaadinIcon.EYE));
            viewButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            viewButton.getElement().setAttribute("title", "View Profile");
            viewButton.addClickListener(e ->
                UI.getCurrent().navigate(UserDetailView.class, match.getId()));

            Button chatButton = new Button(new Icon(VaadinIcon.CHAT));
            chatButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            chatButton.getElement().setAttribute("title", "Send Message");
            chatButton.addClickListener(e -> {
                if (match.getConversationId() != null) {
                    UI.getCurrent().navigate(ChatView.class, match.getConversationId());
                } else {
                    UI.getCurrent().navigate(MessagesView.class);
                }
            });

            actions.add(viewButton, chatButton);
            return actions;
        }).setHeader("Actions").setWidth("100px").setFlexGrow(0);

        // Row click to view profile
        matchGrid.addItemClickListener(event -> {
            Match match = event.getItem();
            UI.getCurrent().navigate(UserDetailView.class, match.getId());
        });

        matchGrid.setSizeFull();
        matchGrid.getStyle().set("cursor", "pointer");

        add(title, matchGrid);
    }

    private void loadMatches() {
        try {
            List<Match> matches = matchService.getMyMatches();
            matchGrid.setItems(matches);

            if (matches.isEmpty()) {
                add(new Paragraph("No matches yet. Keep swiping!"));
            }

        } catch (Exception ex) {
            log.error("Failed to load matches", ex);
            add(new Paragraph("Failed to load matches"));
        }
    }
}
