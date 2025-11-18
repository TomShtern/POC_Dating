package com.dating.ui.views;

import com.dating.ui.dto.Conversation;
import com.dating.ui.dto.Message;
import com.dating.ui.dto.MessageStatus;
import com.dating.ui.security.SecurityUtils;
import com.dating.ui.service.ChatService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ChatView - Real-time messaging interface
 * Displays conversation history and allows sending messages
 * Uses polling for real-time updates (fallback for WebSocket)
 */
@Route(value = "chat", layout = MainLayout.class)
@PageTitle("Chat | POC Dating")
@PermitAll
@Slf4j
public class ChatView extends VerticalLayout implements HasUrlParameter<String> {

    private final ChatService chatService;
    private String conversationId;
    private Conversation conversation;

    private VerticalLayout messagesContainer;
    private TextArea messageInput;
    private Button sendButton;
    private Scroller messagesScroller;
    private Div typingIndicator;

    private String currentUserId;
    private int lastMessageCount = 0;
    private long lastTypingNotification = 0;

    // Polling for real-time updates
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pollingTask;
    private Registration uiDetachRegistration;

    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    public ChatView(ChatService chatService) {
        this.chatService = chatService;
        this.currentUserId = SecurityUtils.getCurrentUserId();

        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        this.conversationId = parameter;

        if (conversationId != null && !conversationId.isEmpty()) {
            loadConversation();
            createUI();
            loadMessages();
        } else {
            add(new Div("Invalid conversation"));
        }
    }

    private void loadConversation() {
        try {
            conversation = chatService.getConversation(conversationId);
        } catch (Exception ex) {
            log.error("Failed to load conversation: {}", conversationId, ex);
        }
    }

