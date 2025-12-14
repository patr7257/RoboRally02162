#!/bin/bash

set -e

echo "======================================"
echo "Starting Manual Deployment"
echo "======================================"

APP_DIR="/opt/roborally/app"
GATEWAY_PORT=8080
HOST_PORT=2948

# Pull latest code
echo "Pulling latest code from GitHub..."
cd $APP_DIR
git pull 

# Build Gateway
echo "Building Gateway service..."
cd $APP_DIR/gateway
mvn clean package -DskipTests

# Build Host
echo "Building Host service..."
cd $APP_DIR/host
mvn clean package -DskipTests

# Build React Client
echo "Building React Client..."
cd $APP_DIR/client
npm install
npm run build

# Stop existing processes
echo "Stopping existing processes..."
pm2 stop gateway || true
pm2 stop host || true

# Start Gateway
echo "Starting Gateway service on port $GATEWAY_PORT..."
cd $APP_DIR/gateway
pm2 start "java -jar target/*.jar --server.port=$GATEWAY_PORT" --name gateway

echo "Gateway buffer time"
sleep 10

# Start Host
echo "Starting Host service on port $HOST_PORT..."
cd $APP_DIR/host
pm2 start "java -jar target/*.jar --server.port=$HOST_PORT" --name host

# Save PM2 configuration
pm2 save

echo "======================================"
echo "Deployment Complete!"
echo "======================================"
echo "Gateway: http://localhost:$GATEWAY_PORT"
echo "Host: http://localhost:$HOST_PORT"
echo "React Client: Served via Nginx"
