#!/bin/bash

echo "======================================"
echo "Application Status"
echo "======================================"

# Check PM2 processes
echo -e "\nPM2 Processes:"
pm2 list

# Check Nginx
echo -e "\nNginx Status:"
sudo systemctl status nginx --no-pager | head -n 5

# Check port usage
echo -e "\nPort Usage:"
echo "Gateway (8080):"
sudo lsof -i :8080 || echo "Not listening"
echo -e "\nHost (2948):"
sudo lsof -i :2948 || echo "Not listening"
echo -e "\nNginx (80/443):"
sudo lsof -i :80 || echo "Not listening"


echo -e "\n======================================"
echo "Recent Gateway Logs:"
pm2 logs gateway --lines 200 --nostream

echo -e "\nRecent Host Logs:"
pm2 logs host --lines 10 --nostream
