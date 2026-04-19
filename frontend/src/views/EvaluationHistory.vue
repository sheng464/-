<template>
  <div class="evaluation-history-container">
    <div class="page-header">
      <h1>📈 能力成长中心</h1>
      <p>追踪您的面试能力提升轨迹</p>
    </div>

    <div v-if="growthData && growthData.totalEvaluations > 0" class="growth-content">
      <div class="summary-cards">
        <div class="summary-card">
          <div class="summary-icon">📊</div>
          <div class="summary-info">
            <span class="summary-value">{{ growthData.totalEvaluations }}</span>
            <span class="summary-label">面试次数</span>
          </div>
        </div>
        <div class="summary-card" v-if="growthData.improvement">
          <div class="summary-icon">📈</div>
          <div class="summary-info">
            <span class="summary-value" :class="getChangeClass(growthData.improvement.overallChange)">
              {{ formatChange(growthData.improvement.overallChange) }}
            </span>
            <span class="summary-label">综合提升</span>
          </div>
        </div>
        <div class="summary-card" v-if="latestEvaluation">
          <div class="summary-icon">🏆</div>
          <div class="summary-info">
            <span class="summary-value">{{ latestEvaluation.overallScore || 0 }}</span>
            <span class="summary-label">最新评分</span>
          </div>
        </div>
      </div>

      <div class="chart-section">
        <h2>能力成长曲线</h2>
        <div class="chart-container">
          <canvas ref="growthChart"></canvas>
        </div>
        <div class="chart-legend">
          <button
            v-for="dim in activeDimensions"
            :key="dim.key"
            :class="['legend-btn', { active: selectedDimension === dim.key }]"
            @click="selectDimension(dim.key)"
          >
            {{ dim.label }}
          </button>
        </div>
      </div>

      <div class="improvement-section" v-if="growthData && growthData.totalEvaluations > 0">
        <h2>能力提升统计</h2>
        <p class="comparison-hint" v-if="growthData.comparisonInfo">
          早期{{ growthData.comparisonInfo.firstHalfCount }}次面试 → 近期{{ growthData.comparisonInfo.secondHalfCount }}次面试
        </p>
        <div class="improvement-grid">
          <div class="improvement-item">
            <span class="improvement-label">综合评分</span>
            <span class="improvement-value" :class="getChangeClass(growthData.improvement?.overallChange ?? 0)">
              {{ growthData.totalEvaluations >= 2 ? formatChange(growthData.improvement?.overallChange ?? 0) : '--' }}
            </span>
          </div>
          <div class="improvement-item">
            <span class="improvement-label">技术能力</span>
            <span class="improvement-value" :class="getChangeClass(growthData.improvement?.technicalChange ?? 0)">
              {{ growthData.totalEvaluations >= 2 ? formatChange(growthData.improvement?.technicalChange ?? 0) : '--' }}
            </span>
          </div>
          <div class="improvement-item">
            <span class="improvement-label">知识深度</span>
            <span class="improvement-value" :class="getChangeClass(growthData.improvement?.knowledgeChange ?? 0)">
              {{ growthData.totalEvaluations >= 2 ? formatChange(growthData.improvement?.knowledgeChange ?? 0) : '--' }}
            </span>
          </div>
          <div class="improvement-item">
            <span class="improvement-label">逻辑能力</span>
            <span class="improvement-value" :class="getChangeClass(growthData.improvement?.logicChange ?? 0)">
              {{ growthData.totalEvaluations >= 2 ? formatChange(growthData.improvement?.logicChange ?? 0) : '--' }}
            </span>
          </div>
        </div>
        <p v-if="growthData.totalEvaluations === 1" class="improvement-hint">完成更多面试后可查看能力提升趋势</p>
      </div>

      <div class="voice-improvement-section" v-if="growthData && growthData.hasVoiceData">
        <h2>🎤 语音能力提升</h2>
        <p class="comparison-hint" v-if="growthData.comparisonInfo">
          早期{{ growthData.comparisonInfo.firstHalfCount }}次面试 → 近期{{ growthData.comparisonInfo.secondHalfCount }}次面试
        </p>
        <div class="improvement-grid">
          <div class="improvement-item">
            <span class="improvement-label">语速表达</span>
            <span class="improvement-value" :class="getChangeClass(growthData.voiceImprovement?.speechRateChange ?? 0)">
              {{ growthData.totalEvaluations >= 2 ? formatChange(growthData.voiceImprovement?.speechRateChange ?? 0) : '--' }}
            </span>
          </div>
          <div class="improvement-item">
            <span class="improvement-label">表达清晰</span>
            <span class="improvement-value" :class="getChangeClass(growthData.voiceImprovement?.clarityChange ?? 0)">
              {{ growthData.totalEvaluations >= 2 ? formatChange(growthData.voiceImprovement?.clarityChange ?? 0) : '--' }}
            </span>
          </div>
          <div class="improvement-item">
            <span class="improvement-label">自信程度</span>
            <span class="improvement-value" :class="getChangeClass(growthData.voiceImprovement?.confidenceChange ?? 0)">
              {{ growthData.totalEvaluations >= 2 ? formatChange(growthData.voiceImprovement?.confidenceChange ?? 0) : '--' }}
            </span>
          </div>
          <div class="improvement-item voice-avg">
            <span class="improvement-label">近期平均得分</span>
            <span class="improvement-value avg-score">
              {{ getVoiceAvgScore() }}
            </span>
          </div>
        </div>
      </div>

      <div class="history-section">
        <h2>历史评估记录</h2>
        <div class="history-list">
          <div
            v-for="item in historyList"
            :key="item.evaluationId"
            class="history-card"
            @click="viewReport(item)"
          >
            <div class="history-main">
              <div class="history-position">{{ item.position }}</div>
              <div class="history-meta">
                <span class="history-type">{{ item.sessionType === 'TEXT' ? '文字' : '语音' }}</span>
                <span class="history-date">{{ formatDate(item.createdAt) }}</span>
              </div>
            </div>
            <div class="history-score">
              <span class="score-value">{{ item.overallScore || 0 }}</span>
              <span class="score-label">分</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">📊</div>
      <h3>暂无评估数据</h3>
      <p>完成面试后即可查看您的能力成长曲线</p>
      <button class="start-btn" @click="goToInterview">开始面试</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'EvaluationHistory',
  data() {
    return {
      historyList: [],
      growthData: {},
      latestEvaluation: null,
      selectedDimension: 'overallTrend',
      chartInstance: null,
      activeDimensions: []
    }
  },
  mounted() {
    this.fetchData()
  },
  beforeDestroy() {
    if (this.chartInstance) {
      this.chartInstance.destroy()
    }
  },
  methods: {
    async fetchData() {
      const token = localStorage.getItem('token')
      if (!token) {
        this.$router.push('/login')
        return
      }

      try {
        const [historyRes, growthRes] = await Promise.all([
          fetch('http://localhost:8080/api/interview/evaluation/history', {
            headers: { Authorization: `Bearer ${token}` }
          }),
          fetch('http://localhost:8080/api/interview/evaluation/growth', {
            headers: { Authorization: `Bearer ${token}` }
          })
        ])

        const historyData = await historyRes.json()
        const growthData = await growthRes.json()

        console.log('API Response - history:', historyData)
        console.log('API Response - growth:', growthData)

        if (historyData.success) {
          this.historyList = historyData.data
          this.latestEvaluation = historyData.data[0]
        }

        if (growthData.success) {
          this.growthData = growthData.data
          console.log('Setting growthData:', this.growthData)
          console.log('Improvement data:', JSON.stringify(this.growthData.improvement, null, 2))
          console.log('Voice Improvement data:', JSON.stringify(this.growthData.voiceImprovement, null, 2))
          console.log('Comparison info:', JSON.stringify(this.growthData.comparisonInfo, null, 2))
          this.setupDimensions()
          this.$nextTick(() => {
            this.initChart()
          })
        }
      } catch (err) {
        console.error('获取数据失败:', err)
      }
    },
    setupDimensions() {
      this.activeDimensions = [
        { key: 'overallTrend', label: '综合评分', data: 'overallTrend' }
      ]

      if (this.growthData.technicalTrend?.length > 0) {
        this.activeDimensions.push({ key: 'technicalTrend', label: '技术能力', data: 'technicalTrend' })
      }
      if (this.growthData.knowledgeTrend?.length > 0) {
        this.activeDimensions.push({ key: 'knowledgeTrend', label: '知识深度', data: 'knowledgeTrend' })
      }
      if (this.growthData.logicTrend?.length > 0) {
        this.activeDimensions.push({ key: 'logicTrend', label: '逻辑能力', data: 'logicTrend' })
      }
      if (this.growthData.speechTrend?.length > 0) {
        this.activeDimensions.push({ key: 'speechTrend', label: '语速表达', data: 'speechTrend' })
      }
      if (this.growthData.clarityTrend?.length > 0) {
        this.activeDimensions.push({ key: 'clarityTrend', label: '表达清晰', data: 'clarityTrend' })
      }
      if (this.growthData.confidenceTrend?.length > 0) {
        this.activeDimensions.push({ key: 'confidenceTrend', label: '自信程度', data: 'confidenceTrend' })
      }
    },
    initChart() {
      console.log('Initializing chart with growthData:', this.growthData)
      console.log('Total evaluations:', this.growthData?.totalEvaluations)
      console.log('Overall trend length:', this.growthData?.overallTrend?.length)

      if (!this.$refs.growthChart || this.growthData.totalEvaluations === 0) {
        console.log('Chart init skipped: no refs or no evaluations')
        return
      }

      const canvas = this.$refs.growthChart
      const ctx = canvas.getContext('2d')

      if (this.chartInstance) {
        this.chartInstance.destroy()
      }

      const dimension = this.activeDimensions.find(d => d.key === this.selectedDimension)
      console.log('Selected dimension:', dimension)
      if (!dimension) return

      const trendData = this.growthData[dimension.data] || []
      console.log('Trend data:', trendData)
      if (trendData.length === 0) return

      const labels = trendData.map(p => p.date)
      const scores = trendData.map(p => p.score)

      canvas.width = canvas.parentElement.offsetWidth
      canvas.height = 300

      this.chartInstance = {
        data: trendData,
        labels,
        scores,
        selectedDimension: this.selectedDimension
      }

      this.drawChart()
    },
    drawChart() {
      if (!this.chartInstance) return

      const canvas = this.$refs.growthChart
      const ctx = canvas.getContext('2d')
      const { labels, scores, data } = this.chartInstance

      ctx.clearRect(0, 0, canvas.width, canvas.height)

      const padding = { top: 20, right: 20, bottom: 40, left: 50 }
      const chartWidth = canvas.width - padding.left - padding.right
      const chartHeight = canvas.height - padding.top - padding.bottom

      ctx.strokeStyle = '#e0e0e0'
      ctx.lineWidth = 1

      for (let i = 0; i <= 4; i++) {
        const y = padding.top + (chartHeight / 4) * i
        ctx.beginPath()
        ctx.moveTo(padding.left, y)
        ctx.lineTo(canvas.width - padding.right, y)
        ctx.stroke()

        ctx.fillStyle = '#999'
        ctx.font = '12px sans-serif'
        ctx.textAlign = 'right'
        ctx.fillText((100 - i * 25).toString(), padding.left - 10, y + 4)
      }

      const stepX = chartWidth / Math.max(labels.length - 1, 1)

      ctx.fillStyle = '#999'
      ctx.font = '11px sans-serif'
      ctx.textAlign = 'center'

      labels.forEach((label, i) => {
        const x = padding.left + stepX * i
        ctx.fillText(label.substring(5), x, canvas.height - 10)
      })

      const gradient = ctx.createLinearGradient(0, padding.top, 0, padding.top + chartHeight)
      gradient.addColorStop(0, 'rgba(102, 126, 234, 0.3)')
      gradient.addColorStop(1, 'rgba(102, 126, 234, 0.01)')

      ctx.beginPath()
      ctx.moveTo(padding.left, padding.top + chartHeight)
      scores.forEach((score, i) => {
        const x = padding.left + stepX * i
        const y = padding.top + chartHeight - (score / 100) * chartHeight
        if (i === 0) {
          ctx.lineTo(x, y)
        } else {
          ctx.lineTo(x, y)
        }
      })
      ctx.lineTo(padding.left + stepX * (scores.length - 1), padding.top + chartHeight)
      ctx.closePath()
      ctx.fillStyle = gradient
      ctx.fill()

      ctx.beginPath()
      ctx.strokeStyle = '#667eea'
      ctx.lineWidth = 3
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'

      scores.forEach((score, i) => {
        const x = padding.left + stepX * i
        const y = padding.top + chartHeight - (score / 100) * chartHeight
        if (i === 0) {
          ctx.moveTo(x, y)
        } else {
          ctx.lineTo(x, y)
        }
      })
      ctx.stroke()

      scores.forEach((score, i) => {
        const x = padding.left + stepX * i
        const y = padding.top + chartHeight - (score / 100) * chartHeight

        ctx.beginPath()
        ctx.arc(x, y, 6, 0, Math.PI * 2)
        ctx.fillStyle = '#667eea'
        ctx.fill()
        ctx.strokeStyle = 'white'
        ctx.lineWidth = 2
        ctx.stroke()
      })
    },
    selectDimension(key) {
      this.selectedDimension = key
      if (this.chartInstance) {
        this.chartInstance.selectedDimension = key
      }
      this.drawChart()
    },
    viewReport(item) {
      sessionStorage.setItem('evaluationReport', JSON.stringify(item))
      this.$router.push({
        name: 'EvaluationReport',
        query: { sessionId: item.sessionId }
      })
    },
    goToInterview() {
      this.$router.push('/interview/start')
    },
    formatDate(dateStr) {
      if (!dateStr) return '--'
      return new Date(dateStr).toLocaleDateString('zh-CN')
    },
    formatChange(change) {
      if (change === null || change === undefined) return '0'
      return change > 0 ? `+${change.toFixed(1)}` : change.toFixed(1)
    },
    getChangeClass(change) {
      if (change > 0) return 'positive'
      if (change < 0) return 'negative'
      return 'neutral'
    },
    getVoiceAvgScore() {
      if (!this.growthData.voiceStats) return '--'
      const speech = this.growthData.voiceStats.speechRateAvg || 0
      const clarity = this.growthData.voiceStats.clarityAvg || 0
      const confidence = this.growthData.voiceStats.confidenceAvg || 0
      const count = [speech, clarity, confidence].filter(v => v > 0).length
      if (count === 0) return '--'
      return ((speech + clarity + confidence) / count).toFixed(1)
    }
  }
}
</script>

