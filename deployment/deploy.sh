#!/bin/bash
# ====================================================
# SICMS Backend Automated Deployment Script for Oracle VM
# ====================================================

set -e

APP_DIR="/opt/sicms-backend"
JAR_NAME="sicms-0.0.1-SNAPSHOT.jar"

echo "🚀 Starting SICMS Backend Deployment..."

# Create app directory if not exists
sudo mkdir -p $APP_DIR

# Build Maven Package
echo "📦 Packaging Spring Boot JAR..."
mvn clean package -DskipTests

# Copy JAR to deployment directory
echo "🚚 Deploying JAR to $APP_DIR..."
sudo cp target/$JAR_NAME $APP_DIR/app.jar
sudo chown -R ubuntu:ubuntu $APP_DIR

# Restart systemd service
echo "🔄 Restarting sicms-backend service..."
sudo systemctl daemon-reload
sudo systemctl restart sicms-backend

# Check status
echo "✅ Deployment successful! Service Status:"
sudo systemctl status sicms-backend --no-pager
