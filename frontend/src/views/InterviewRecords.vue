<template>
  <div class="interview-records-container">
    <div class="page-header">
      <h1>面试记录</h1>
      <p>查看和管理您的历史面试记录</p>
    </div>

    <div class="records-list" v-if="records.length > 0">
      <div v-for="record in records" :key="record.id" class="record-card">
        <div class="record-header">
          <div class="record-info">
            <h3>{{ record.position }}</h3>
            <span class="record-status" :class="record.status.toLowerCase()">
              {{ getStatusText(record.status) }}
            </span>
          </div>
          <span class="record-date">{{ formatDate(record.createdAt) }}</span>
        </div>
        <div class="record-meta">
          <span class="meta-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <path d="M12 6v6l4 2"></path>
            </svg>
            {{ record.createdAt ? formatTime(record.createdAt) : '--' }}
          </span>
        </div>
        <div class="record-actions">
          <button class="btn btn-primary" @click="viewDetail(record)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
              <circle cx="12" cy="12" r="3"></circle>
            </svg>
            查看详情
          </button>
          <button v-if="record.status === 'COMPLETED'" class="btn btn-warning" @click="viewEvaluation(record)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="20" x2="18" y2="10"></line>
              <line x1="12" y1="20" x2="12" y2="4"></line>
              <line x1="6" y1="20" x2="6" y2="14"></line>
            </svg>
            查看评估
          </button>
          <button class="btn btn-danger-outline" @click="deleteRecord(record.id)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
            </svg>
            删除
          </button>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
        </svg>
      </div>
      <h3>暂无面试记录</h3>
      <p>开始您的第一次AI模拟面试吧</p>
      <button class="btn btn-primary btn-lg" @click="goToStart">
        立即开始
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'InterviewRecords',
  data() {
    return {
      records: []
    }
  },
  mounted() {
    this.fetchRecords()
  },
  methods: {
    async fetchRecords() {
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
          this.records = data.data
        }
      } catch (err) {
        console.error('获取面试记录失败:', err)
      }
    },
    getStatusText(status) {
      const statusMap = {
        'ACTIVE': '进行中',
        'COMPLETED': '已完成',
        'EXPIRED': '已结束'
      }
      return statusMap[status] || status
    },
    formatDate(dateStr) {
      if (!dateStr) return '--'
      const date = new Date(dateStr)
      return date.toLocaleDateString('zh-CN')
    },
    formatTime(dateStr) {
      if (!dateStr) return '--'
      const date = new Date(dateStr)
      return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    },
    viewDetail(record) {
      this.$router.push({
        name: 'InterviewMessages',
        params: {
          sessionId: record.id,
          position: record.position
        }
      })
    },
    async viewEvaluation(record) {
      const token = localStorage.getItem('token')
      if (!token) {
        this.$router.push('/login')
        return
      }

      try {
        const response = await fetch(`http://localhost:8080/api/interview/evaluation/${record.id}`, {
          headers: { Authorization: `Bearer ${token}` }
        })
        const data = await response.json()
        if (data.success) {
          sessionStorage.setItem('evaluationReport', JSON.stringify(data.data))
          this.$router.push({ name: 'EvaluationReport', query: { sessionId: record.id } })
        } else {
          alert('该面试暂无评估报告')
        }
      } catch (err) {
        console.error('获取评估报告失败:', err)
        alert('获取评估报告失败')
      }
    },
    deleteRecord(id) {
      if (confirm('确定要删除这条面试记录吗？')) {
        this.performDelete(id)
      }
    },
    async performDelete(id) {
      const token = localStorage.getItem('token')
      if (!token) {
        this.$router.push('/login')
        return
      }

      try {
        const response = await fetch(`http://localhost:8080/api/interview/session/${id}`, {
          method: 'DELETE',
          headers: { Authorization: `Bearer ${token}` }
        })
        const data = await response.json()
        if (data.success) {
          this.records = this.records.filter(r => r.id !== id)
        } else {
          alert('删除失败: ' + data.message)
        }
      } catch (err) {
        console.error('删除面试记录失败:', err)
        alert('删除失败，请稍后重试')
      }
    },
    goToStart() {
      this.$router.push('/interview/start')
    }
  }
}
</script>

<style scoped>
.interview-records-container {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-color);
  margin-bottom: 8px;
}

.page-header p {
  font-size: 1rem;
  color: var(--text-secondary);
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.record-card {
  background: var(--surface-color);
  border-radius: var(--radius-xl);
  padding: 24px;
  border: 1px solid var(--border-color);
  transition: all 0.3s;
}

.record-card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--primary-color);
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.record-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.record-info h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-color);
}

.record-status {
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  font-weight: 500;
}

.record-status.active {
  background: rgba(16, 185, 129, 0.1);
  color: var(--success-color);
}

.record-status.completed {
  background: var(--primary-light);
  color: var(--primary-color);
}

.record-status.expired {
  background: var(--background-color);
  color: var(--text-muted);
}

.record-date {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.record-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.meta-item svg {
  width: 16px;
  height: 16px;
}

.record-actions {
  display: flex;
  gap: 12px;
  border-top: 1px solid var(--border-color);
  padding-top: 16px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 20px;
  font-size: 0.875rem;
  font-weight: 500;
  border-radius: var(--radius-md);
  transition: all 0.2s;
  cursor: pointer;
  border: none;
}

.btn svg {
  width: 16px;
  height: 16px;
}

.btn-primary {
  background: var(--primary-color);
  color: white;
}

.btn-primary:hover {
  background: var(--primary-hover);
}

.btn-warning {
  background: linear-gradient(135deg, #F59E0B, #D97706);
  color: white;
}

.btn-warning:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

.btn-danger-outline {
  background: white;
  color: var(--error-color);
  border: 1px solid var(--error-color);
}

.btn-danger-outline:hover {
  background: var(--error-color);
  color: white;
}

.btn-lg {
  padding: 14px 28px;
  font-size: 1rem;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: var(--surface-color);
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-color);
}

.empty-icon {
  width: 80px;
  height: 80px;
  background: var(--primary-light);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.empty-icon svg {
  width: 40px;
  height: 40px;
  color: var(--primary-color);
}

.empty-state h3 {
  font-size: 1.25rem;
  color: var(--text-color);
  margin-bottom: 8px;
}

.empty-state p {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin-bottom: 24px;
}

@media (max-width: 768px) {
  .record-header {
    flex-direction: column;
    gap: 8px;
  }

  .record-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .record-actions {
    flex-wrap: wrap;
  }

  .btn {
    padding: 8px 16px;
    font-size: 0.8125rem;
  }
}
</style>
