@echo off
chcp 65001 >nul
echo ========================================
echo     AI 面试系统 - 停止所有服务
echo ========================================
echo.

:: 停止后端
echo [1/5] 停止后端服务...
taskkill /F /IM java.exe 2>nul
echo [OK] 后端已停止

:: 停止前端
echo.
echo [2/5] 停止前端服务...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173 ^| findstr LISTENING') do (
    taskkill /F /PID %%a 2>nul
)
echo [OK] 前端已停止

:: 停止 Docker 容器
echo.
echo [3/5] 停止 Docker 容器...
docker stop interview_mysql interview_redis interview_chroma interview_minio 2>nul
docker rm interview_mysql interview_redis interview_chroma interview_minio 2>nul
echo [OK] Docker 容器已停止

:: 停止 Whisper
echo.
echo [4/5] 停止 Whisper 服务...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5000 ^| findstr LISTENING') do (
    taskkill /F /PID %%a 2>nul
)
echo [OK] Whisper 已停止

:: 停止 Ollama
echo.
echo [5/5] 停止 Ollama 服务...
taskkill /F /IM ollama.exe 2>nul
echo [OK] Ollama 已停止

echo.
echo ========================================
echo     所有服务已停止
echo ========================================
pause
