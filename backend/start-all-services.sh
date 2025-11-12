#!/bin/bash

# POC Dating - Start All Microservices
# This script starts all backend services in separate terminal windows/tabs

set -e

echo "=================================================="
echo "  POC Dating - Starting All Microservices"
echo "=================================================="
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Check prerequisites
echo "Checking prerequisites..."

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java is not installed. Please install Java 21 or higher.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java found: $(java -version 2>&1 | head -n 1)${NC}"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven is not installed. Please install Maven 3.8 or higher.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Maven found: $(mvn -v | head -n 1)${NC}"

# Check PostgreSQL
if ! command -v psql &> /dev/null; then
    echo -e "${YELLOW}⚠ PostgreSQL client (psql) not found. Make sure PostgreSQL is installed.${NC}"
    echo -e "${YELLOW}  Services will fail if databases are not set up.${NC}"
else
    echo -e "${GREEN}✓ PostgreSQL client found${NC}"
fi

echo ""
echo "=================================================="
echo "  Database Setup Check"
echo "=================================================="
echo ""
echo "Make sure you have run the database setup script:"
echo "  psql -U postgres -f $SCRIPT_DIR/setup-databases.sql"
echo ""
read -p "Have you set up the databases? (y/n) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Please run the database setup script first, then try again.${NC}"
    exit 1
fi

echo ""
echo "=================================================="
echo "  Starting Services"
echo "=================================================="
echo ""

# Service directories
SERVICES=(
    "user-service:8081"
    "match-service:8082"
    "chat-service:8083"
    "recommendation-service:8084"
    "vaadin-ui-service:8090"
)

# Function to start a service in a new terminal
start_service() {
    local service_name=$1
    local service_port=$2
    local service_dir="$SCRIPT_DIR/$service_name"

    echo "Starting $service_name on port $service_port..."

    # Check if directory exists
    if [ ! -d "$service_dir" ]; then
        echo -e "${RED}❌ Service directory not found: $service_dir${NC}"
        return 1
    fi

    # Detect terminal and start service
    if command -v gnome-terminal &> /dev/null; then
        # Linux with GNOME
        gnome-terminal --tab --title="$service_name" -- bash -c "cd '$service_dir' && mvn spring-boot:run; exec bash"
    elif command -v konsole &> /dev/null; then
        # Linux with KDE
        konsole --new-tab -e bash -c "cd '$service_dir' && mvn spring-boot:run; exec bash" &
    elif command -v xterm &> /dev/null; then
        # Generic X terminal
        xterm -title "$service_name" -e "cd '$service_dir' && mvn spring-boot:run; bash" &
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        osascript -e "tell application \"Terminal\" to do script \"cd '$service_dir' && mvn spring-boot:run\""
    else
        # Fallback: run in background with logs
        echo -e "${YELLOW}⚠ No supported terminal found. Starting $service_name in background...${NC}"
        cd "$service_dir"
        nohup mvn spring-boot:run > "$service_dir/$service_name.log" 2>&1 &
        echo "  Log file: $service_dir/$service_name.log"
    fi

    echo -e "${GREEN}✓ $service_name started${NC}"
    sleep 2  # Give it time to start before launching next
}

# Start all services
for service_info in "${SERVICES[@]}"; do
    IFS=':' read -r service_name service_port <<< "$service_info"
    start_service "$service_name" "$service_port"
done

echo ""
echo "=================================================="
echo "  All Services Started!"
echo "=================================================="
echo ""
echo "Services are starting up. This may take a minute..."
echo ""
echo "Service URLs:"
echo "  User Service:            http://localhost:8081/api"
echo "  Match Service:           http://localhost:8082/api"
echo "  Chat Service:            http://localhost:8083/api/chat"
echo "  Recommendation Service:  http://localhost:8084/api"
echo "  Vaadin UI:               http://localhost:8090"
echo ""
echo "Health Check URLs:"
echo "  User Service:            http://localhost:8081/actuator/health"
echo "  Match Service:           http://localhost:8082/actuator/health"
echo "  Chat Service:            http://localhost:8083/actuator/health"
echo "  Recommendation Service:  http://localhost:8084/actuator/health"
echo "  Vaadin UI:               http://localhost:8090/actuator/health"
echo ""
echo "To stop services: Close the terminal windows or use Ctrl+C"
echo ""
echo -e "${GREEN}Happy coding! 🚀${NC}"
