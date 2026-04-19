@echo off
chcp 65001 >nul 2>&1
echo ========================================
echo     AI Interview System - Startup Script
echo ========================================
echo.

set SCRIPT_DIR=%~dp0
set DATA_DIR=%SCRIPT_DIR%docker-data

if not exist "%DATA_DIR%" mkdir "%DATA_DIR%"
if not exist "%DATA_DIR%\mysql" mkdir "%DATA_DIR%\mysql"
if not exist "%DATA_DIR%\redis" mkdir "%DATA_DIR%\redis"
if not exist "%DATA_DIR%\chroma" mkdir "%DATA_DIR%\chroma"
if not exist "%DATA_DIR%\minio" mkdir "%DATA_DIR%\minio"

echo Data directory: %DATA_DIR%
echo.

echo [1/10] Checking Python dependencies...
python -m pip install -r "%SCRIPT_DIR%requirements.txt" --quiet
if errorlevel 1 (
    echo [WARNING] Some Python packages may not be installed correctly
) else (
    echo [OK] Python dependencies ready
)

echo.
echo [2/10] Checking Docker...
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not running. Please start Docker Desktop first.
    pause
    exit /b 1
)
echo [OK] Docker is running

echo.
echo [3/10] Cleaning up old processes and containers...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM node.exe 2>nul
taskkill /F /IM python.exe 2>nul
docker stop interview_mysql interview_redis interview_chroma interview_minio 2>nul
docker rm interview_mysql interview_redis interview_chroma interview_minio 2>nul
echo [OK] Old processes and containers removed

echo.
echo [4/10] Starting MySQL on port 3307...
docker run -d --name interview_mysql ^
  -p 3307:3306 ^
  -e MYSQL_ROOT_PASSWORD=1234 ^
  -e MYSQL_DATABASE=ai_interview ^
  -v "%DATA_DIR%\mysql:/var/lib/mysql" ^
  mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
if errorlevel 1 (
    echo [ERROR] Failed to start MySQL
) else (
    echo [OK] MySQL started on port 3307 (password: 1234)
    echo     Data: %DATA_DIR%\mysql
)

echo.
echo [5/10] Starting Redis on port 6380...
docker run -d --name interview_redis ^
  -p 6380:6379 ^
  -v "%DATA_DIR%\redis:/data" ^
  redis:7-alpine redis-server --requirepass 12345
if errorlevel 1 (
    echo [ERROR] Failed to start Redis
) else (
    echo [OK] Redis started on port 6380 (password: 12345)
    echo     Data: %DATA_DIR%\redis
)

echo.
echo [6/10] Starting Chroma on port 8000...
docker run -d --name interview_chroma ^
  -p 8000:8000 ^
  -v "%DATA_DIR%\chroma:/chroma/chroma" ^
  chromadb/chroma
if errorlevel 1 (
    echo [ERROR] Failed to start Chroma
) else (
    echo [OK] Chroma started on port 8000
    echo     Data: %DATA_DIR%\chroma
)

echo.
echo [7/10] Starting MinIO on port 9000...
docker run -d --name interview_minio ^
  -p 9000:9000 -p 9001:9001 ^
  -e MINIO_ROOT_USER=minioadmin ^
  -e MINIO_ROOT_PASSWORD=minioadmin ^
  -v "%DATA_DIR%\minio:/data" ^
  minio/minio server /data --console-address ":9001"
if errorlevel 1 (
    echo [ERROR] Failed to start MinIO. Port 9000 may be in use.
) else (
    echo [OK] MinIO started on port 9000, Console on 9001
    echo     Data: %DATA_DIR%\minio
)

echo.
echo [8/10] Waiting for Docker services...
timeout /t 15 /nobreak >nul
echo [OK] Docker services are ready

echo.
echo [9/10] Starting Ollama...
start "Ollama" cmd /k "ollama serve"
echo [OK] Ollama starting on port 11434...

echo.
echo [10/10] Starting Whisper...
start "Whisper" cmd /k "cd /d %~dp0 && python whisper_server.py"
echo [OK] Whisper starting on port 5000...

echo.
echo ========================================
echo     Starting Backend and Frontend
echo ========================================
start "Backend" cmd /k "cd /d %~dp0backend && mvn spring-boot:run -DskipTests"
echo [OK] Backend starting...

start "Frontend" cmd /k "cd /d %~dp0frontend && npm run dev"
echo [OK] Frontend starting...

echo.
echo ========================================
echo     All services starting...
echo ========================================
echo.
echo Service URLs:
echo   - Frontend: http://localhost:5173
echo   - Backend: http://localhost:8080
echo   - MinIO Console: http://localhost:9001
echo   - Chroma: http://localhost:8000
echo.
echo Docker Services:
echo   - MySQL: localhost:3307 (password: 1234)
echo   - Redis: localhost:6380 (password: 12345)
echo.
echo Other Services:
echo   - Ollama: localhost:11434
echo   - Whisper: localhost:5000
echo.
echo Data directories:
echo   %DATA_DIR%
echo   - mysql/   (MySQL data)
echo   - redis/   (Redis data)
echo   - chroma/  (Chroma vector data)
echo   - minio/   (MinIO object storage)
echo ========================================
pause
