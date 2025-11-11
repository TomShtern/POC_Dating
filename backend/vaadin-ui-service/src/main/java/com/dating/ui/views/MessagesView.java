package com.dating.ui.views;

import com.dating.ui.dto.Conversation;
import com.dating.ui.service.ChatService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

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
        conversationGrid.addColumn(conv -> conv.getOtherUser().getFirstName()).setHeader("Name");
        conversationGrid.addColumn(conv ->
            conv.getLastMessage() != null ? conv.getLastMessage().getText() : "No messages yet"
        ).setHeader("Last Message");
        conversationGrid.addColumn(Conversation::getUnreadCount).setHeader("Unread");

        // TODO: Add click listener to navigate to ChatView
        // conversationGrid.addItemClickListener(event -> {
        //     Conversation conv = event.getItem();
        //     UI.getCurrent().navigate(ChatView.class, conv.getId());
        // });

        conversationGrid.setSizeFull();

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
