<template>
  <div class="evaluation-report-container">
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
        返回
      </button>
      <h1>📊 能力评估报告</h1>
      <p>{{ evaluation.position }} · {{ formatDate(evaluation.createdAt) }}</p>
    </div>

    <div v-if="isLoading" class="loading-overlay">
      <div class="loading-content">
        <div class="loading-spinner"></div>
        <h3>正在生成评估报告</h3>
        <p>AI正在分析您的面试表现，请稍候...</p>
      </div>
    </div>

    <div class="report-content" v-else>
      <div class="overall-section">
        <div class="score-circle">
          <svg viewBox="0 0 120 120">
            <circle cx="60" cy="60" r="54" fill="none" stroke="#e0e0e0" stroke-width="12"/>
            <circle cx="60" cy="60" r="54" fill="none" :stroke="getScoreColor(evaluation.overallScore)"
              stroke-width="12" stroke-linecap="round"
              :stroke-dasharray="getCircleDash(evaluation.overallScore)"
              transform="rotate(-90 60 60)"/>
          </svg>
          <div class="score-text">
            <span class="score-value">{{ evaluation.overallScore || 0 }}</span>
            <span class="score-label">综合评分</span>
          </div>
        </div>
        <div class="grade-badge" :class="getGradeClass(evaluation.overallGrade)">
          {{ evaluation.overallGrade }}
        </div>
      </div>

      <div class="dimensions-section">
        <h2>能力维度分析</h2>
        <div class="dimensions-grid">
          <div v-for="(dim, key) in commonDimensions" :key="key" class="dimension-card">
            <div class="dimension-header">
              <span class="dimension-name">{{ getDimensionName(key) }}</span>
              <span class="dimension-grade" :class="getGradeClass(dim.grade)">{{ dim.grade }}</span>
            </div>
            <div class="dimension-score-bar">
              <div class="bar-bg">
                <div class="bar-fill" :style="{width: dim.score + '%', background: getScoreColor(dim.score)}"></div>
              </div>
              <span class="score-num">{{ dim.score }}</span>
            </div>
            <p v-if="dim.detail" class="dimension-detail">{{ dim.detail }}</p>
          </div>
        </div>
      </div>

      <div v-if="isVoiceInterview && voiceDimensions && voiceDimensionsCount > 0" class="voice-dimensions-section">
        <h2>🎤 语音表达能力分析</h2>
        <div class="voice-dimensions-grid">
          <div v-for="(dim, key) in voiceDimensions" :key="key" class="dimension-card voice-card">
            <div class="dimension-header">
              <span class="dimension-name">{{ getDimensionName(key) }}</span>
              <span class="dimension-grade" :class="getGradeClass(dim.grade)">{{ dim.grade }}</span>
            </div>
            <div class="dimension-score-bar">
              <div class="bar-bg">
                <div class="bar-fill" :style="{width: dim.score + '%', background: getScoreColor(dim.score)}"></div>
              </div>
              <span class="score-num">{{ dim.score }}</span>
            </div>
            <p v-if="dim.detail" class="dimension-detail">{{ dim.detail }}</p>
          </div>
        </div>
      </div>

      <div class="analysis-section">
        <div class="analysis-card strengths">
          <h3>✨ 优势亮点</h3>
          <p>{{ evaluation.strengths || '暂无亮点分析' }}</p>
        </div>
        <div class="analysis-card weaknesses">
          <h3>📝 不足之处</h3>
          <p>{{ evaluation.weaknesses || '暂无不足分析' }}</p>
        </div>
      </div>

      <div class="suggestions-section">
        <h2>💡 改进建议</h2>
        <div class="suggestions-card">
          <p>{{ evaluation.improvementSuggestions || '暂无改进建议' }}</p>
        </div>
      </div>

      <div v-if="fullAnalysisData" class="full-analysis-section">
        <h2>📋 完整分析报告</h2>
        <div class="analysis-content">
          <p>{{ fullAnalysisData }}</p>
        </div>
      </div>

      <div class="interview-stats">
        <div class="stat-item">
          <span class="stat-label">面试类型</span>
          <span class="stat-value">{{ evaluation.sessionType === 'TEXT' ? '文字面试' : '语音面试' }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">回答问题数</span>
          <span class="stat-value">{{ evaluation.questionCount }} 个</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">追问次数</span>
          <span class="stat-value">{{ evaluation.followupCount }} 次</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'EvaluationReport',
  data() {
    return {
      evaluation: {},
      isLoading: false,
      evaluationPolling: null
    }
  },
  computed: {
    fullAnalysisData() {
      if (!this.evaluation.fullAnalysis) return null
      try {
        const json = JSON.parse(this.evaluation.fullAnalysis)
        return json.fullAnalysis || json.FinalSummary || this.evaluation.fullAnalysis
      } catch {
        return this.evaluation.fullAnalysis
      }
    },
    commonDimensions() {
      if (!this.evaluation.dimensions) return {}
      const voiceKeys = ['speechRate', 'clarity', 'confidence']
      const result = {}
      for (const key in this.evaluation.dimensions) {
        if (!voiceKeys.includes(key)) {
          result[key] = this.evaluation.dimensions[key]
        }
      }
      return result
    },
    voiceDimensions() {
      if (!this.evaluation.dimensions) return null
      const voiceKeys = ['speechRate', 'clarity', 'confidence']
      const result = {}
      let count = 0
      for (const key in this.evaluation.dimensions) {
        if (voiceKeys.includes(key)) {
          result[key] = this.evaluation.dimensions[key]
          count++
        }
      }
      return count > 0 ? result : null
    },
    voiceDimensionsCount() {
      if (!this.voiceDimensions) return 0
      return Object.keys(this.voiceDimensions).length
    },
    isVoiceInterview() {
      return this.evaluation.sessionType === 'VOICE'
    }
  },
  mounted() {
    const stored = sessionStorage.getItem('evaluationReport')
    if (stored) {
      this.evaluation = JSON.parse(stored)
    } else if (this.$route.params.evaluation) {
      this.evaluation = this.$route.params.evaluation
    } else if (this.$route.query.sessionId) {
      this.fetchEvaluationWithPolling(this.$route.query.sessionId)
    }
  },
  beforeDestroy() {
    if (this.evaluationPolling) {
      clearInterval(this.evaluationPolling)
    }
  },
  methods: {
    async fetchEvaluationWithPolling(sessionId) {
      this.isLoading = true
      this.fetchEvaluation(sessionId)

      this.evaluationPolling = setInterval(async () => {
        const token = localStorage.getItem('token')
        if (!token) return

        try {
          const response = await fetch(`http://localhost:8080/api/interview/evaluation/${sessionId}`, {
            headers: { Authorization: `Bearer ${token}` }
          })
          const data = await response.json()
          if (data.success && data.data && Object.keys(data.data).length > 0) {
            this.evaluation = data.data
            this.isLoading = false
            if (this.evaluationPolling) {
              clearInterval(this.evaluationPolling)
              this.evaluationPolling = null
            }
          }
        } catch (err) {
          console.error('轮询评估报告失败:', err)
        }
      }, 2000)
    },
    async fetchEvaluation(sessionId) {
      const token = localStorage.getItem('token')
      if (!token) {
        this.$router.push('/login')
        return
      }

      try {
        const response = await fetch(`http://localhost:8080/api/interview/evaluation/${sessionId}`, {
          headers: { Authorization: `Bearer ${token}` }
        })
        const data = await response.json()
        if (data.success) {
          this.evaluation = data.data
        }
      } catch (err) {
        console.error('获取评估报告失败:', err)
      }
    },
    goBack() {
      this.$router.back()
    },
    formatDate(dateStr) {
      if (!dateStr) return '--'
      return new Date(dateStr).toLocaleDateString('zh-CN')
    },
    getScoreColor(score) {
      if (score >= 90) return '#4caf50'
      if (score >= 75) return '#8bc34a'
      if (score >= 60) return '#ff9800'
      return '#f44336'
    },
    getGradeClass(grade) {
      if (grade === '优秀') return 'grade-excellent'
      if (grade === '良好') return 'grade-good'
      if (grade === '及格') return 'grade-pass'
      return 'grade-fail'
    },
    getCircleDash(score) {
      const circumference = 2 * Math.PI * 54
      const percent = (score || 0) / 100
      return `${percent * circumference} ${circumference}`
    },
    getDimensionName(key) {
      const names = {
        technicalCorrectness: '技术正确性',
        knowledgeDepth: '知识深度',
        logicRigorous: '逻辑严谨性',
        speechRate: '语速评分',
        clarity: '表达清晰度',
        confidence: '自信程度'
      }
      return names[key] || key
    }
  }
}
</script>

