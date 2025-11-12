#!/bin/bash

# POC Dating - Check Service Health
# This script checks if all services are running and healthy

echo "=================================================="
echo "  POC Dating - Service Health Check"
echo "=================================================="
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check service health
check_service() {
    local service_name=$1
    local port=$2
    local health_url="http://localhost:$port/actuator/health"

    echo -n "Checking $service_name (port $port)... "

    # Check if port is open
    if ! nc -z localhost $port 2>/dev/null; then
        echo -e "${RED}❌ NOT RUNNING${NC}"
        return 1
    fi

    # Check health endpoint
    response=$(curl -s -o /dev/null -w "%{http_code}" $health_url 2>/dev/null)

    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✓ HEALTHY${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠ RUNNING but health check failed (HTTP $response)${NC}"
        return 2
    fi
}

# Check all services
echo "Service Status:"
echo ""

check_service "user-service" 8081
user_status=$?

check_service "match-service" 8082
match_status=$?

check_service "chat-service" 8083
chat_status=$?

check_service "recommendation-service" 8084
recommendation_status=$?

check_service "vaadin-ui-service" 8090
vaadin_status=$?

echo ""
echo "=================================================="
echo "  Summary"
echo "=================================================="
echo ""

# Calculate totals
total=5
healthy=0
running=0
stopped=0

for status in $user_status $match_status $chat_status $recommendation_status $vaadin_status; do
    if [ $status -eq 0 ]; then
        ((healthy++))
        ((running++))
    elif [ $status -eq 2 ]; then
        ((running++))
    else
        ((stopped++))
    fi
done

echo "Total Services: $total"
echo -e "Healthy: ${GREEN}$healthy${NC}"
echo -e "Running (unhealthy): ${YELLOW}$((running - healthy))${NC}"
echo -e "Stopped: ${RED}$stopped${NC}"

echo ""

if [ $healthy -eq $total ]; then
    echo -e "${GREEN}✓ All services are healthy!${NC}"
    echo ""
    echo "You can access the Vaadin UI at: http://localhost:8090"
    exit 0
elif [ $running -gt 0 ]; then
    echo -e "${YELLOW}⚠ Some services need attention.${NC}"
    echo ""
    echo "Check service logs for more details."
    exit 1
else
    echo -e "${RED}❌ No services are running.${NC}"
    echo ""
    echo "Start services with: ./start-all-services.sh"
    exit 2
fi
