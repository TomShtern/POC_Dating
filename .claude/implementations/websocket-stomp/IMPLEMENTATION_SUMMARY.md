# Spring WebSocket with RabbitMQ STOMP Broker Relay - Implementation Summary

## Overview
This document provides a quick reference for implementing horizontal scaling of WebSocket connections using RabbitMQ STOMP Broker Relay in the POC Dating application.

## Files Generated
1. **WebSocketConfig.java** - Complete configuration class
2. **WebSocketErrorHandler.java** - Error handling component
3. **rabbitmq-enabled-plugins** - RabbitMQ plugin configuration
4. **application.yml** - Complete YAML configuration
5. **docker-compose changes** - Updated docker-compose.yml sections
6. **env.example additions** - New environment variables
7. **WEBSOCKET_STOMP_RELAY.md** - Comprehensive research documentation

## Implementation Steps

### Step 1: Enable RabbitMQ STOMP Plugin

Create `./config/rabbitmq-enabled-plugins`:
```
[rabbitmq_management,rabbitmq_stomp].
```

This tells RabbitMQ to load the STOMP plugin on startup.

### Step 2: Update docker-compose.yml

In the `rabbitmq` service:
```yaml
ports:
  - "5672:5672"     # AMQP (existing)
  - "61613:61613"   # STOMP (new - for WebSocket relay)
  - "15672:15672"   # Management UI (existing)

volumes:
  - rabbitmq_data:/var/lib/rabbitmq
  - ./config/rabbitmq-enabled-plugins:/etc/rabbitmq/enabled_plugins:ro  # NEW
```

In the `chat-service` environment:
```yaml
RABBITMQ_STOMP_HOST: ${RABBITMQ_STOMP_HOST:-rabbitmq}
RABBITMQ_STOMP_PORT: ${RABBITMQ_STOMP_PORT:-61613}
RABBITMQ_STOMP_CLIENT_LOGIN: ${RABBITMQ_STOMP_CLIENT_LOGIN:-guest}
RABBITMQ_STOMP_CLIENT_PASSCODE: ${RABBITMQ_STOMP_CLIENT_PASSCODE:-guest}
RABBITMQ_STOMP_SYSTEM_LOGIN: ${RABBITMQ_STOMP_SYSTEM_LOGIN:-}
RABBITMQ_STOMP_SYSTEM_PASSCODE: ${RABBITMQ_STOMP_SYSTEM_PASSCODE:-}
RABBITMQ_STOMP_CONNECTION_POOL_SIZE: ${RABBITMQ_STOMP_CONNECTION_POOL_SIZE:-5}
RABBITMQ_STOMP_HEARTBEAT_INTERVAL: ${RABBITMQ_STOMP_HEARTBEAT_INTERVAL:-60000}
RABBITMQ_STOMP_HEARTBEAT_TOLERANCE: ${RABBITMQ_STOMP_HEARTBEAT_TOLERANCE:-180000}
RABBITMQ_STOMP_RECONNECT_DELAY: ${RABBITMQ_STOMP_RECONNECT_DELAY:-5000}
WEBSOCKET_BROKER_TYPE: ${WEBSOCKET_BROKER_TYPE:-stomp}
```

### Step 3: Update .env.example

Add all STOMP configuration variables (see env-example-additions.txt).

### Step 4: Update application.yml

Add the `app.rabbitmq.stomp` configuration section (see application-stomp-config.yml).

### Step 5: Replace WebSocketConfig.java

Replace the current WebSocketConfig.java with the new version that includes:
- Broker type detection (simple vs stomp)
- STOMP relay configuration
- Fallback to simple broker for development
- Thread pool configuration
- Heartbeat settings

### Step 6: Add WebSocketErrorHandler.java

Add new component in `src/main/java/com/dating/chat/config/WebSocketErrorHandler.java`:
- Handles disconnection events
- Logs STOMP errors with diagnostics
- Provides error recovery hints

### Step 7: Dependencies Verification

No new Maven dependencies required! Already have:
- spring-boot-starter-websocket (includes STOMP relay)
- spring-boot-starter-amqp (includes RabbitMQ support)

## Key Configuration Parameters

### Connection Pool
```yaml
connection-pool-size: 5  # Connections to maintain to RabbitMQ
```

### Heartbeat (Milliseconds)
```yaml
heartbeat-interval: 60000      # Send heartbeat every 60 seconds
heartbeat-tolerance: 180000    # Tolerate 180 seconds of silence
reconnect-delay: 5000          # Wait 5 seconds before reconnecting
```

### Buffer Sizes
```java
.setBufferSize(65536)           // 64 KB send buffer
.setReceiveBufferSize(65536)    // 64 KB receive buffer
```

### Thread Pools
```java
// Inbound channel (client → server)
corePoolSize: 8
maxPoolSize: 32
queueCapacity: 1000
keepAliveSeconds: 60

// Outbound channel (server → client)
corePoolSize: 8
maxPoolSize: 32
queueCapacity: 1000
keepAliveSeconds: 60
```