<style scoped>
.evaluation-history-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
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

.summary-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
  margin-bottom: 25px;
}

.summary-card {
  background: white;
  border-radius: 15px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
}

.summary-icon {
  font-size: 32px;
}

.summary-info {
  display: flex;
  flex-direction: column;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: #333;
}

.summary-value.positive { color: #4caf50; }
.summary-value.negative { color: #f44336; }
.summary-value.neutral { color: #999; }

.summary-label {
  font-size: 12px;
  color: #999;
}

.chart-section, .improvement-section, .history-section {
  background: white;
  border-radius: 20px;
  padding: 25px;
  margin-bottom: 25px;
}

.chart-section h2, .improvement-section h2, .history-section h2 {
  font-size: 18px;
  color: #333;
  margin-bottom: 20px;
}

.chart-container {
  position: relative;
  height: 300px;
}

.chart-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 15px;
}

.legend-btn {
  padding: 6px 14px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.legend-btn.active {
  background: #667eea;
  color: white;
  border-color: #667eea;
}

.improvement-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.improvement-item {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.improvement-label {
  font-size: 14px;
  color: #666;
}

.improvement-value {
  font-size: 18px;
  font-weight: 600;
}

.improvement-value.positive { color: #4caf50; }
.improvement-value.negative { color: #f44336; }
.improvement-value.neutral { color: #999; }

.improvement-hint {
  text-align: center;
  color: #999;
  font-size: 13px;
  margin-top: 15px;
}

.comparison-hint {
  text-align: center;
  color: #667eea;
  font-size: 13px;
  margin-bottom: 15px;
  font-weight: 500;
}

.voice-improvement-section {
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  border-radius: 20px;
  padding: 25px;
  margin-bottom: 25px;
  border: 2px solid #e8eaff;
}

.voice-improvement-section h2 {
  font-size: 18px;
  color: #333;
  margin-bottom: 20px;
}

.voice-avg {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.voice-avg .improvement-label {
  color: white;
  opacity: 0.9;
}

.voice-avg .avg-score {
  color: white;
  font-size: 22px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8f9fa;
  border-radius: 12px;
  padding: 15px 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.history-card:hover {
  transform: translateX(5px);
  background: #f0f0f0;
}

.history-position {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 5px;
}

.history-meta {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: #999;
}

.history-score {
  text-align: right;
}

.history-score .score-value {
  font-size: 28px;
  font-weight: 700;
  color: #667eea;
}

.history-score .score-label {
  font-size: 12px;
  color: #999;
  margin-left: 2px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 20px;
}

.empty-icon {
  font-size: 60px;
  margin-bottom: 20px;
}

.empty-state h3 {
  font-size: 20px;
  color: #333;
  margin-bottom: 10px;
}

.empty-state p {
  font-size: 14px;
  color: #999;
  margin-bottom: 25px;
}

.start-btn {
  padding: 12px 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  cursor: pointer;
}
</style>
