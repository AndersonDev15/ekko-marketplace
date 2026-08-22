#!/bin/bash

# ==========================================
# Ekko - Start all services
# ==========================================

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$PROJECT_ROOT/logs"

mkdir -p "$LOG_DIR"

echo "=========================================="
echo "       Starting Ekko services"
echo "=========================================="

# ------------------------------------------
# Start Eureka Server
# ------------------------------------------

echo "Starting Eureka Server..."

cd "$PROJECT_ROOT/eureka-server"

nohup ./mvnw spring-boot:run \
    > "$LOG_DIR/eureka.log" 2>&1 &

EUREKA_PID=$!

echo "Eureka Server PID: $EUREKA_PID"
echo "Waiting for Eureka Server..."

# ------------------------------------------
# Wait for Eureka
# ------------------------------------------

until curl -sf http://localhost:8761/actuator/health > /dev/null; do
    sleep 2
done

echo "Eureka Server is UP!"
echo ""

# ------------------------------------------
# Start microservices
# ------------------------------------------

start_service() {

    SERVICE_NAME=$1

    echo "Starting $SERVICE_NAME..."

    cd "$PROJECT_ROOT/$SERVICE_NAME"

    nohup ./mvnw spring-boot:run \
        > "$LOG_DIR/$SERVICE_NAME.log" 2>&1 &

    echo "$SERVICE_NAME PID: $!"
}

start_service "api-gateway"
start_service "seller-service"
start_service "product-service"
start_service "order-service"
start_service "payment-service"
start_service "review-service"
start_service "notification-service"

echo ""
echo "=========================================="
echo "       All services started"
echo "=========================================="
echo ""
echo "Eureka:"
echo "http://localhost:8761"
echo ""
echo "Logs:"
echo "$LOG_DIR"
echo ""

