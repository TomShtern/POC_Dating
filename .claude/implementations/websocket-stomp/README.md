# Spring WebSocket + RabbitMQ STOMP Broker Relay Implementation

## Overview
Complete implementation files for enabling horizontal scaling of WebSocket connections using RabbitMQ STOMP Broker Relay in the POC Dating Chat Service.

## Research Document
**Location**: `/home/user/POC_Dating/.claude/WEBSOCKET_STOMP_RELAY.md`

This comprehensive 800+ line document contains:
- Exact Spring dependencies needed (spoiler: none additional required)
- Complete WebSocketConfig.java configuration with detailed comments
- RabbitMQ STOMP plugin configuration
- Connection pool settings and tuning
- Heartbeat configuration and timing
- Environment variables setup
- Application configuration
- Error handling implementation
- Docker Compose updates
- Testing procedures
- Performance tuning guide
- Troubleshooting reference

## Implementation Files

### 1. WebSocketConfig.java
**Purpose**: Main configuration class for WebSocket with STOMP relay support

**Key Features**:
- Dynamic broker selection (simple vs STOMP relay)
- Fallback to simple broker for development
- Full heartbeat configuration
- Thread pool settings for inbound/outbound channels
- Buffer size configuration
- Comprehensive logging

**How to Apply**:
```bash
cp WebSocketConfig.java backend/chat-service/src/main/java/com/dating/chat/config/
```

**Replace**: Current version at `/home/user/POC_Dating/backend/chat-service/src/main/java/com/dating/chat/config/WebSocketConfig.java`

---

### 2. WebSocketErrorHandler.java
**Purpose**: Handles WebSocket disconnections, STOMP errors, and session cleanup

**Key Features**:
- @EventListener for SessionDisconnectEvent
- Specific error diagnosis for common STOMP issues
- Session resource cleanup
- Detailed error logging with actionable hints

**How to Apply**:
```bash
cp WebSocketErrorHandler.java backend/chat-service/src/main/java/com/dating/chat/config/
```

**Note**: This is a NEW component, not replacing anything.

---

### 3. rabbitmq-enabled-plugins
**Purpose**: Configuration file that tells RabbitMQ which plugins to load

**Contents**:
```
[rabbitmq_management,rabbitmq_stomp].
```

**How to Apply**:
```bash
mkdir -p config
cp rabbitmq-enabled-plugins config/
```

**Docker Mapping**: Must be mounted as read-only in docker-compose.yml:
```yaml
volumes:
  - ./config/rabbitmq-enabled-plugins:/etc/rabbitmq/enabled_plugins:ro
```

---

### 4. application-stomp-config.yml
**Purpose**: Complete application.yml configuration with STOMP settings

**Key Sections**:
- Server configuration (port 8083)
- PostgreSQL datasource
- Redis cache
- RabbitMQ AMQP (for event exchange)
- RabbitMQ STOMP relay configuration
- JWT security
- Logging configuration
- WebSocket-specific settings

**How to Apply**:
Option A - Replace entire file:
```bash
cp application-stomp-config.yml backend/chat-service/src/main/resources/application.yml
```

Option B - Merge with existing (recommended):
1. Add the `app.rabbitmq.stomp` section to existing application.yml
2. Update `app.websocket.broker-type` configuration

---

### 5. docker-compose-changes.yaml
**Purpose**: Updated docker-compose.yml sections for STOMP support

**Changes**:
1. RabbitMQ service:
   - Add port 61613 for STOMP protocol
   - Add plugin configuration volume mount

2. Chat service environment:
   - Add 9 STOMP-related environment variables
   - Set WEBSOCKET_BROKER_TYPE to stomp

**How to Apply**:
1. Open `/home/user/POC_Dating/docker-compose.yml`
2. Update the `rabbitmq` service with new ports and volumes
3. Update the `chat-service` environment with new variables

**Critical**: Ensure RabbitMQ port 61613 is exposed:
```yaml
ports:
  - "61613:61613"   # STOMP protocol
```

---

### 6. env-example-additions.txt
**Purpose**: New environment variables to add to .env.example

**Variables**:
- RABBITMQ_STOMP_HOST (default: rabbitmq)
- RABBITMQ_STOMP_PORT (default: 61613)
- RABBITMQ_STOMP_CLIENT_LOGIN (default: guest)
- RABBITMQ_STOMP_CLIENT_PASSCODE (default: guest)
- RABBITMQ_STOMP_SYSTEM_LOGIN (optional)
- RABBITMQ_STOMP_SYSTEM_PASSCODE (optional)
- Connection pool and heartbeat settings
- WEBSOCKET_BROKER_TYPE (default: stomp)

**How to Apply**:
1. Open `/home/user/POC_Dating/.env.example`
2. Append contents of env-example-additions.txt
3. Update your `.env` file accordingly

---

### 7. IMPLEMENTATION_SUMMARY.md
**Purpose**: Quick reference guide and implementation checklist

**Contents**:
- Implementation steps (7-step checklist)
- Key configuration parameters
- Broker type selection (simple vs stomp)
- Message flow diagrams
- Verification procedures
- Performance tuning guide
- Troubleshooting quick reference
- Horizontal scaling examples
- Rollback procedures

---

## Implementation Workflow

### Phase 1: Infrastructure Setup (5 minutes)
1. Create `./config/rabbitmq-enabled-plugins` file
2. Update docker-compose.yml (RabbitMQ ports + Chat Service environment)
3. Update .env.example with STOMP variables

### Phase 2: Code Changes (10 minutes)
1. Replace WebSocketConfig.java
2. Add WebSocketErrorHandler.java
3. Update application.yml with STOMP configuration

