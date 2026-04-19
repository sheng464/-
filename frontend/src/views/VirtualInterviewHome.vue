<template>
  <div class="interview-home-container">
    <div class="page-header">
      <h1>虚拟面试</h1>
      <p>选择合适的模块开始您的面试训练</p>
    </div>

    <div class="modules-grid">
      <div class="module-card primary" @click="goToStart">
        <div class="module-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="5 3 19 12 5 21 5 3"></polygon>
          </svg>
        </div>
        <div class="module-content">
          <h3>开始面试</h3>
          <p>选择岗位和类型，开始 AI 模拟面试</p>
        </div>
        <div class="module-arrow">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="5" y1="12" x2="19" y2="12"></line>
            <polyline points="12 5 19 12 12 19"></polyline>
          </svg>
        </div>
      </div>

      <div class="module-card" @click="goToRecords">
        <div class="module-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
          </svg>
        </div>
        <div class="module-content">
          <h3>面试记录</h3>
          <p>查看历史面试记录和对话详情</p>
        </div>
        <div class="module-arrow">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="5" y1="12" x2="19" y2="12"></line>
            <polyline points="12 5 19 12 12 19"></polyline>
          </svg>
        </div>
      </div>

      <div class="module-card" @click="goToAbility">
        <div class="module-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="20" x2="18" y2="10"></line>
            <line x1="12" y1="20" x2="12" y2="4"></line>
            <line x1="6" y1="20" x2="6" y2="14"></line>
          </svg>
        </div>
        <div class="module-content">
          <h3>能力分析</h3>
          <p>分析您的技能树和能力画像</p>
        </div>
        <div class="module-arrow">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="5" y1="12" x2="19" y2="12"></line>
            <polyline points="12 5 19 12 12 19"></polyline>
          </svg>
        </div>
      </div>

      <div class="module-card" @click="goToKnowledge">
        <div class="module-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
          </svg>
        </div>
        <div class="module-content">
          <h3>知识库</h3>
          <p>系统化学习面试知识点</p>
        </div>
        <div class="module-arrow">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="5" y1="12" x2="19" y2="12"></line>
            <polyline points="12 5 19 12 12 19"></polyline>
          </svg>
        </div>
      </div>
    </div>

    <div class="quick-tips">
      <h2>面试小贴士</h2>
      <div class="tips-grid">
        <div class="tip-card">
          <div class="tip-icon">💡</div>
          <h4>提前准备</h4>
          <p>在面试前复习相关技术知识和项目经验</p>
        </div>
        <div class="tip-card">
          <div class="tip-icon">🎯</div>
          <h4>STAR 法则</h4>
          <p>使用 STAR 法则回答行为面试问题</p>
        </div>
        <div class="tip-card">
          <div class="tip-icon">📝</div>
          <h4>复盘总结</h4>
          <p>每次面试后查看评估报告，持续改进</p>
        </div>
        <div class="tip-card">
          <div class="tip-icon">⏱️</div>
          <h4>时间管理</h4>
          <p>注意把握答题节奏，不要过于紧张</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'VirtualInterviewHome',
  methods: {
    goToStart() {
      this.$router.push('/interview/start')
    },
    goToRecords() {
      this.$router.push('/interview/records')
    },
    goToAbility() {
      this.$router.push('/evaluation/history')
    },
    goToKnowledge() {
      this.$router.push('/knowledge')
    }
  }
}
</script>

<style scoped>
.interview-home-container {
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
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

.modules-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 48px;
}

.module-card {
  background: var(--surface-color);
  border-radius: var(--radius-xl);
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid var(--border-color);
}

.module-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary-color);
}

.module-card.primary {
  background: linear-gradient(135deg, var(--primary-color), #7C3AED);
  border: none;
}

.module-card.primary:hover {
  box-shadow: 0 12px 32px rgba(79, 70, 229, 0.3);
}

.module-card.primary .module-icon {
  background: rgba(255, 255, 255, 0.2);
}

.module-card.primary .module-icon svg {
  color: white;
}

.module-card.primary .module-content h3,
.module-card.primary .module-content p {
  color: white;
}

.module-card.primary .module-content p {
  color: rgba(255, 255, 255, 0.8);
}

.module-card.primary .module-arrow {
  background: rgba(255, 255, 255, 0.2);
}

.module-card.primary .module-arrow svg {
  color: white;
}

.module-icon {
  width: 56px;
  height: 56px;
  min-width: 56px;
  background: var(--primary-light);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.module-icon svg {
  width: 28px;
  height: 28px;
  color: var(--primary-color);
}

.module-content {
  flex: 1;
}

.module-content h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 4px;
}

.module-content p {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.module-arrow {
  width: 40px;
  height: 40px;
  min-width: 40px;
  background: var(--background-color);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.module-card:hover .module-arrow {
  background: var(--primary-color);
}

.module-card:hover .module-arrow svg {
  color: white;
}

.module-arrow svg {
  width: 20px;
  height: 20px;
  color: var(--text-muted);
}

.quick-tips {
  background: var(--surface-color);
  border-radius: var(--radius-xl);
  padding: 32px;
  border: 1px solid var(--border-color);
}

.quick-tips h2 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 24px;
}

.tips-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.tip-card {
  text-align: center;
  padding: 20px;
  background: var(--background-color);
  border-radius: var(--radius-lg);
  transition: all 0.3s;
}

.tip-card:hover {
  background: var(--primary-light);
}

.tip-icon {
  font-size: 32px;
  margin-bottom: 12px;
}

.tip-card h4 {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 8px;
}

.tip-card p {
  font-size: 0.8125rem;
  color: var(--text-secondary);
  line-height: 1.5;
}

@media (max-width: 1024px) {
  .tips-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .modules-grid {
    grid-template-columns: 1fr;
  }

  .tips-grid {
    grid-template-columns: 1fr;
  }

  .module-card {
    padding: 20px;
  }
}
</style>
