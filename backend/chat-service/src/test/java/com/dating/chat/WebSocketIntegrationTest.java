package com.dating.chat;

import com.dating.chat.dto.websocket.ChatMessageEvent;
import com.dating.chat.dto.websocket.MessageType;
import com.dating.chat.dto.websocket.SendMessageRequest;
import com.dating.chat.model.Conversation;
import com.dating.chat.repository.ConversationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for WebSocket chat functionality.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private WebSocketStompClient stompClient;
    private String websocketUrl;

    private UUID user1Id;
    private UUID user2Id;
    private UUID matchId;

    @BeforeEach
    void setUp() {
        websocketUrl = "ws://localhost:" + port + "/ws";

        // Create STOMP client with SockJS
        SockJsClient sockJsClient = new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))
        );

        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // Create test users and conversation
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();
        matchId = UUID.randomUUID();

        // Ensure user1Id < user2Id for consistency
        if (user1Id.compareTo(user2Id) > 0) {
            UUID temp = user1Id;
            user1Id = user2Id;
            user2Id = temp;
        }

        // Create conversation for testing
        Conversation conversation = Conversation.builder()
                .matchId(matchId)
                .user1Id(user1Id)
                .user2Id(user2Id)
                .build();
        conversationRepository.save(conversation);
    }

    @Test
    void shouldConnectWithValidToken() throws Exception {
        String token = generateToken(user1Id.toString(), "testuser");

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        StompSession session = stompClient.connectAsync(
                websocketUrl,
                (org.springframework.web.socket.WebSocketHttpHeaders) null,
                connectHeaders,
                (StompSessionHandler) new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);

        assertThat(session.isConnected()).isTrue();
        session.disconnect();
    }

    @Test
    void shouldSendAndReceiveMessage() throws Exception {
        // Create tokens for both users
        String token1 = generateToken(user1Id.toString(), "user1");
        String token2 = generateToken(user2Id.toString(), "user2");

        // Connect user 2 to receive messages
        StompHeaders headers2 = new StompHeaders();
        headers2.add("Authorization", "Bearer " + token2);

        BlockingQueue<ChatMessageEvent> receivedMessages = new LinkedBlockingQueue<>();

        StompSession session2 = stompClient.connectAsync(
                websocketUrl,
                (org.springframework.web.socket.WebSocketHttpHeaders) null,
                headers2,
                (StompSessionHandler) new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);

        // Subscribe to messages
        session2.subscribe("/user/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessages.add((ChatMessageEvent) payload);
            }
        });

        // Wait for subscription to be established
        Thread.sleep(500);

        // Connect user 1 to send message
        StompHeaders headers1 = new StompHeaders();
        headers1.add("Authorization", "Bearer " + token1);

        StompSession session1 = stompClient.connectAsync(
                websocketUrl,
                (org.springframework.web.socket.WebSocketHttpHeaders) null,
                headers1,
                (StompSessionHandler) new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);

        // Send message from user 1
        SendMessageRequest request = new SendMessageRequest(
                matchId,
                "Hello from user 1!",
                MessageType.TEXT,
                null,
                null
        );
        session1.send("/app/chat.send", request);

        // Wait for message to be received
        ChatMessageEvent received = receivedMessages.poll(5, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.matchId()).isEqualTo(matchId);
        assertThat(received.senderId()).isEqualTo(user1Id);
        assertThat(received.content()).isEqualTo("Hello from user 1!");
        assertThat(received.type()).isEqualTo(MessageType.TEXT);

        // Cleanup
        session1.disconnect();
        session2.disconnect();
    }

    /**
     * Generate a JWT token for testing.
     */
    private String generateToken(String userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId)
                .claim("username", username)
                .claim("email", username + "@test.com")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setIssuer("dating-app")
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
    }
}
