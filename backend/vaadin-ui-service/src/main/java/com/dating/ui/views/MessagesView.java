package com.dating.ui.views;

import com.dating.ui.dto.Conversation;
import com.dating.ui.service.ChatService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMM d, HH:mm");

    public MessagesView(ChatService chatService) {
        this.chatService = chatService;

        setSizeFull();
        setPadding(true);

        createUI();
        loadConversations();
    }

    private void createUI() {
        H2 title = new H2("Messages");

        conversationGrid = new Grid<>(Conversation.class, false);

        // User name column
        conversationGrid.addColumn(conv -> {
            if (conv.getOtherUser() != null) {
                String name = conv.getOtherUser().getFirstName();
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

        add(title, conversationGrid);
    }

    private void loadConversations() {
        try {
            List<Conversation> conversations = chatService.getConversations();
            conversationGrid.setItems(conversations);

            if (conversations.isEmpty()) {
                add(new Paragraph("No conversations yet. Start matching!"));
            }

        } catch (Exception ex) {
            log.error("Failed to load conversations", ex);
            add(new Paragraph("Failed to load conversations"));
        }
    }
}
