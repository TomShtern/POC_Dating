package com.dating.ui.views;

import com.dating.ui.dto.Match;
import com.dating.ui.service.MatchService;
import com.dating.ui.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import com.vaadin.flow.component.html.Div;

/**
 * Matches view - Display all matches
 */
@Route(value = "matches", layout = MainLayout.class)
@PageTitle("My Matches | POC Dating")
@PermitAll
@Slf4j
public class MatchesView extends VerticalLayout {

    private final MatchService matchService;
    private final UserService userService;
    private Grid<Match> matchGrid;
    private List<Match> allMatches;
    private VerticalLayout emptyState;

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMM d, yyyy");

    public MatchesView(MatchService matchService, UserService userService) {
        this.matchService = matchService;
        this.userService = userService;

        setSizeFull();
        setPadding(true);

        createUI();
        loadMatches();
    }

    private void createUI() {
        H2 title = new H2("Your Matches");

        // Sorting toolbar
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Select<String> sortSelect = new Select<>();
        sortSelect.setLabel("Sort by");
        sortSelect.setItems("Newest First", "Oldest First", "Name A-Z", "Name Z-A");
        sortSelect.setValue("Newest First");
        sortSelect.addValueChangeListener(e -> sortMatches(e.getValue()));

        Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(e -> loadMatches());

        toolbar.add(sortSelect, refreshButton);

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

            Button unmatchButton = new Button(new Icon(VaadinIcon.CLOSE_SMALL));
            unmatchButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            unmatchButton.getElement().setAttribute("title", "Unmatch");
            unmatchButton.addClickListener(e -> showUnmatchDialog(match));

            Button reportButton = new Button(new Icon(VaadinIcon.FLAG));
            reportButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            reportButton.getElement().setAttribute("title", "Report User");
            reportButton.addClickListener(e -> {
                String userName = match.getOtherUser() != null
                    ? match.getOtherUser().getFirstName()
                    : "User";
                String userId = match.getOtherUser() != null
                    ? match.getOtherUser().getId()
                    : null;
                if (userId != null) {
                    ReportUserDialog dialog = new ReportUserDialog(userService, userId, userName);
                    dialog.open();
                }
            });

            actions.add(viewButton, chatButton, unmatchButton, reportButton);
            return actions;
        }).setHeader("Actions").setWidth("180px").setFlexGrow(0);

        // Row click to view profile
        matchGrid.addItemClickListener(event -> {
            Match match = event.getItem();
            UI.getCurrent().navigate(UserDetailView.class, match.getId());
        });

        matchGrid.setSizeFull();
        matchGrid.getStyle().set("cursor", "pointer");

        add(title, toolbar, matchGrid);
    }

    private void sortMatches(String sortOption) {
        if (allMatches == null || allMatches.isEmpty()) return;

        List<Match> sorted = switch (sortOption) {
            case "Oldest First" -> allMatches.stream()
                .sorted(Comparator.comparing(Match::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
            case "Name A-Z" -> allMatches.stream()
                .sorted(Comparator.comparing(m ->
                    m.getOtherUser() != null ? m.getOtherUser().getFirstName() : "",
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
            case "Name Z-A" -> allMatches.stream()
                .sorted(Comparator.comparing(
                    (Match m) -> m.getOtherUser() != null ? m.getOtherUser().getFirstName() : "",
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).reversed())
                .toList();
            default -> allMatches.stream() // Newest First
                .sorted(Comparator.comparing(Match::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        };

        matchGrid.setItems(sorted);
    }

    private void loadMatches() {
        try {
            allMatches = matchService.getMyMatches();
            // Sort by newest first by default
            sortMatches("Newest First");

            if (allMatches.isEmpty()) {
                matchGrid.setVisible(false);
                showEmptyState();
            } else {
                matchGrid.setVisible(true);
                if (emptyState != null) {
                    emptyState.setVisible(false);
                }
            }

        } catch (Exception ex) {
            log.error("Failed to load matches", ex);
            matchGrid.setVisible(false);
            showEmptyState();
        }
    }

    private void showEmptyState() {
        if (emptyState == null) {
            emptyState = new VerticalLayout();
            emptyState.setAlignItems(Alignment.CENTER);
            emptyState.setJustifyContentMode(JustifyContentMode.CENTER);
            emptyState.setPadding(true);
            emptyState.addClassName("empty-state");

            Icon heartIcon = new Icon(VaadinIcon.HEART);
            heartIcon.setSize("48px");
            heartIcon.getStyle().set("color", "#d1d5db");

            H2 emptyTitle = new H2("No matches yet");
            emptyTitle.getStyle().set("color", "#9ca3af");

            Paragraph emptyText = new Paragraph(
                "Keep swiping to find your perfect match!");
            emptyText.getStyle()
                .set("color", "#9ca3af")
                .set("text-align", "center");

            Button discoverButton = new Button("Discover People", new Icon(VaadinIcon.SEARCH));
            discoverButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            discoverButton.addClickListener(e ->
                UI.getCurrent().navigate(""));

            emptyState.add(heartIcon, emptyTitle, emptyText, discoverButton);
            add(emptyState);
        }
        emptyState.setVisible(true);
    }

    private void showUnmatchDialog(Match match) {
        String userName = match.getOtherUser() != null
            ? match.getOtherUser().getFirstName()
            : "this user";

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Unmatch");
        dialog.setText("Are you sure you want to unmatch with " + userName + "? " +
            "This action cannot be undone and your conversation will be deleted.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Unmatch");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(event -> {
            try {
                matchService.unmatch(match.getId());

                Notification.show("Unmatched successfully",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                // Refresh the list
                loadMatches();

            } catch (Exception ex) {
                log.error("Failed to unmatch", ex);
                Notification.show("Failed to unmatch",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.open();
    }
}
