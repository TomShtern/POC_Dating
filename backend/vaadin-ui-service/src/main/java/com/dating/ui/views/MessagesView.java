package com.dating.ui.views;

import com.dating.ui.dto.Conversation;
import com.dating.ui.service.ChatService;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Messages view - List of conversations
 * TODO: Implement ChatView with @Push for real-time messaging
 */
@Route(value = "messages", layout = MainLayout.class)
@PageTitle("Messages | POC Dating")
@PermitAll
@Slf4j
public class MessagesView extends VerticalLayout {

    private final ChatService chatService;
    private Grid<Conversation> conversationGrid;
    private TextField searchField;
    private List<Conversation> allConversations;
    private VerticalLayout emptyState;

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMM d, HH:mm");

    public MessagesView(ChatService chatService, PageViewMetricsService pageViewMetrics) {
        this.chatService = chatService;

        // Record page view metric
        pageViewMetrics.recordPageView("messages");

        setSizeFull();
        setPadding(true);

        createUI();
        loadConversations();
    }

    private void createUI() {
        H2 title = new H2("Messages");

        // Search and refresh bar
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);

        searchField = new TextField();
        searchField.setPlaceholder("Search conversations...");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> filterConversations(e.getValue()));
        searchField.setWidth("300px");

        Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(e -> loadConversations());

        toolbar.add(searchField, refreshButton);
        toolbar.expand(searchField);

        conversationGrid = new Grid<>(Conversation.class, false);

        // User name column
        conversationGrid.addColumn(conv -> {
            if (conv.getOtherUser() != null) {
                String firstName = conv.getOtherUser().getFirstName();
                String name = (firstName != null && !firstName.isEmpty()) ? firstName : "Unknown";
                if (conv.getOtherUser().getLastName() != null) {
                    name += " " + conv.getOtherUser().getLastName();
                }
                return name;
            }
            return "Unknown";
        }).setHeader("Name").setWidth("150px").setFlexGrow(0);

        // Last message preview
        conversationGrid.addColumn(conv -> {
            if (conv.getLastMessage() != null && conv.getLastMessage().getText() != null) {
                String text = conv.getLastMessage().getText();
                return text.length() > 50 ? text.substring(0, 47) + "..." : text;
            }
            return "No messages yet";
        }).setHeader("Last Message").setFlexGrow(1);

        // Timestamp column
        conversationGrid.addColumn(conv -> {
            if (conv.getLastMessage() != null && conv.getLastMessage().getCreatedAt() != null) {
                return DATE_FORMATTER.format(
                    conv.getLastMessage().getCreatedAt()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime()
                );
            }
            return "";
        }).setHeader("Time").setWidth("120px").setFlexGrow(0);

        // Unread count badge
        conversationGrid.addComponentColumn(conv -> {
            int unread = conv.getUnreadCount();
            if (unread > 0) {
                com.vaadin.flow.component.html.Span badge = new com.vaadin.flow.component.html.Span(String.valueOf(unread));
                badge.getStyle()
                    .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
                    .set("color", "white")
                    .set("border-radius", "12px")
                    .set("padding", "2px 8px")
                    .set("font-size", "0.85rem")
                    .set("font-weight", "500");
                return badge;
            }
            return new com.vaadin.flow.component.html.Span("");
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        // Click to open chat
        conversationGrid.addItemClickListener(event -> {
            Conversation conv = event.getItem();
            UI.getCurrent().navigate(ChatView.class, conv.getId());
        });

        // Styling
        conversationGrid.setSizeFull();
        conversationGrid.getStyle().set("cursor", "pointer");

        add(title, toolbar, conversationGrid);
    }

    private void filterConversations(String searchTerm) {
        if (allConversations == null) return;

        if (searchTerm == null || searchTerm.isEmpty()) {
            conversationGrid.setItems(allConversations);
        } else {
            String lowerSearch = searchTerm.toLowerCase();
            List<Conversation> filtered = allConversations.stream()
                .filter(conv -> {
                    if (conv.getOtherUser() != null) {
                        String firstName = conv.getOtherUser().getFirstName();
                        String name = (firstName != null && !firstName.isEmpty()) ? firstName : "";
                        if (conv.getOtherUser().getLastName() != null) {
                            name += " " + conv.getOtherUser().getLastName();
                        }
                        return name.toLowerCase().contains(lowerSearch);
                    }
                    return false;
                })
                .toList();
            conversationGrid.setItems(filtered);
        }
    }

    private void loadConversations() {
        try {
            allConversations = chatService.getConversations();
            conversationGrid.setItems(allConversations);

            if (allConversations.isEmpty()) {
                conversationGrid.setVisible(false);
                showEmptyState();
            } else {
                conversationGrid.setVisible(true);
                if (emptyState != null) {
                    emptyState.setVisible(false);
                }
            }

        } catch (Exception ex) {
            log.error("Failed to load conversations", ex);
            conversationGrid.setVisible(false);
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

            Icon chatIcon = new Icon(VaadinIcon.CHAT);
            chatIcon.setSize("48px");
            chatIcon.getStyle().set("color", "#d1d5db");

            H2 emptyTitle = new H2("No conversations yet");
            emptyTitle.getStyle().set("color", "#9ca3af");

            Paragraph emptyText = new Paragraph(
                "Match with someone to start chatting!");
            emptyText.getStyle()
                .set("color", "#9ca3af")
                .set("text-align", "center");

            Button matchesButton = new Button("View Matches", new Icon(VaadinIcon.HEART));
            matchesButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            matchesButton.addClickListener(e ->
                UI.getCurrent().navigate(MatchesView.class));

            emptyState.add(chatIcon, emptyTitle, emptyText, matchesButton);
            add(emptyState);
        }
        emptyState.setVisible(true);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        // Simple view - no listeners to clean up
    }
}
