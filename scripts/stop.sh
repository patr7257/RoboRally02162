#!/bin/bash

echo "======================================"
echo "Stopping All Services"
echo "======================================"

# Stop PM2 processes
echo "Stopping Gateway..."
pm2 stop gateway || echo "Gateway not running"

echo "Stopping Host..."
pm2 stop host || echo "Host not running"

# Delete processes from PM2
echo "Removing from PM2..."
pm2 delete gateway || true
pm2 delete host || true

pm2 save

echo "======================================"
echo "All Services Stopped"
echo "======================================"