### Phase 3: Deployment & Testing (10 minutes)
1. Rebuild Chat Service: `docker-compose build chat-service`
2. Restart services: `docker-compose down && docker-compose up -d`
3. Verify STOMP plugin: `telnet localhost 61613`
4. Check logs: `docker-compose logs -f chat-service | grep -i stomp`
5. Test WebSocket connection

### Phase 4: Verification (5 minutes)
1. STOMP port connectivity test
2. RabbitMQ management console check
3. Chat Service logs verification
4. WebSocket client connection test

**Total Implementation Time**: ~30 minutes

---

## Configuration Highlights

### Broker Type Selection
```yaml
# Development (simple broker)
app.websocket.broker-type: simple

# Production (RabbitMQ STOMP relay)
app.websocket.broker-type: stomp
```

### Heartbeat Configuration
```yaml
app.rabbitmq.stomp:
  heartbeat-interval: 60000       # 60 seconds
  heartbeat-tolerance: 180000     # 3 minutes
  reconnect-delay: 5000           # 5 seconds
```

### Connection Pool
```yaml
app.rabbitmq.stomp:
  connection-pool-size: 5  # Adjust for throughput
```

---

## Dependencies
No new Maven dependencies required!

Already included in pom.xml:
- `spring-boot-starter-websocket` - Includes STOMP relay support
- `spring-boot-starter-amqp` - RabbitMQ support

---

## Architecture

### Message Flow (STOMP Relay)
```
Client1 ←→ WebSocket ←→ [Chat Service 1] 
                              ↓ STOMP (port 61613)
                         [RabbitMQ STOMP Broker]
                              ↑ STOMP
                        [Chat Service 2]
                            ↑ WebSocket
                          Client2
```

### Horizontal Scaling
- Before: Single instance, 1000 concurrent users max
- After: Multiple instances (3+), 10,000+ concurrent users
- No single point of failure at Chat Service level
- Message distribution via RabbitMQ

---

## Verification Checklist

- [ ] RabbitMQ STOMP plugin enabled (port 61613 listening)
- [ ] Docker volumes mounted correctly (enabled_plugins file)
- [ ] Environment variables configured in docker-compose.yml
- [ ] application.yml has STOMP configuration
- [ ] WebSocketConfig.java replaced with new version
- [ ] WebSocketErrorHandler.java added
- [ ] Services restarted: `docker-compose down && docker-compose up -d`
- [ ] STOMP connectivity verified: `telnet localhost 61613`
- [ ] Chat Service logs show "STOMP Relay configured"
- [ ] WebSocket client can connect and subscribe

---

## Troubleshooting

### "Cannot connect to STOMP broker"
1. Verify port 61613 is exposed in docker-compose.yml
2. Check STOMP plugin is enabled: `docker exec dating_rabbitmq rabbitmq-plugins list | grep stomp`
3. Test connectivity: `telnet localhost 61613`

### "STOMP authentication failed"
1. Verify RABBITMQ_STOMP_CLIENT_LOGIN/PASSCODE in environment
2. Ensure RabbitMQ user exists: `docker exec dating_rabbitmq rabbitmqctl list_users`

### "Connection timeout"
1. Verify RABBITMQ_STOMP_HOST is correct and resolvable
2. Check network connectivity between services
3. Increase heartbeat tolerance if network is unstable

See WEBSOCKET_STOMP_RELAY.md for detailed troubleshooting.

---

## Testing

### WebSocket Connection Test (Browser Console)
```javascript
const stompClient = new StompJs.Client({
  brokerURL: "ws://localhost:8090/ws",
  onConnect: (frame) => {
    console.log("Connected to STOMP broker");
    stompClient.subscribe("/topic/chat/test", (msg) => {
      console.log("Received:", msg.body);
    });
  },
  onError: (error) => {
    console.error("STOMP error:", error);
  }
});
stompClient.activate();
```

### STOMP Port Connectivity Test (Terminal)
```bash
telnet localhost 61613
# Should respond with CONNECTED frame
```

---

## Performance Tuning

### For High Message Throughput
```yaml
connection-pool-size: 10
heartbeat-interval: 30000
heartbeat-tolerance: 90000
```

### For Resource Conservation
```yaml
connection-pool-size: 3
heartbeat-interval: 120000
heartbeat-tolerance: 300000
```

---

## Rollback Plan

### Option 1: Temporary Fallback (Keep Config)
```yaml
WEBSOCKET_BROKER_TYPE=simple
```
Restart service - will use in-memory broker.

### Option 2: Full Rollback
1. Revert WebSocketConfig.java to original
2. Remove STOMP environment variables
3. Restart services

No data loss - simple broker is transient.

---

## Related Documentation

- **Comprehensive Research**: `/home/user/POC_Dating/.claude/WEBSOCKET_STOMP_RELAY.md`
- **RabbitMQ STOMP Protocol**: https://www.rabbitmq.com/stomp.html
- **Spring WebSocket Guide**: https://spring.io/guides/gs/messaging-stomp-websocket/
- **Spring STOMP Relay Docs**: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/messaging/simp/broker/StompBrokerRelayMessageHandler.html

---

## Questions & Support

For detailed information on any aspect:
1. Check WEBSOCKET_STOMP_RELAY.md (comprehensive reference)
2. Review code comments in WebSocketConfig.java
3. See IMPLEMENTATION_SUMMARY.md for quick reference
4. Check docker-compose-changes.yaml for infrastructure details

---

**Status**: Research Complete - Ready for Implementation
**Last Updated**: 2025-11-18
**Version**: 1.0
