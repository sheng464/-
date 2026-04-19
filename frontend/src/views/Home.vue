<template>
  <div class="home-container">
    <div class="hero-section">
      <div class="hero-content">
        <h1>欢迎使用 <span class="highlight">AI 智能面试系统</span></h1>
        <p>通过 AI 驱动的模拟面试，提升您的面试技巧，快速成长为您理想岗位的专业人才</p>
        <div class="hero-actions">
          <button class="btn btn-primary btn-lg" @click="goToInterview">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="5 3 19 12 5 21 5 3"></polygon>
            </svg>
            开始面试
          </button>
          <button class="btn btn-secondary btn-lg" @click="goToKnowledge">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
            </svg>
            学习知识
          </button>
        </div>
      </div>
    </div>

    <div class="features-section">
      <div class="feature-card" @click="goToInterview">
        <div class="feature-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="23 7 16 12 23 17 23 7"></polygon>
            <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
          </svg>
        </div>
        <h3>AI 模拟面试</h3>
        <p>与 AI 面试官进行真实场景模拟，获得即时反馈与评估</p>
      </div>

      <div class="feature-card" @click="goToRecords">
        <div class="feature-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
          </svg>
        </div>
        <h3>面试记录</h3>
        <p>查看历史面试记录，回顾每一场面试的表现与改进点</p>
      </div>

      <div class="feature-card" @click="goToGrowth">
        <div class="feature-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="20" x2="18" y2="10"></line>
            <line x1="12" y1="20" x2="12" y2="4"></line>
            <line x1="6" y1="20" x2="6" y2="14"></line>
          </svg>
        </div>
        <h3>能力成长</h3>
        <p>追踪您的能力提升曲线，可视化展示您的成长轨迹</p>
      </div>

      <div class="feature-card" @click="goToKnowledge">
        <div class="feature-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
          </svg>
        </div>
        <h3>知识库</h3>
        <p>系统化学习面试知识点，巩固您的技术基础</p>
      </div>
    </div>

    <div class="records-section" v-if="interviewRecords.length > 0">
      <div class="section-header">
        <h2>最近面试记录</h2>
        <button class="btn btn-secondary" @click="goToRecords">查看全部</button>
      </div>
      <div class="records-list">
        <div class="record-card" v-for="(record, index) in interviewRecords" :key="index">
          <div class="record-header">
            <span class="record-position">{{ record.position }}</span>
            <span class="record-rating" :class="getRatingClass(record.rating)">{{ record.rating }}</span>
          </div>
          <p class="record-explanation">{{ record.explanation }}</p>
          <div class="record-footer">
            <span class="record-duration">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <path d="M12 6v6l4 2"></path>
              </svg>
              {{ record.duration }}
            </span>
            <span class="record-date">{{ formatDate(record.date) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Home',
  data() {
    return {
      interviewRecords: []
    }
  },
  mounted() {
    this.loadInterviewRecords()
  },
  methods: {
    goToInterview() {
      this.$router.push('/interview/start')
    },
    goToRecords() {
      this.$router.push('/interview/records')
    },
    goToGrowth() {
      this.$router.push('/evaluation/history')
    },
    goToKnowledge() {
      this.$router.push('/knowledge')
    },
    loadInterviewRecords() {
      const records = JSON.parse(localStorage.getItem('interviewRecords') || '[]')
      this.interviewRecords = records.slice(0, 3)
    },
    getRatingClass(rating) {
      if (rating === 'S' || rating === 'A') return 'excellent'
      if (rating === 'B') return 'good'
      if (rating === 'C') return 'normal'
      return 'poor'
    },
    formatDate(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
    }
  }
}
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
}

.hero-section {
  background: linear-gradient(135deg, var(--primary-color) 0%, #7C3AED 100%);
  border-radius: var(--radius-xl);
  padding: 48px;
  margin-bottom: 40px;
  position: relative;
  overflow: hidden;
}

.hero-section::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
  border-radius: 50%;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 600px;
}

.hero-content h1 {
  font-size: 2.25rem;
  font-weight: 700;
  color: white;
  margin-bottom: 16px;
  line-height: 1.2;
}

.hero-content h1 .highlight {
  background: linear-gradient(135deg, #FDE68A 0%, #FCD34D 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-content p {
  font-size: 1.125rem;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 32px;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.btn-lg {
  padding: 14px 28px;
  font-size: 1rem;
}

.btn-primary.btn-lg {
  background: white;
  color: var(--primary-color);
  font-weight: 600;
}

.btn-primary.btn-lg:hover {
  background: var(--primary-light);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.btn-secondary.btn-lg {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.btn-secondary.btn-lg:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.5);
}

.hero-actions .btn svg {
  width: 20px;
  height: 20px;
}

.features-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 40px;
}

.feature-card {
  background: var(--surface-color);
  border-radius: var(--radius-xl);
  padding: 28px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid var(--border-color);
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary-color);
}

.feature-icon {
  width: 56px;
  height: 56px;
  background: var(--primary-light);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.feature-icon svg {
  width: 28px;
  height: 28px;
  color: var(--primary-color);
}

.feature-card h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 8px;
}

.feature-card p {
  font-size: 0.875rem;
  color: var(--text-secondary);
  line-height: 1.6;
}

.records-section {
  background: var(--surface-color);
  border-radius: var(--radius-xl);
  padding: 32px;
  border: 1px solid var(--border-color);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-header h2 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-color);
}

.records-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.record-card {
  background: var(--background-color);
  border-radius: var(--radius-lg);
  padding: 20px;
  transition: all 0.2s;
}

.record-card:hover {
  background: var(--primary-light);
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.record-position {
  font-weight: 600;
  color: var(--text-color);
}

.record-rating {
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  font-weight: 600;
}

.record-rating.excellent {
  background: linear-gradient(135deg, var(--secondary-color), #34D399);
  color: white;
}

.record-rating.good {
  background: linear-gradient(135deg, #60A5FA, #3B82F6);
  color: white;
}

.record-rating.normal {
  background: linear-gradient(135deg, #FBBF24, #F59E0B);
  color: white;
}

.record-rating.poor {
  background: linear-gradient(135deg, #F87171, #EF4444);
  color: white;
}

.record-explanation {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin-bottom: 16px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.record-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.75rem;
  color: var(--text-muted);
}

.record-duration {
  display: flex;
  align-items: center;
  gap: 4px;
}

.record-duration svg {
  width: 14px;
  height: 14px;
}

@media (max-width: 1024px) {
  .features-section {
    grid-template-columns: repeat(2, 1fr);
  }

  .records-list {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding: 32px 24px;
  }

  .hero-content h1 {
    font-size: 1.75rem;
  }

  .hero-actions {
    flex-direction: column;
  }

  .features-section {
    grid-template-columns: 1fr;
  }

  .records-list {
    grid-template-columns: 1fr;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
