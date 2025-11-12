#!/bin/bash

# POC Dating - Stop All Microservices
# This script stops all backend services

set -e

echo "=================================================="
echo "  POC Dating - Stopping All Microservices"
echo "=================================================="
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Find and kill processes by port
stop_service_by_port() {
    local port=$1
    local service_name=$2

    echo "Stopping $service_name (port $port)..."

    # Find process using the port
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        pid=$(lsof -ti:$port 2>/dev/null || echo "")
    else
        # Linux
        pid=$(fuser $port/tcp 2>/dev/null | awk '{print $1}' || echo "")
    fi

    if [ -z "$pid" ]; then
        echo -e "${YELLOW}  ⚠ No process found on port $port${NC}"
    else
        kill $pid 2>/dev/null && echo -e "${GREEN}  ✓ Stopped (PID: $pid)${NC}" || echo -e "${RED}  ✗ Failed to stop${NC}"
    fi
}

# Service ports
stop_service_by_port 8081 "user-service"
stop_service_by_port 8082 "match-service"
stop_service_by_port 8083 "chat-service"
stop_service_by_port 8084 "recommendation-service"
stop_service_by_port 8090 "vaadin-ui-service"

echo ""
echo "=================================================="
echo "  All Services Stopped!"
echo "=================================================="
echo ""