    private void createUI() {
        // Header with back button and user name
        HorizontalLayout header = createHeader();

        // Messages container with scroller
        messagesContainer = new VerticalLayout();
        messagesContainer.setWidthFull();
        messagesContainer.setPadding(true);
        messagesContainer.setSpacing(true);

        messagesScroller = new Scroller(messagesContainer);
        messagesScroller.setSizeFull();
        messagesScroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        messagesScroller.getStyle()
            .set("background", "#f5f5f5")
            .set("flex-grow", "1");

        // Typing indicator
        typingIndicator = createTypingIndicator();
        typingIndicator.setVisible(false);

        // Message input area
        HorizontalLayout inputArea = createInputArea();

        add(header, messagesScroller, typingIndicator, inputArea);
        setFlexGrow(1, messagesScroller);
    }

    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(true);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle()
            .set("background", "white")
            .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)")
            .set("z-index", "1");

        // Back button
        Button backButton = new Button(new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClickListener(e ->
            UI.getCurrent().navigate(MessagesView.class));

        // User name
        String otherUserName = "Chat";
        if (conversation != null && conversation.getOtherUser() != null) {
            otherUserName = conversation.getOtherUser().getFirstName();
            if (conversation.getOtherUser().getLastName() != null) {
                otherUserName += " " + conversation.getOtherUser().getLastName();
            }
        }
        H3 title = new H3(otherUserName);
        title.getStyle().set("margin", "0");

        header.add(backButton, title);
        return header;
    }

    private HorizontalLayout createInputArea() {
        HorizontalLayout inputArea = new HorizontalLayout();
        inputArea.setWidthFull();
        inputArea.setPadding(true);
        inputArea.setSpacing(true);
        inputArea.setAlignItems(FlexComponent.Alignment.END);
        inputArea.getStyle()
            .set("background", "white")
            .set("border-top", "1px solid #e0e0e0");

        messageInput = new TextArea();
        messageInput.setPlaceholder("Type a message...");
        messageInput.setWidthFull();
        messageInput.setMinHeight("60px");
        messageInput.setMaxHeight("120px");
        messageInput.addKeyPressListener(Key.ENTER, e -> {
            if (!e.isShiftKey()) {
                sendMessage();
            }
        });

        // Send typing notification when user is typing
        messageInput.addValueChangeListener(e -> {
            long now = System.currentTimeMillis();
            // Send typing notification at most every 3 seconds
            if (now - lastTypingNotification > 3000) {
                lastTypingNotification = now;
                try {
                    chatService.sendTypingIndicator(conversationId);
                } catch (Exception ex) {
                    // Ignore typing indicator errors
                    log.debug("Failed to send typing indicator", ex);
                }
            }
        });

        sendButton = new Button(new Icon(VaadinIcon.PAPERPLANE));
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClickListener(e -> sendMessage());
        sendButton.getStyle()
            .set("min-width", "50px")
            .set("height", "50px");

        inputArea.add(messageInput, sendButton);
        inputArea.setFlexGrow(1, messageInput);

        return inputArea;
    }

    private void loadMessages() {
        try {
            List<Message> messages = chatService.getMessages(conversationId);
            messagesContainer.removeAll();

            for (Message message : messages) {
                addMessageBubble(message);
            }

            lastMessageCount = messages.size();
            scrollToBottom();

        } catch (Exception ex) {
            log.error("Failed to load messages", ex);
            Notification.show("Failed to load messages",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void addMessageBubble(Message message) {
        boolean isOwnMessage = message.getSenderId() != null &&
            message.getSenderId().equals(currentUserId);

        Div bubble = new Div();
        bubble.addClassName("message-bubble");
        bubble.addClassName(isOwnMessage ? "own-message" : "other-message");

        // Message text
        Div textDiv = new Div();
        textDiv.setText(message.getText());
        textDiv.getStyle()
            .set("word-wrap", "break-word")
            .set("white-space", "pre-wrap");

        // Timestamp and status
        HorizontalLayout meta = new HorizontalLayout();
        meta.setSpacing(false);
        meta.getStyle()
            .set("font-size", "0.75rem")
            .set("color", isOwnMessage ? "rgba(255,255,255,0.7)" : "#888")
            .set("margin-top", "4px");

        String timeStr = message.getCreatedAt() != null
            ? TIME_FORMATTER.format(message.getCreatedAt())
            : "";
        Span timeSpan = new Span(timeStr);
        meta.add(timeSpan);

        // Message status indicator for own messages
        if (isOwnMessage && message.getStatus() != null) {
            Icon statusIcon = getStatusIcon(message.getStatus());
            statusIcon.setSize("12px");
            statusIcon.getStyle().set("margin-left", "4px");
            meta.add(statusIcon);
        }

        bubble.add(textDiv, meta);

        // Bubble container for alignment
        Div bubbleWrapper = new Div(bubble);
        bubbleWrapper.getStyle()
            .set("display", "flex")
            .set("justify-content", isOwnMessage ? "flex-end" : "flex-start")
            .set("margin-bottom", "8px");

        // Bubble styling
        bubble.getStyle()
            .set("max-width", "70%")
            .set("padding", "12px 16px")
            .set("border-radius", isOwnMessage ? "18px 18px 4px 18px" : "18px 18px 18px 4px")
            .set("background", isOwnMessage
                ? "linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
                : "white")
            .set("color", isOwnMessage ? "white" : "#333")
            .set("box-shadow", "0 1px 2px rgba(0,0,0,0.1)");

        messagesContainer.add(bubbleWrapper);
    }

    private Icon getStatusIcon(MessageStatus status) {
        return switch (status) {
            case SENT -> {
                Icon icon = new Icon(VaadinIcon.CHECK);
                icon.getStyle().set("color", "rgba(255,255,255,0.7)");
                yield icon;
            }
            case DELIVERED -> {
                Icon icon = new Icon(VaadinIcon.CHECK);
                icon.getStyle().set("color", "rgba(255,255,255,0.9)");
                yield icon;
            }
            case READ -> {
                Icon icon = new Icon(VaadinIcon.CHECK_CIRCLE);
                icon.getStyle().set("color", "#4ade80");
                yield icon;
            }
        };
    }

    private void sendMessage() {
        String text = messageInput.getValue().trim();

        if (text.isEmpty()) {
            return;
        }

        try {
            // Optimistic update - show message immediately
            Message optimisticMessage = Message.builder()
                .senderId(currentUserId)
                .text(text)
                .createdAt(Instant.now())
                .status(MessageStatus.SENT)
                .build();

            addMessageBubble(optimisticMessage);
            scrollToBottom();
            messageInput.clear();
            messageInput.focus();

            // Send to backend
            chatService.sendMessage(conversationId, text);
            lastMessageCount++;

        } catch (Exception ex) {
            log.error("Failed to send message", ex);
            Notification.show("Failed to send message",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void scrollToBottom() {
        // Scroll to bottom after DOM update
        getElement().executeJs(
            "setTimeout(() => {" +
            "  const scroller = this.querySelector('vaadin-scroller');" +
            "  if (scroller) scroller.scrollTop = scroller.scrollHeight;" +
            "}, 100)"
        );
    }

    private Div createTypingIndicator() {
        Div indicator = new Div();
        indicator.getStyle()
            .set("padding", "8px 16px")
            .set("background", "white")
            .set("color", "#666")
            .set("font-size", "0.85rem")
            .set("font-style", "italic")
            .set("border-top", "1px solid #f0f0f0");

        String otherUserName = "Someone";
        if (conversation != null && conversation.getOtherUser() != null) {
            otherUserName = conversation.getOtherUser().getFirstName();
        }

        // Animated typing dots
        Span text = new Span(otherUserName + " is typing");
        Span dots = new Span("...");
        dots.getStyle()
            .set("animation", "typing-dots 1.4s infinite")
            .set("display", "inline-block");

        indicator.add(text, dots);
        return indicator;
    }

    private void showTypingIndicator() {
        if (typingIndicator != null) {
            typingIndicator.setVisible(true);

            // Hide after 4 seconds if no new typing notification
            getUI().ifPresent(ui -> ui.access(() -> {
                try {
                    Thread.sleep(4000);
                    typingIndicator.setVisible(false);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Start polling for new messages
        UI ui = attachEvent.getUI();
        pollingTask = scheduler.scheduleAtFixedRate(() -> {
            if (ui.isClosing()) {
                return;
            }

            ui.access(() -> {
                try {
                    List<Message> messages = chatService.getMessages(conversationId);
                    if (messages.size() > lastMessageCount) {
                        // New messages received
                        List<Message> newMessages = messages.subList(
                            lastMessageCount, messages.size());

                        for (Message msg : newMessages) {
                            // Don't add own messages again (already added optimistically)
                            if (msg.getSenderId() == null ||
                                !msg.getSenderId().equals(currentUserId)) {
                                addMessageBubble(msg);
                            }
                        }

                        lastMessageCount = messages.size();
                        scrollToBottom();
                    }
                } catch (Exception ex) {
                    log.debug("Polling error: {}", ex.getMessage());
                }
            });
        }, 2, 2, TimeUnit.SECONDS);

        // Register UI detach handler
        uiDetachRegistration = ui.addDetachListener(e -> stopPolling());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        stopPolling();
    }

    private void stopPolling() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            pollingTask.cancel(true);
        }
        if (uiDetachRegistration != null) {
            uiDetachRegistration.remove();
        }
    }
}
