#!/bin/sh
# Entrypoint script for Workshop Hub with Docker-in-Docker
# Starts the Docker daemon, waits for it to be ready, then starts the Spring Boot app

set -e

echo "Starting Docker daemon..."
# Start dockerd directly in the background
dockerd --host=unix:///var/run/docker.sock --storage-driver=overlay2 &

# Wait for Docker daemon to be ready
echo "Waiting for Docker daemon to be ready..."
MAX_RETRIES=30
RETRY_COUNT=0
while ! docker info > /dev/null 2>&1; do
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        echo "ERROR: Docker daemon failed to start after $MAX_RETRIES attempts"
        exit 1
    fi
    echo "Waiting for Docker daemon... (attempt $RETRY_COUNT/$MAX_RETRIES)"
    sleep 2
done

echo "Docker daemon is ready!"
docker info | head -5

echo "Starting Workshop Hub application..."
exec java -jar /app/app.jar

