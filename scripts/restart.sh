#!/bin/bash

echo "======================================"
echo "Restarting All Services"
echo "======================================"

# Restart PM2 processes
echo "Restarting Gateway..."
pm2 restart gateway

echo "Restarting Host..."
pm2 restart host

# Reload Nginx
echo "Reloading Nginx..."
sudo systemctl reload nginx

echo "======================================"
echo "All Services Restarted"
echo "======================================"
