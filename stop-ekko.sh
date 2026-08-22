#!/bin/bash

# ==========================================
# Ekko - Stop all services
# ==========================================

echo "=========================================="
echo "       Stopping Ekko services"
echo "=========================================="

pkill -f "spring-boot:run"

echo ""
echo "All Ekko services stopped."