<style scoped>
.evaluation-report-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255,255,255,0.1);
  border: none;
  color: white;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 20px;
}

.back-btn svg {
  width: 20px;
  height: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: white;
  margin-bottom: 8px;
}

.page-header p {
  font-size: 14px;
  color: rgba(255,255,255,0.7);
}

.report-content {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.overall-section {
  background: white;
  border-radius: 20px;
  padding: 30px;
  text-align: center;
}

.score-circle {
  position: relative;
  width: 150px;
  height: 150px;
  margin: 0 auto 15px;
}

.score-circle svg {
  width: 100%;
  height: 100%;
}

.score-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.score-value {
  display: block;
  font-size: 42px;
  font-weight: 700;
  color: #333;
}

.score-label {
  font-size: 12px;
  color: #999;
}

.grade-badge {
  display: inline-block;
  padding: 6px 24px;
  border-radius: 20px;
  font-size: 16px;
  font-weight: 600;
}

.grade-excellent { background: #e8f5e9; color: #4caf50; }
.grade-good { background: #f1f8e9; color: #8bc34a; }
.grade-pass { background: #fff3e0; color: #ff9800; }
.grade-fail { background: #ffebee; color: #f44336; }

.dimensions-section, .suggestions-section, .full-analysis-section {
  background: white;
  border-radius: 20px;
  padding: 25px;
}

.dimensions-section h2, .suggestions-section h2, .full-analysis-section h2 {
  font-size: 18px;
  color: #333;
  margin-bottom: 20px;
}

.dimensions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 15px;
}

.dimension-card {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 15px;
}

.dimension-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.dimension-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.dimension-grade {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
}

.dimension-score-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bar-bg {
  flex: 1;
  height: 8px;
  background: #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.score-num {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  min-width: 30px;
}

.dimension-detail {
  font-size: 12px;
  color: #666;
  margin-top: 8px;
  line-height: 1.5;
}

.analysis-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.analysis-card {
  background: white;
  border-radius: 20px;
  padding: 20px;
}

.analysis-card h3 {
  font-size: 16px;
  margin-bottom: 12px;
}

.strengths h3 { color: #4caf50; }
.weaknesses h3 { color: #f44336; }

.analysis-card p {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.suggestions-card {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 15px;
}

.suggestions-card p {
  font-size: 14px;
  color: #666;
  line-height: 1.8;
}

.analysis-content {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 15px;
}

.analysis-content p {
  font-size: 14px;
  color: #666;
  line-height: 1.8;
  white-space: pre-wrap;
}

.voice-dimensions-section {
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  border-radius: 20px;
  padding: 25px;
  border: 2px solid #e8eaff;
}

.voice-dimensions-section h2 {
  font-size: 18px;
  color: #333;
  margin-bottom: 20px;
}

.voice-dimensions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.voice-card {
  background: white;
  border: 1px solid #e8eaff;
}

.interview-stats {
  display: flex;
  justify-content: center;
  gap: 30px;
  background: white;
  border-radius: 20px;
  padding: 20px;
}

.stat-item {
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.loading-content {
  background: white;
  padding: 40px 50px;
  border-radius: 20px;
  text-align: center;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-content h3 {
  font-size: 20px;
  color: #333;
  margin-bottom: 10px;
}

.loading-content p {
  font-size: 14px;
  color: #666;
}
</style>
