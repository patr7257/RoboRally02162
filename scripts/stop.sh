#!/bin/bash

## author: Asger Allin Jensen


echo "======================================"
echo "Stopping All Services"
echo "======================================"

echo "Stopping Gateway..."
pm2 stop gateway || echo "Gateway not running"

echo "Stopping Host..."
pm2 stop host || echo "Host not running"

echo "Removing from PM2..."
pm2 delete gateway || true
pm2 delete host || true

pm2 save

echo "======================================"
echo "All Services Stopped"
echo "======================================"
