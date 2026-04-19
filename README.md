# AI 面试系统

一个基于 AI 的面试系统，支持文本面试和语音面试。

## 功能特性

- 支持简历上传和岗位匹配
- AI 智能面试提问
- 技术基础知识面试（八股文）
- 项目经验面试
- 语音识别面试
- 面试结果评估

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+
- Docker (用于 Ollama、Whisper、Chroma)
- ffmpeg (用于音频处理)

## 快速启动

### 方式一：一键启动（推荐）

```bash
# Windows
./start-all.bat

# Linux/Mac
chmod +x start-all.sh
./start-all.sh
```

### 方式二：手动启动

#### 1. 启动基础服务（Docker）

```bash
# MySQL
docker run -d --name mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=interview_db \
  mysql:8.0

# Redis
docker run -d --name redis \
  -p 6379:6379 \
  redis:7-alpine

# Chroma (向量数据库)
docker run -d --name chroma \
  -p 8000:8000 \
  chromadb/chroma

# MinIO (对象存储)
docker run -d --name minio \
  -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

#### 2. 启动 Ollama

```bash
# 安装并启动 Ollama
ollama serve

# 下载模型
ollama pull llama3.1:8b
```

#### 3. 启动 Whisper

```bash
# 安装依赖
pip install faster-whisper flask pydub librosa numpy

# 启动服务
python whisper_server.py
```

#### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

#### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

## 服务端口

| 服务 | 端口 | 地址 |
|------|------|------|
| 前端 | 5173 | http://localhost:5173 |
| 后端 | 8080 | http://localhost:8080 |
| MySQL | 3307 | localhost:3307 |
| Redis | 6380 | localhost:6380 |
| Chroma | 8000 | http://localhost:8000 |
| MinIO API | 9000 | http://localhost:9000 |
| MinIO Console | 9001 | http://localhost:9001 |
| Ollama | 11434 | http://localhost:11434 |
| Whisper | 5000 | http://localhost:5000 |

## 配置说明

后端配置文件：`backend/src/main/resources/application.yml`

主要配置项：
- 数据库连接信息
- Redis 连接信息
- MinIO 连接信息
- Ollama API 地址
- Whisper API 地址

## 技术栈

### 后端
- Spring Boot 3.x
- Spring Data JPA
- MySQL
- Redis
- MinIO
- RestClient

### 前端
- Vue 3
- Vite
- Axios
- Pinia

### AI 服务
- Ollama (本地 LLM)
- Whisper (语音识别)
- Chroma (向量数据库)
