#!/bin/bash

## author: Asger Allin Jensen

echo "======================================"
echo "Restarting All Services"
echo "======================================"

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
