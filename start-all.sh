#!/bin/bash

echo "========================================"
echo "    AI Interview System - Startup Script"
echo "========================================"
echo ""

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DATA_DIR="$SCRIPT_DIR/docker-data"

mkdir -p "$DATA_DIR/mysql"
mkdir -p "$DATA_DIR/redis"
mkdir -p "$DATA_DIR/chroma"
mkdir -p "$DATA_DIR/minio"

echo "Data directory: $DATA_DIR"
echo ""

echo "[1/9] Checking Docker..."
if ! command -v docker &> /dev/null; then
    echo "[ERROR] Docker is not installed"
    exit 1
fi
docker info > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "[ERROR] Docker is not running. Please start Docker first."
    exit 1
fi
echo "[OK] Docker is running"

echo ""
echo "[2/9] Cleaning up old containers..."
docker stop interview_mysql interview_redis interview_chroma interview_minio 2>/dev/null
docker rm interview_mysql interview_redis interview_chroma interview_minio 2>/dev/null
echo "[OK] Old containers removed"

echo ""
echo "[3/9] Starting MySQL on port 3307..."
docker run -d --name interview_mysql \
  -p 3307:3306 \
  -e MYSQL_ROOT_PASSWORD=1234 \
  -e MYSQL_DATABASE=ai_interview \
  -v "$DATA_DIR/mysql:/var/lib/mysql" \
  mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
echo "[OK] MySQL started on port 3307 (password: 1234)"
echo "    Data: $DATA_DIR/mysql"

echo ""
echo "[4/9] Starting Redis on port 6380..."
docker run -d --name interview_redis \
  -p 6380:6379 \
  -v "$DATA_DIR/redis:/data" \
  redis:7-alpine redis-server --requirepass 12345
echo "[OK] Redis started on port 6380 (password: 12345)"
echo "    Data: $DATA_DIR/redis"

echo ""
echo "[5/9] Starting Chroma on port 8000..."
docker run -d --name interview_chroma \
  -p 8000:8000 \
  -v "$DATA_DIR/chroma:/chroma/chroma" \
  chromadb/chroma
echo "[OK] Chroma started on port 8000"
echo "    Data: $DATA_DIR/chroma"

echo ""
echo "[6/9] Starting MinIO on port 9000..."
docker run -d --name interview_minio \
  -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  -v "$DATA_DIR/minio:/data" \
  minio/minio server /data --console-address ":9001"
echo "[OK] MinIO started on port 9000, Console on 9001"
echo "    Data: $DATA_DIR/minio"

echo ""
echo "[7/9] Waiting for Docker services..."
sleep 15
echo "[OK] Docker services are ready"

echo ""
echo "[8/9] Starting Ollama..."
nohup ollama serve > /dev/null 2>&1 &
echo "[OK] Ollama starting on port 11434..."

echo ""
echo "[9/9] Starting Whisper..."
nohup python3 whisper_server.py > whisper.log 2>&1 &
echo "[OK] Whisper starting on port 5000..."

echo ""
echo "========================================"
echo "    Starting Backend and Frontend"
echo "========================================"
nohup sh -c "cd $SCRIPT_DIR/backend && mvn spring-boot:run" > backend.log 2>&1 &
echo "[OK] Backend starting..."

nohup sh -c "cd $SCRIPT_DIR/frontend && npm run dev" > frontend.log 2>&1 &
echo "[OK] Frontend starting..."

echo ""
echo "========================================"
echo "    All services starting..."
echo "========================================"
echo ""
echo "Service URLs:"
echo "  - Frontend: http://localhost:5173"
echo "  - Backend: http://localhost:8080"
echo "  - MinIO Console: http://localhost:9001"
echo "  - Chroma: http://localhost:8000"
echo ""
echo "Docker Services:"
echo "  - MySQL: localhost:3307 (password: 1234)"
echo "  - Redis: localhost:6380 (password: 12345)"
echo ""
echo "Other Services:"
echo "  - Ollama: localhost:11434"
echo "  - Whisper: localhost:5000"
echo ""
echo "Data directories:"
echo "  $DATA_DIR"
echo "  - mysql/   (MySQL data)"
echo "  - redis/   (Redis data)"
echo "  - chroma/  (Chroma vector data)"
echo "  - minio/   (MinIO object storage)"
echo ""
echo "Log files:"
echo "  - backend.log"
echo "  - frontend.log"
echo "  - whisper.log"
echo "========================================"
