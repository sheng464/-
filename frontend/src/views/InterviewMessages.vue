<template>
  <div class="messages-container">
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="19" y1="12" x2="5" y2="12"></line>
          <polyline points="12 19 5 12 12 5"></polyline>
        </svg>
        返回
      </button>
      <div class="header-info">
        <h1>{{ position }} - 面试消息记录</h1>
        <div class="session-meta">
          <span class="meta-item">面试时间: {{ formatDateTime(sessionInfo.createdAt) }}</span>
          <span class="meta-divider">|</span>
          <span class="meta-item">问题数: {{ sessionInfo.questionCount }} / {{ sessionInfo.maxQuestions }}</span>
        </div>
      </div>
    </div>

    <div class="messages-list" v-if="messages.length > 0">
      <div v-for="(msg, index) in messages" :key="index" class="message-item" :class="msg.role">
        <div class="message-avatar">
          <svg v-if="msg.role === 'assistant'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <path d="M12 16v-4"></path>
            <path d="M12 8h.01"></path>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
        </div>
        <div class="message-body">
          <div class="message-label">{{ msg.role === 'assistant' ? 'AI 面试官' : '我' }}</div>
          <div class="message-content">{{ msg.content }}</div>
          <div class="message-time">{{ msg.time }}</div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
        </svg>
      </div>
      <h3>暂无消息记录</h3>
      <p>该面试会话暂无消息或尚未开始</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'InterviewMessages',
  props: {
    sessionId: String,
    position: String
  },
  data() {
    return {
      messages: [],
      sessionInfo: {
        createdAt: '',
        questionCount: 0,
        maxQuestions: 10,
        followupCount: 0,
        maxFollowups: 3
      }
    }
  },
  mounted() {
    this.fetchSessionInfo()
    this.fetchMessages()
  },
  methods: {
    async fetchSessionInfo() {
      const token = localStorage.getItem('token')
      if (!token) {
        this.$router.push('/login')
        return
      }

      try {
        const response = await fetch('http://localhost:8080/api/interview/sessions', {
          headers: { Authorization: `Bearer ${token}` }
        })
        const data = await response.json()
        if (data.success) {
          const session = data.data.find(s => s.id == this.sessionId)
          if (session) {
            this.sessionInfo = {
              createdAt: session.createdAt,
              questionCount: session.questionCount || 0,
              maxQuestions: session.maxQuestions || 10,
              followupCount: session.followupCount || 0,
              maxFollowups: session.maxFollowups || 3
            }
            if (session.position) {
              this.position = session.position
            }
          }
        }
      } catch (err) {
        console.error('获取会话信息失败:', err)
      }
    },
    async fetchMessages() {
      const token = localStorage.getItem('token')
      if (!token) {
        this.$router.push('/login')
        return
      }

      try {
        const response = await fetch(`http://localhost:8080/api/interview/history/${this.sessionId}`, {
          headers: { Authorization: `Bearer ${token}` }
        })
        const data = await response.json()
        if (data.success) {
          this.messages = data.data
        } else {
          console.error('获取消息失败:', data.message)
        }
      } catch (err) {
        console.error('获取消息失败:', err)
      }
    },
    formatDateTime(dateStr) {
      if (!dateStr) return '--'
      const date = new Date(dateStr)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    goBack() {
      this.$router.push('/interview/records')
    }
  }
}
</script>

<style scoped>
.messages-container {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  padding: 20px;
  background: white;
  border-radius: 15px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 15px;
  background: #f5f5f5;
  border: none;
  border-radius: 8px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
  flex-shrink: 0;
}

.back-btn:hover {
  background: #eee;
}

.back-btn svg {
  width: 18px;
  height: 18px;
}

.header-info {
  flex: 1;
}

.header-info h1 {
  font-size: 20px;
  color: #333;
  margin: 0 0 8px 0;
}

.session-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.meta-divider {
  color: #ddd;
}

.messages-list {
  background: white;
  border-radius: 15px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.message-item {
  display: flex;
  gap: 15px;
  margin-bottom: 25px;
}

.message-item:last-child {
  margin-bottom: 0;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 45px;
  height: 45px;
  min-width: 45px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-item.assistant .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.message-item.user .message-avatar {
  background: #4caf50;
}

.message-avatar svg {
  width: 22px;
  height: 22px;
  color: white;
}

.message-body {
  flex: 1;
  max-width: 80%;
}

.message-item.user .message-body {
  text-align: right;
}

.message-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
}

.message-content {
  padding: 14px 18px;
  border-radius: 15px;
  font-size: 15px;
  line-height: 1.6;
  word-break: break-word;
}

.message-item.assistant .message-content {
  background: #f5f5f5;
  color: #333;
  border-bottom-left-radius: 5px;
}

.message-item.user .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 5px;
  text-align: left;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 6px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.empty-icon {
  width: 80px;
  height: 80px;
  background: #f5f5f5;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.empty-icon svg {
  width: 40px;
  height: 40px;
  color: #ccc;
}

.empty-state h3 {
  font-size: 20px;
  color: #333;
  margin-bottom: 10px;
}

.empty-state p {
  font-size: 14px;
  color: #999;
}
</style>
