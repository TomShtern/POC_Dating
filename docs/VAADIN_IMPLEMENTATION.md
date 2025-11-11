# Vaadin Implementation Guide

**Document Status:** ✅ **ACTIVE** - Current implementation approach
**Last Updated:** 2025-11-11
**Version:** 1.0

---

## Table of Contents

1. [Overview](#overview)
2. [Project Setup](#project-setup)
3. [Architecture Integration](#architecture-integration)
4. [Building Views](#building-views)
5. [Service Integration](#service-integration)
6. [WebSocket Chat Implementation](#websocket-chat-implementation)
7. [Security](#security)
8. [Styling and Theming](#styling-and-theming)
9. [Testing](#testing)
10. [Deployment](#deployment)
11. [Development Workflow](#development-workflow)

---

## Overview

This guide covers implementing the POC Dating Application frontend using Vaadin - a full-stack Java framework that allows building web UIs entirely in Java.

### Why Vaadin?

- ✅ 100% Java (team expertise)
- ✅ 3-week MVP timeline
- ✅ Type-safe integration with backend
- ✅ Built-in WebSocket support (@Push)
- ✅ Understand every line of code

### Architecture Decision

We're creating a new **Vaadin UI Service** that integrates with existing microservices:

```
┌────────────────────────────────────┐
│  Vaadin UI Service (Port 8090)     │  ← NEW SERVICE
│  - Pure Java views                 │
│  - Calls backend via Feign         │
│  - Spring Security integration     │
└────────────┬───────────────────────┘
             │ REST calls
┌────────────▼───────────────────────┐
│  API Gateway (8080)                │
└────────────┬───────────────────────┘
             │
   ┌─────────┼──────────┬────────────┐
   │         │          │            │
┌──▼──┐  ┌──▼──┐  ┌────▼──┐  ┌─────▼────┐
│User │  │Match│  │Chat   │  │Recommend │
│8081 │  │8082 │  │8083   │  │8084      │
└─────┘  └─────┘  └───────┘  └──────────┘
```

---

## Project Setup

### Step 1: Create Vaadin UI Service

```bash
cd backend

# Create new Maven module
mkdir vaadin-ui-service
cd vaadin-ui-service
```

### Step 2: Create pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.dating</groupId>
        <artifactId>poc-dating-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>vaadin-ui-service</artifactId>
    <packaging>jar</packaging>

    <name>Vaadin UI Service</name>
    <description>Web UI built with Vaadin (Pure Java)</description>

    <properties>
        <vaadin.version>24.3.0</vaadin.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.vaadin</groupId>
                <artifactId>vaadin-bom</artifactId>
                <version>${vaadin.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Vaadin -->
        <dependency>
            <groupId>com.vaadin</groupId>
            <artifactId>vaadin-spring-boot-starter</artifactId>
        </dependency>

        <!-- Common Library (shared DTOs) -->
        <dependency>
            <groupId>com.dating</groupId>
            <artifactId>common-library</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

        <!-- Spring Cloud OpenFeign (call microservices) -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <!-- Spring Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Redis (session storage) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.session</groupId>
            <artifactId>spring-session-data-redis</artifactId>
        </dependency>

        <!-- WebSocket (for chat) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>

        <!-- Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>com.vaadin</groupId>
            <artifactId>vaadin-testbench-junit5</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <!-- Vaadin production build -->
            <plugin>
                <groupId>com.vaadin</groupId>
                <artifactId>vaadin-maven-plugin</artifactId>
                <version>${vaadin.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-frontend</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### Step 3: Update Parent POM

Add new module to `backend/pom.xml`:

```xml
<modules>
    <module>common-library</module>
    <module>user-service</module>
    <module>match-service</module>
    <module>chat-service</module>
    <module>recommendation-service</module>
    <module>api-gateway</module>
    <module>vaadin-ui-service</module>  <!-- ADD THIS -->
</modules>
```

### Step 4: Create Application Class

`src/main/java/com/dating/ui/VaadinUIApplication.java`:

```java
package com.dating.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class VaadinUIApplication {
    public static void main(String[] args) {
        SpringApplication.run(VaadinUIApplication.class, args);
    }
}
```

### Step 5: Configuration

`src/main/resources/application.yml`:

```yaml
server:
  port: 8090

spring:
  application:
    name: vaadin-ui-service

  # Redis session storage
  session:
    store-type: redis
    redis:
      namespace: vaadin:session

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

# Vaadin configuration
vaadin:
  productionMode: false
  pnpm:
    enable: true

# Backend service URLs
services:
  user-service:
    url: http://${USER_SERVICE_HOST:localhost}:8081
  match-service:
    url: http://${MATCH_SERVICE_HOST:localhost}:8082
  chat-service:
    url: http://${CHAT_SERVICE_HOST:localhost}:8083
  recommendation-service:
    url: http://${RECOMMENDATION_SERVICE_HOST:localhost}:8084

# Security
jwt:
  secret: ${JWT_SECRET:your-secret-key-change-in-production}

# Logging
logging:
  level:
    com.dating: DEBUG
    com.vaadin: INFO
```

---

## Architecture Integration

### Feign Clients (Call Microservices)

`src/main/java/com/dating/ui/client/UserServiceClient.java`:

```java
package com.dating.ui.client;

import com.dating.common.dto.User;
import com.dating.common.dto.LoginRequest;
import com.dating.common.dto.AuthResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient {

    @PostMapping("/api/users/auth/login")
    AuthResponse login(@RequestBody LoginRequest request);

    @PostMapping("/api/users/auth/register")
    AuthResponse register(@RequestBody RegisterRequest request);

    @GetMapping("/api/users/{userId}")
    User getUser(@PathVariable String userId, @RequestHeader("Authorization") String token);

    @PutMapping("/api/users/{userId}")
    User updateUser(@PathVariable String userId, @RequestBody User user,
                    @RequestHeader("Authorization") String token);
}
```

`src/main/java/com/dating/ui/client/MatchServiceClient.java`:

```java
package com.dating.ui.client;

import com.dating.common.dto.User;
import com.dating.common.dto.SwipeRequest;
import com.dating.common.dto.SwipeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "match-service", url = "${services.match-service.url}")
public interface MatchServiceClient {

    @GetMapping("/api/matches/next-profile")
    User getNextProfile(@RequestHeader("Authorization") String token);

    @PostMapping("/api/matches/swipe")
    SwipeResponse recordSwipe(@RequestBody SwipeRequest request,
                              @RequestHeader("Authorization") String token);

    @GetMapping("/api/matches/my-matches")
    List<Match> getMyMatches(@RequestHeader("Authorization") String token);
}
```

### Service Layer (Business Logic)

`src/main/java/com/dating/ui/service/UserService.java`:

```java
package com.dating.ui.service;

import com.dating.common.dto.User;
import com.dating.common.dto.AuthResponse;
import com.dating.ui.client.UserServiceClient;
import com.dating.ui.security.SecurityUtils;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserServiceClient userClient;

    public AuthResponse login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        return userClient.login(request);
    }

    public User getCurrentUser() {
        String userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentToken();
        return userClient.getUser(userId, "Bearer " + token);
    }

    public User updateProfile(User user) {
        String token = SecurityUtils.getCurrentToken();
        return userClient.updateUser(user.getId(), user, "Bearer " + token);
    }
}
```

---

## Building Views

### Login View

`src/main/java/com/dating/ui/views/LoginView.java`:

```java
package com.dating.ui.views;

import com.dating.ui.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.slf4j.Slf4j;

@Route("login")
@PageTitle("Login | POC Dating")
@AnonymousAllowed
@Slf4j
public class LoginView extends VerticalLayout {

    private final UserService userService;

    private EmailField emailField;
    private PasswordField passwordField;
    private Button loginButton;
    private Button registerButton;

    public LoginView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        createUI();
    }

    private void createUI() {
        H1 title = new H1("❤️ POC Dating");

        emailField = new EmailField("Email");
        emailField.setPlaceholder("you@example.com");
        emailField.setRequired(true);
        emailField.setWidth("300px");

        passwordField = new PasswordField("Password");
        passwordField.setRequired(true);
        passwordField.setWidth("300px");

        loginButton = new Button("Login", e -> handleLogin());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidth("300px");

        registerButton = new Button("Create Account", e -> handleRegister());
        registerButton.setWidth("300px");

        add(title, emailField, passwordField, loginButton, registerButton);
    }

    private void handleLogin() {
        String email = emailField.getValue();
        String password = passwordField.getValue();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        try {
            AuthResponse response = userService.login(email, password);

            // Store token in session
            VaadinSession.getCurrent().setAttribute("authToken", response.getAccessToken());
            VaadinSession.getCurrent().setAttribute("userId", response.getUser().getId());

            // Navigate to main app
            UI.getCurrent().navigate(SwipeView.class);

        } catch (Exception ex) {
            log.error("Login failed", ex);
            showError("Invalid email or password");
        }
    }

    private void handleRegister() {
        UI.getCurrent().navigate(RegisterView.class);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
```

### Swipe View

`src/main/java/com/dating/ui/views/SwipeView.java`:

```java
package com.dating.ui.views;

import com.dating.common.dto.User;
import com.dating.ui.service.MatchService;
import com.dating.ui.components.ProfileCard;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Swipe | POC Dating")
@Slf4j
public class SwipeView extends VerticalLayout {

    private final MatchService matchService;

    private User currentUser;
    private ProfileCard profileCard;
    private Button likeButton;
    private Button superLikeButton;
    private Button passButton;

    public SwipeView(MatchService matchService) {
        this.matchService = matchService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        createUI();
        loadNextProfile();
    }

    private void createUI() {
        H2 title = new H2("Discover");

        profileCard = new ProfileCard();
        profileCard.setWidth("400px");
        profileCard.setHeight("600px");

        // Action buttons
        passButton = new Button("✖️ Pass", e -> handlePass());
        passButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        superLikeButton = new Button("⭐ Super Like", e -> handleSuperLike());
        superLikeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        likeButton = new Button("❤️ Like", e -> handleLike());
        likeButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout buttons = new HorizontalLayout(passButton, superLikeButton, likeButton);
        buttons.setSpacing(true);

        add(title, profileCard, buttons);
    }

    private void loadNextProfile() {
        try {
            currentUser = matchService.getNextProfile();

            if (currentUser != null) {
                profileCard.setUser(currentUser);
            } else {
                profileCard.showNoMoreProfiles();
            }

        } catch (Exception ex) {
            log.error("Failed to load profile", ex);
            Notification.show("Failed to load profiles", 3000, Notification.Position.TOP_CENTER);
        }
    }

    private void handleLike() {
        if (currentUser == null) return;

        SwipeResponse response = matchService.recordSwipe(currentUser.getId(), SwipeType.LIKE);

        if (response.isMatch()) {
            showMatchNotification(currentUser);
        }

        loadNextProfile();
    }

    private void handleSuperLike() {
        if (currentUser == null) return;

        SwipeResponse response = matchService.recordSwipe(currentUser.getId(), SwipeType.SUPER_LIKE);

        if (response.isMatch()) {
            showMatchNotification(currentUser);
        }

        loadNextProfile();
    }

    private void handlePass() {
        if (currentUser == null) return;

        matchService.recordSwipe(currentUser.getId(), SwipeType.PASS);
        loadNextProfile();
    }

    private void showMatchNotification(User user) {
        Notification notification = new Notification();
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.setDuration(5000);
        notification.setPosition(Notification.Position.MIDDLE);

        VerticalLayout content = new VerticalLayout();
        content.add(new H2("🎉 It's a Match!"));
        content.add(new Paragraph("You and " + user.getFirstName() + " liked each other!"));

        Button chatButton = new Button("Send Message", e -> {
            UI.getCurrent().navigate(ChatView.class, user.getId());
            notification.close();
        });
        chatButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        content.add(chatButton);
        notification.add(content);
        notification.open();
    }
}
```

### Profile Card Component

`src/main/java/com/dating/ui/components/ProfileCard.java`:

```java
package com.dating.ui.components;

import com.dating.common.dto.User;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class ProfileCard extends VerticalLayout {

    private Image profileImage;
    private H2 nameLabel;
    private Paragraph ageLocation;
    private Paragraph bio;

    public ProfileCard() {
        setSpacing(false);
        setPadding(false);
        addClassName("profile-card");

        profileImage = new Image();
        profileImage.setWidth("100%");
        profileImage.setHeight("400px");
        profileImage.getStyle()
            .set("object-fit", "cover")
            .set("border-radius", "8px 8px 0 0");

        VerticalLayout infoSection = new VerticalLayout();
        infoSection.setPadding(true);

        nameLabel = new H2();
        ageLocation = new Paragraph();
        ageLocation.getStyle().set("color", "var(--lumo-secondary-text-color)");

        bio = new Paragraph();
        bio.getStyle()
            .set("margin-top", "10px")
            .set("font-size", "14px");

        infoSection.add(nameLabel, ageLocation, bio);

        add(profileImage, infoSection);

        getStyle()
            .set("border", "1px solid var(--lumo-contrast-10pct)")
            .set("border-radius", "8px")
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");
    }

    public void setUser(User user) {
        profileImage.setSrc(user.getPhotoUrl() != null ? user.getPhotoUrl() : "/images/default-avatar.png");
        profileImage.setAlt(user.getFirstName());

        nameLabel.setText(user.getFirstName() + ", " + user.getAge());

        String location = user.getCity() != null ? user.getCity() : "Unknown location";
        ageLocation.setText(location);

        bio.setText(user.getBio() != null ? user.getBio() : "No bio available");
    }

    public void showNoMoreProfiles() {
        removeAll();

        VerticalLayout emptyState = new VerticalLayout();
        emptyState.setSizeFull();
        emptyState.setAlignItems(Alignment.CENTER);
        emptyState.setJustifyContentMode(JustifyContentMode.CENTER);

        H2 title = new H2("🎉 You're all caught up!");
        Paragraph text = new Paragraph("Check back later for more profiles");

        emptyState.add(title, text);
        add(emptyState);
    }
}
```

---

## WebSocket Chat Implementation

### Chat View with @Push

`src/main/java/com/dating/ui/views/ChatView.java`:

```java
package com.dating.ui.views;

import com.dating.common.dto.Message;
import com.dating.ui.service.ChatService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "chat/:conversationId", layout = MainLayout.class)
@PageTitle("Chat | POC Dating")
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET_XHR)
@Slf4j
public class ChatView extends VerticalLayout implements HasUrlParameter<String> {

    private final ChatService chatService;

    private String conversationId;
    private MessageList messageList;
    private MessageInput messageInput;

    public ChatView(ChatService chatService) {
        this.chatService = chatService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        createUI();
    }

    @Override
    public void setParameter(BeforeEvent event, String conversationId) {
        this.conversationId = conversationId;
        loadMessages();
        subscribeToMessages();
    }

    private void createUI() {
        messageList = new MessageList();
        messageList.setSizeFull();

        messageInput = new MessageInput();
        messageInput.setWidthFull();
        messageInput.addSubmitListener(e -> sendMessage(e.getValue()));

        add(messageList, messageInput);
        expand(messageList);
    }

    private void loadMessages() {
        try {
            List<Message> messages = chatService.getMessages(conversationId);
            displayMessages(messages);
        } catch (Exception ex) {
            log.error("Failed to load messages", ex);
        }
    }

    private void subscribeToMessages() {
        // Subscribe to WebSocket messages from chat-service
        chatService.subscribeToConversation(conversationId, this::onNewMessage);
    }

    private void sendMessage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        try {
            Message message = chatService.sendMessage(conversationId, text);

            // Add to UI immediately (optimistic update)
            addMessageToUI(message);

            messageInput.clear();

        } catch (Exception ex) {
            log.error("Failed to send message", ex);
            Notification.show("Failed to send message", 3000, Notification.Position.TOP_CENTER);
        }
    }

    private void onNewMessage(Message message) {
        // This method is called when WebSocket receives a message
        // Use UI.access() for thread-safe UI updates
        getUI().ifPresent(ui -> ui.access(() -> {
            addMessageToUI(message);
        }));
    }

    private void addMessageToUI(Message message) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        boolean isCurrentUser = message.getSenderId().equals(currentUserId);

        MessageListItem item = new MessageListItem(
            message.getText(),
            message.getCreatedAt(),
            isCurrentUser ? "You" : message.getSenderName()
        );
        item.setUserColorIndex(isCurrentUser ? 0 : 1);

        messageList.add(item);
    }

    private void displayMessages(List<Message> messages) {
        String currentUserId = SecurityUtils.getCurrentUserId();

        List<MessageListItem> items = messages.stream()
            .map(msg -> {
                boolean isCurrentUser = msg.getSenderId().equals(currentUserId);
                MessageListItem item = new MessageListItem(
                    msg.getText(),
                    msg.getCreatedAt(),
                    isCurrentUser ? "You" : msg.getSenderName()
                );
                item.setUserColorIndex(isCurrentUser ? 0 : 1);
                return item;
            })
            .collect(Collectors.toList());

        messageList.setItems(items);
    }

    @Override
    public void onDetach(DetachEvent event) {
        super.onDetach(event);
        // Unsubscribe from WebSocket when leaving view
        chatService.unsubscribeFromConversation(conversationId);
    }
}
```

### Chat Service with WebSocket Client

`src/main/java/com/dating/ui/service/ChatService.java`:

```java
package com.dating.ui.service;

import com.dating.common.dto.Message;
import com.dating.ui.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@Slf4j
public class ChatService {

    private final ChatServiceClient chatClient;
    private final WebSocketClient wsClient;
    private final Map<String, WebSocketSession> activeSessions;
    private final Map<String, Consumer<Message>> messageHandlers;

    public ChatService(ChatServiceClient chatClient) {
        this.chatClient = chatClient;
        this.wsClient = new StandardWebSocketClient();
        this.activeSessions = new ConcurrentHashMap<>();
        this.messageHandlers = new ConcurrentHashMap<>();
    }

    public List<Message> getMessages(String conversationId) {
        String token = SecurityUtils.getCurrentToken();
        return chatClient.getMessages(conversationId, "Bearer " + token);
    }

    public Message sendMessage(String conversationId, String text) {
        String token = SecurityUtils.getCurrentToken();
        SendMessageRequest request = new SendMessageRequest(text);
        return chatClient.sendMessage(conversationId, request, "Bearer " + token);
    }

    public void subscribeToConversation(String conversationId, Consumer<Message> messageHandler) {
        messageHandlers.put(conversationId, messageHandler);

        try {
            String wsUrl = "ws://localhost:8083/chat/ws/" + conversationId;
            String token = SecurityUtils.getCurrentToken();

            WebSocketSession session = wsClient.doHandshake(
                new TextWebSocketHandler() {
                    @Override
                    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                        // Parse incoming message
                        Message msg = parseMessage(message.getPayload());

                        // Call the message handler
                        Consumer<Message> handler = messageHandlers.get(conversationId);
                        if (handler != null) {
                            handler.accept(msg);
                        }
                    }
                },
                wsUrl
            ).get();

            // Send authentication
            session.sendMessage(new TextMessage("{\"token\": \"" + token + "\"}"));

            activeSessions.put(conversationId, session);

        } catch (Exception ex) {
            log.error("Failed to connect to chat WebSocket", ex);
        }
    }

    public void unsubscribeFromConversation(String conversationId) {
        WebSocketSession session = activeSessions.remove(conversationId);
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception ex) {
                log.error("Failed to close WebSocket session", ex);
            }
        }
        messageHandlers.remove(conversationId);
    }

    private Message parseMessage(String json) {
        // Parse JSON to Message object
        // Use Jackson or Gson
        return objectMapper.readValue(json, Message.class);
    }
}
```

---

## Security

### Security Configuration

`src/main/java/com/dating/ui/security/SecurityConfig.java`:

```java
package com.dating.ui.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        // Allow access to login and register pages
        setLoginView(http, LoginView.class);
    }
}
```

### Security Utils

`src/main/java/com/dating/ui/security/SecurityUtils.java`:

```java
package com.dating.ui.security;

import com.vaadin.flow.server.VaadinSession;

public class SecurityUtils {

    public static String getCurrentUserId() {
        VaadinSession session = VaadinSession.getCurrent();
        return session != null ? (String) session.getAttribute("userId") : null;
    }

    public static String getCurrentToken() {
        VaadinSession session = VaadinSession.getCurrent();
        return session != null ? (String) session.getAttribute("authToken") : null;
    }

    public static boolean isAuthenticated() {
        return getCurrentToken() != null;
    }

    public static void logout() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute("userId", null);
            session.setAttribute("authToken", null);
            session.close();
        }
    }
}
```

---

## Styling and Theming

### Custom Theme

`frontend/themes/dating-theme/styles.css`:

```css
:root {
    --dating-primary-color: #ff4458;
    --dating-secondary-color: #00bfa5;
}

.profile-card {
    transition: transform 0.2s;
}

.profile-card:hover {
    transform: scale(1.02);
}

/* Override Lumo theme colors */
html {
    --lumo-primary-color: var(--dating-primary-color);
}
```

Apply theme in `VaadinUIApplication.java`:

```java
@Theme("dating-theme")
@PWA(name = "POC Dating", shortName = "Dating")
public class VaadinUIApplication extends SpringBootApplication {
    // ...
}
```

---

## Testing

### View Test Example

```java
@SpringBootTest
@AutoConfigureTestDatabase
class SwipeViewTest {

    @Autowired
    private MatchService matchService;

    @Test
    void testLoadProfile() {
        // Mock service
        when(matchService.getNextProfile()).thenReturn(createTestUser());

        // Create view
        SwipeView view = new SwipeView(matchService);

        // Verify profile loaded
        assertNotNull(view.getCurrentUser());
    }
}
```

---

## Deployment

### Docker Configuration

`backend/vaadin-ui-service/Dockerfile`:

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/vaadin-ui-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Add to docker-compose.yml

```yaml
vaadin-ui:
  build:
    context: ./backend/vaadin-ui-service
    dockerfile: Dockerfile
  container_name: dating_vaadin_ui
  ports:
    - "8090:8090"
  environment:
    SERVER_PORT: 8090
    USER_SERVICE_HOST: user-service
    MATCH_SERVICE_HOST: match-service
    CHAT_SERVICE_HOST: chat-service
    RECOMMENDATION_SERVICE_HOST: recommendation-service
    REDIS_HOST: redis
    JWT_SECRET: ${JWT_SECRET}
  depends_on:
    - user-service
    - match-service
    - chat-service
    - recommendation-service
    - redis
  networks:
    - dating_network
  restart: unless-stopped
```

---

## Development Workflow

### Day 1: Setup & Login
1. Create vaadin-ui-service module ✓
2. Add dependencies ✓
3. Create LoginView ✓
4. Test login flow

### Day 2-3: Core Views
1. SwipeView with ProfileCard
2. MatchService integration
3. Basic styling

### Day 4-5: Chat
1. ChatView with @Push
2. WebSocket integration
3. Message persistence

### Day 6-7: Profile & Settings
1. ProfileView
2. SettingsView
3. User preferences

### Week 2: Polish
1. Error handling
2. Loading states
3. Notifications
4. Responsive design

### Week 3: Testing & Deploy
1. Integration tests
2. Docker setup
3. Documentation
4. Demo preparation

---

## Next Steps

1. **Start Implementation**: Follow this guide to create vaadin-ui-service
2. **Build First View**: Login page (Day 1)
3. **Iterate**: Add views incrementally
4. **Test Integration**: Ensure Feign clients work with backend
5. **Deploy**: Docker Compose local testing

---

## Troubleshooting

### Common Issues

**Issue: Feign client connection refused**
```
Solution: Ensure backend services are running and URLs are correct in application.yml
```

**Issue: WebSocket connection fails**
```
Solution: Check CORS configuration in chat-service, verify @Push annotation
```

**Issue: Session not persisting**
```
Solution: Verify Redis is running, check spring.session configuration
```

---

## Related Documents

- [FRONTEND_OPTIONS_ANALYSIS.md](FRONTEND_OPTIONS_ANALYSIS.md) - Why Vaadin was chosen
- [ARCHITECTURE.md](ARCHITECTURE.md) - Updated system architecture
- [DEVELOPMENT.md](DEVELOPMENT.md) - Development guide

---

**Ready to start coding?** Follow the setup steps and build your first view!