## Broker Type Selection

### Development: Simple Broker
```yaml
app.websocket.broker-type: simple
```

Advantages:
- No external dependencies
- Fast setup
- Good for local testing

Limitations:
- Single instance only
- No message persistence
- Lost on restart

### Production: STOMP Relay
```yaml
app.websocket.broker-type: stomp
```

Advantages:
- Horizontal scaling (multiple instances)
- Message persistence (in RabbitMQ)
- Distributed message routing
- Automatic failover

Requirements:
- RabbitMQ with STOMP plugin
- Network connectivity to RabbitMQ:61613

## Message Flow

### Development (Simple Broker)
```
Client1 ←→ WebSocket ←→ Chat Service
                            ↓
                      In-Memory Broker
                            ↓
Client2 ←→ WebSocket ←→ Chat Service (same instance)
```

### Production (STOMP Relay)
```
Client1 ←→ WebSocket ←→ Chat Service 1 ←→ STOMP ←→ RabbitMQ
                                           ↑ (port 61613)
Client2 ←→ WebSocket ←→ Chat Service 2 ←──┴──────
                                           STOMP
```

## Verification Steps

### 1. Verify STOMP Plugin is Running
```bash
# Test STOMP port connectivity
telnet localhost 61613

# Expected response:
# CONNECTED
# server:RabbitMQ/3.12.0
# version:1.0,1.1,1.2
```

### 2. Check RabbitMQ Management Console
```
http://localhost:15672
Admin → Plugins
Should see: [E*] rabbitmq_stomp
```

### 3. Verify Chat Service Logs
```bash
docker-compose logs -f chat-service | grep -i "stomp\|relay"

# Expected output:
# Configuring RabbitMQ STOMP Broker Relay: rabbitmq:61613
# STOMP Relay configured with:
```

### 4. Test WebSocket Connection
```javascript
const stompClient = new StompJs.Client({
  brokerURL: "ws://localhost:8090/ws",
  onConnect: (frame) => {
    console.log("Connected!");
    // Subscribe to chat topic
    stompClient.subscribe("/topic/chat/test", (msg) => {
      console.log(msg.body);
    });
  }
});
stompClient.activate();
```

## Performance Tuning

### For High Message Throughput
```yaml
connection-pool-size: 10              # Increase connections
heartbeat-interval: 30000             # More frequent heartbeats
heartbeat-tolerance: 90000            # Faster failure detection
```

### For Resource Conservation
```yaml
connection-pool-size: 3               # Fewer connections
heartbeat-interval: 120000            # Less frequent heartbeats
heartbeat-tolerance: 300000           # More tolerant
```

### For Many Concurrent Users
```java
corePoolSize: 16 to 32
maxPoolSize: 64 to 128
queueCapacity: 5000 to 10000
```

## Troubleshooting Quick Reference

| Issue | Root Cause | Fix |
|-------|-----------|-----|
| "Cannot connect to STOMP broker" | Port 61613 not listening | Check docker-compose port mapping + STOMP plugin enabled |
| "STOMP authentication failed" | Wrong credentials | Verify RABBITMQ_STOMP_CLIENT_LOGIN/PASSCODE |
| "Connection timeout" | Network unreachable | Check RABBITMQ_STOMP_HOST is resolvable from container |
| "Slow WebSocket messages" | Low connection pool | Increase connection-pool-size in config |
| "Frequent disconnections" | Heartbeat timeout | Increase heartbeat-tolerance or check network |

## Horizontal Scaling Example

### Before (Simple Broker)
```
Users: 1000 concurrent
Instances: 1
Bottleneck: Single chat-service instance
```

### After (STOMP Relay)
```
Users: 10,000+ concurrent
Instances: 3 (or more)
Distribution: RabbitMQ handles routing
No bottleneck: Messages routed through RabbitMQ
```

## Additional Resources

- **Detailed Configuration**: See WEBSOCKET_STOMP_RELAY.md
- **Code Examples**: WebSocketConfig.java, WebSocketErrorHandler.java
- **RabbitMQ Docs**: https://www.rabbitmq.com/stomp.html
- **Spring WebSocket Docs**: https://spring.io/guides/gs/messaging-stomp-websocket/

## Next Steps

1. Apply all code changes
2. Update docker-compose.yml and .env
3. Restart services: `docker-compose down && docker-compose up -d`
4. Verify STOMP plugin is running
5. Test WebSocket connections
6. Monitor logs for any STOMP errors
7. Load test with multiple concurrent connections
8. Monitor RabbitMQ STOMP connections in management console

## Rollback Plan

If issues occur, revert to simple broker:

### Option 1: Temporary (Keep Config)
```yaml
WEBSOCKET_BROKER_TYPE=simple
```

Restart service - will use in-memory broker.

### Option 2: Full Rollback
1. Revert WebSocketConfig.java to original version
2. Remove STOMP environment variables
3. Restart services

No data loss - simple broker doesn't persist.
