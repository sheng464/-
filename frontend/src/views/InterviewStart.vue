<template>
  <div class="interview-start-container">
    <div class="page-header">
      <h1>🚀 开始面试</h1>
      <p>选择您的面试岗位类型并上传简历</p>
    </div>

    <div class="step-indicator">
      <div class="step" :class="{ active: step >= 1, completed: step > 1 }">
        <div class="step-number">1</div>
        <span>选择岗位</span>
      </div>
      <div class="step-line" :class="{ active: step > 1 }"></div>
      <div class="step" :class="{ active: step >= 1.5, completed: step > 1.5 }">
        <div class="step-number">2</div>
        <span>选择类型</span>
      </div>
      <div class="step-line" :class="{ active: step > 1.5 }"></div>
      <div class="step" :class="{ active: step >= 2, completed: step > 2 }">
        <div class="step-number">3</div>
        <span>上传简历</span>
      </div>
      <div class="step-line" :class="{ active: step > 2 }"></div>
      <div class="step" :class="{ active: step >= 3, completed: step > 3 }">
        <div class="step-number">4</div>
        <span>选择方式</span>
      </div>
      <div class="step-line" :class="{ active: step > 3 }"></div>
      <div class="step" :class="{ active: step >= 4 }">
        <div class="step-number">5</div>
        <span>开始面试</span>
      </div>
    </div>

    <div class="step-content" v-show="step === 1">
      <div class="section-card">
        <h2>选择面试岗位</h2>
        <div class="types-grid">
          <div class="type-card" :class="{ selected: selectedType === 'java_backend' }" @click="selectedType = 'java_backend'">
            <div class="type-icon">☕</div>
            <span>Java后端</span>
          </div>
          <div class="type-card" :class="{ selected: selectedType === 'web_frontend' }" @click="selectedType = 'web_frontend'">
            <div class="type-icon">🎨</div>
            <span>Web前端</span>
          </div>
        </div>
        <button class="next-btn" :disabled="!selectedType" @click="nextStep">
          下一步
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="5" y1="12" x2="19" y2="12"></line>
            <polyline points="12 5 19 12 12 19"></polyline>
          </svg>
        </button>
      </div>
    </div>

    <div class="step-content" v-show="step === 1.5">
      <div class="section-card">
        <h2>选择面试类型</h2>
        <p class="upload-tip">请选择您想要的面试练习类型</p>
        <div class="types-grid interview-type-grid">
          <div class="type-card large" :class="{ selected: interviewType === 'PROJECT' }" @click="interviewType = 'PROJECT'">
            <div class="type-icon">💼</div>
            <span class="type-title">项目深挖</span>
            <span class="type-desc">基于简历项目进行深度追问，考察实战经验</span>
          </div>
          <div class="type-card large" :class="{ selected: interviewType === 'TECH' }" @click="interviewType = 'TECH'">
            <div class="type-icon">📚</div>
            <span class="type-title">八股文</span>
            <span class="type-desc">考察技术基础知识，如Java、Spring、数据库等</span>
          </div>
        </div>
        <div class="button-group">
          <button class="back-btn" @click="prevStep">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12"></line>
              <polyline points="12 19 5 12 12 5"></polyline>
            </svg>
            上一步
          </button>
          <button class="next-btn" :disabled="!interviewType" @click="nextStep">
            下一步
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="5" y1="12" x2="19" y2="12"></line>
              <polyline points="12 5 19 12 12 19"></polyline>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <div class="step-content" v-show="step === 2">
      <div class="section-card">
        <h2>上传简历</h2>
        <p class="upload-tip">上传您的简历，AI将根据简历内容生成个性化面试问题</p>

        <div v-if="hasResume && !forceUpload" class="resume-options">
          <div class="resume-option-card existing" @click="useExistingResume">
            <div class="option-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
              </svg>
            </div>
            <div class="option-info">
              <span class="option-title">使用我的简历</span>
              <span class="option-desc">{{ existingResumeName }}</span>
            </div>
            <div class="option-arrow">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </div>
          </div>

          <div class="divider">
            <span>或</span>
          </div>

          <div class="resume-option-card upload" @click="forceUpload = true">
            <div class="option-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                <polyline points="17 8 12 3 7 8"></polyline>
                <line x1="12" y1="3" x2="12" y2="15"></line>
              </svg>
            </div>
            <div class="option-info">
              <span class="option-title">上传新简历</span>
              <span class="option-desc">支持 PDF、Word 格式</span>
            </div>
            <div class="option-arrow">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </div>
          </div>
        </div>

        <div v-else class="upload-area" @click="triggerUpload" @dragover.prevent @drop.prevent="handleDrop">
          <input type="file" ref="fileInput" @change="handleFileChange" accept=".pdf,.doc,.docx" style="display: none" />
          <div v-if="!resumeFile" class="upload-placeholder">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="17 8 12 3 7 8"></polyline>
              <line x1="12" y1="3" x2="12" y2="15"></line>
            </svg>
            <p>点击或拖拽文件到此处上传</p>
            <span class="upload-formats">支持 PDF、Word 格式</span>
          </div>
          <div v-else class="file-info">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
            </svg>
            <div class="file-details">
              <span class="file-name">{{ resumeFile.name }}</span>
              <span class="file-size">{{ formatFileSize(resumeFile.size) }}</span>
            </div>
            <button class="remove-btn" @click.stop="removeFile">×</button>
          </div>
        </div>

        <button v-if="forceUpload" class="back-option-btn" @click="forceUpload = false; resumeFile = null">
          ← 返回选择简历
        </button>

        <div class="button-group">
          <button class="back-btn" @click="prevStep">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12"></line>
              <polyline points="12 19 5 12 12 5"></polyline>
            </svg>
            上一步
          </button>
          <button class="next-btn" :disabled="!resumeFile" @click="nextStep">
            下一步
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="5" y1="12" x2="19" y2="12"></line>
              <polyline points="12 5 19 12 12 19"></polyline>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <div class="step-content" v-show="step === 3">
      <div class="section-card">
        <h2>选择面试方式</h2>
        <p class="upload-tip">请选择您偏好的面试方式</p>

        <div class="interview-mode-grid">
          <div class="mode-card" :class="{ selected: interviewMode === 'TEXT' }" @click="interviewMode = 'TEXT'">
            <div class="mode-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
              </svg>
            </div>
            <h3>文字面试</h3>
            <p>通过文字输入进行面试</p>
            <ul class="mode-features">
              <li>✓ 打字输入回答</li>
              <li>✓ 随时修改答案</li>
              <li>✓ 适合打字熟练者</li>
            </ul>
            <div class="mode-select-indicator" v-if="interviewMode === 'TEXT'">已选择</div>
          </div>

          <div class="mode-card" :class="{ selected: interviewMode === 'VOICE' }" @click="interviewMode = 'VOICE'">
            <div class="mode-icon voice">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
                <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
                <line x1="12" y1="19" x2="12" y2="23"></line>
                <line x1="8" y1="23" x2="16" y2="23"></line>
              </svg>
            </div>
            <h3>语音面试</h3>
            <p>通过语音输入进行面试</p>
            <ul class="mode-features">
              <li>✓ 语音实时转文字</li>
              <li>✓ 分析表达表现</li>
              <li>✓ 更真实的面试体验</li>
            </ul>
            <div class="mode-select-indicator" v-if="interviewMode === 'VOICE'">已选择</div>
          </div>
        </div>

        <div class="button-group">
          <button class="back-btn" @click="prevStep">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12"></line>
              <polyline points="12 19 5 12 12 5"></polyline>
            </svg>
            上一步
          </button>
          <button class="next-btn" :disabled="!interviewMode" @click="nextStep">
            下一步
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="5" y1="12" x2="19" y2="12"></line>
              <polyline points="12 5 19 12 12 19"></polyline>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <div class="step-content" v-show="step === 4">
      <div class="section-card">
        <div class="interview-ready">
          <div class="ready-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="23 7 16 12 23 17 23 7"></polygon>
              <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
            </svg>
          </div>
          <h2>面试即将开始</h2>
          <p>岗位类型: <strong>{{ getTypeName(selectedType) }}</strong></p>
          <p>面试类型: <strong>{{ interviewType === 'PROJECT' ? '项目深挖' : '八股文' }}</strong></p>
          <p>面试方式: <strong>{{ interviewMode === 'TEXT' ? '文字面试' : '语音面试' }}</strong></p>
          <p class="ready-tip" v-if="interviewMode === 'VOICE'">请确保您的麦克风正常工作</p>
          <p class="ready-tip" v-else>请确保您的网络连接稳定</p>
          <button class="start-btn" @click="beginInterview">
            进入面试
          </button>
        </div>
      </div>
    </div>

    <div v-if="isLoading" class="loading-overlay">
      <div class="loading-content">
        <div class="loading-spinner">
          <div class="spinner-ring"></div>
          <div class="spinner-ring"></div>
          <div class="spinner-ring"></div>
        </div>
        <h3>AI正在解析简历</h3>
        <p>请稍候，AI正在根据您的简历生成个性化面试问题...</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'InterviewStart',
  data() {
    return {
      step: 1,
      selectedType: '',
      resumeFile: null,
      hasResume: false,
      existingResumeName: '',
      existingResumeId: null,
      forceUpload: false,
      isLoading: false,
      interviewMode: 'TEXT',
      interviewType: 'PROJECT'
    }
  },
  mounted() {
    this.checkExistingResume()
  },
  methods: {
    checkExistingResume() {
      const token = localStorage.getItem('token')
      if (token) {
        fetch('http://localhost:8080/api/resume/info', {
          headers: { Authorization: `Bearer ${token}` }
        })
          .then(res => res.json())
          .then(data => {
            if (data.success && data.data) {
              this.hasResume = true
              this.existingResumeName = data.data.fileName || '已上传的简历'
              this.existingResumeId = data.data.id
            }
          })
          .catch(() => {})
      }
    },
    useExistingResume() {
      this.resumeFile = { name: this.existingResumeName, isExisting: true, resumeId: this.existingResumeId }
      this.step = 3
    },
    nextStep() {
      if (this.step === 1) {
        this.step = 1.5
      } else if (this.step === 1.5) {
        this.step = 2
      } else if (this.step === 2) {
        this.step = 3
      } else if (this.step === 3) {
        this.step = 4
      }
    },
    prevStep() {
      if (this.step === 1.5) {
        this.step = 1
      } else if (this.step === 2) {
        this.step = 1.5
      } else if (this.step === 3) {
        this.step = 2
      } else if (this.step === 4) {
        this.step = 3
      }
    },
    triggerUpload() {
      this.$refs.fileInput.click()
    },
    handleFileChange(event) {
      const file = event.target.files[0]
      if (file) {
        this.resumeFile = file
      }
    },
    handleDrop(event) {
      const file = event.dataTransfer.files[0]
      if (file && (file.type === 'application/pdf' || file.name.endsWith('.doc') || file.name.endsWith('.docx'))) {
        this.resumeFile = file
      }
    },
    removeFile() {
      this.resumeFile = null
      this.$refs.fileInput.value = ''
    },
    formatFileSize(size) {
      if (size < 1024) return size + ' B'
      if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
      return (size / (1024 * 1024)).toFixed(1) + ' MB'
    },
    getTypeName(type) {
      const names = {
        java_backend: 'Java后端',
        web_frontend: 'Web前端'
      }
      return names[type] || type
    },
    startInterview() {
      this.step = 3
    },
    async beginInterview() {
      const token = localStorage.getItem('token')
      if (!token) {
        alert('请先登录')
        return
      }

      const formData = new FormData()
      formData.append('position', this.selectedType)
      formData.append('sessionType', this.interviewMode)
      formData.append('interviewType', this.interviewType)

      if (this.resumeFile) {
        if (this.resumeFile.isExisting && this.resumeFile.resumeId) {
          formData.append('resumeId', this.resumeFile.resumeId)
        } else {
          formData.append('file', this.resumeFile)
        }
      }

      this.isLoading = true

      try {
        const response = await fetch('http://localhost:8080/api/interview/start', {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}` },
          body: formData
        })

        const data = await response.json()
        this.isLoading = false

        if (data.success) {
          const sessionId = data.data.sessionId
          const maxQuestions = data.data.maxQuestions || 10
          const maxFollowups = data.data.maxFollowups || 3
          const initialMessage = data.data.reply || ''
          const sessionType = this.interviewMode === 'TEXT' ? 'TEXT' : 'VOICE'
          this.$router.push({
            name: 'VirtualInterview',
            params: {
              sessionId: sessionId,
              position: this.getTypeName(this.selectedType),
              sessionType: sessionType
            },
            query: {
              maxQuestions: maxQuestions,
              maxFollowups: maxFollowups,
              initialMessage: initialMessage
            }
          })
        } else {
          alert(data.message || '面试启动失败')
        }
      } catch (err) {
        this.isLoading = false
        alert('面试启动失败: ' + err.message)
      }
    }
  }
}
</script>

<style scoped>
.interview-start-container {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 32px;
  font-weight: 700;
  color: var(--primary-color);
  margin-bottom: 10px;
}

.page-header p {
  font-size: 16px;
  color: var(--text-secondary);
}

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 40px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.step-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--border-color);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  transition: all 0.3s;
}

.step.active .step-number {
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  color: white;
}

.step.completed .step-number {
  background: var(--success-color);
  color: white;
}

.step span {
  font-size: 14px;
  color: var(--text-muted);
}

.step.active span {
  color: var(--primary-color);
  font-weight: 600;
}

.step-line {
  width: 80px;
  height: 3px;
  background: var(--border-color);
  margin: 0 10px;
  margin-bottom: 25px;
  transition: all 0.3s;
}

.step-line.active {
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
}

.section-card {
  background: var(--surface-color);
  border-radius: var(--radius-xl);
  padding: 40px;
  box-shadow: var(--shadow-lg);
}

.section-card h2 {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 20px;
  text-align: center;
}

.types-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-bottom: 30px;
}

.interview-type-grid {
  grid-template-columns: repeat(2, 1fr);
}

.type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px;
  border: 2px solid var(--border-color);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: all 0.3s;
}

.type-card.large {
  padding: 30px 20px;
  text-align: center;
}

.type-card.large .type-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.type-card.large .type-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color);
}

.type-card.large .type-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 5px;
}

.type-card:hover {
  border-color: var(--primary-color);
}

.type-card.selected {
  border-color: var(--primary-color);
  background: var(--primary-light);
}

.type-icon {
  font-size: 36px;
}

.type-card span {
  font-weight: 500;
  color: var(--text-color);
}

.next-btn, .start-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  color: white;
  border: none;
  border-radius: var(--radius-lg);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.next-btn:hover:not(:disabled), .start-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(79, 70, 229, 0.4);
}

.next-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.next-btn svg, .start-btn svg {
  width: 20px;
  height: 20px;
}

.upload-tip {
  text-align: center;
  color: var(--text-secondary);
  margin-bottom: 25px;
}

.resume-options {
  margin-bottom: 20px;
}

.resume-option-card {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 20px;
  border: 2px solid var(--border-color);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: all 0.3s;
}

.resume-option-card:hover {
  border-color: var(--primary-color);
  background: var(--primary-light);
}

.resume-option-card.existing {
  border-color: var(--success-color);
  background: rgba(16, 185, 129, 0.05);
}

.resume-option-card.existing:hover {
  border-color: #059669;
  background: rgba(16, 185, 129, 0.1);
}

.option-icon {
  width: 50px;
  height: 50px;
  background: var(--background-color);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.option-icon svg {
  width: 28px;
  height: 28px;
  color: var(--primary-color);
}

.resume-option-card.existing .option-icon {
  background: rgba(16, 185, 129, 0.1);
}

.resume-option-card.existing .option-icon svg {
  color: var(--success-color);
}

.option-info {
  flex: 1;
  text-align: left;
}

.option-title {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 4px;
}

.option-desc {
  font-size: 13px;
  color: var(--text-muted);
}

.option-arrow {
  font-size: 20px;
  color: var(--text-muted);
}

.option-arrow svg {
  width: 20px;
  height: 20px;
}

.divider {
  display: flex;
  align-items: center;
  margin: 15px 0;
}

.divider::before, .divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border-color);
}

.divider span {
  padding: 0 15px;
  color: var(--text-muted);
  font-size: 13px;
}

.back-option-btn {
  display: block;
  width: 100%;
  margin-top: 15px;
  padding: 12px;
  background: none;
  border: none;
  color: var(--primary-color);
  font-size: 14px;
  cursor: pointer;
  text-align: center;
}

.back-option-btn:hover {
  text-decoration: underline;
}

.upload-area {
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-xl);
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 25px;
}

.upload-area:hover {
  border-color: var(--primary-color);
  background: var(--primary-light);
}

.upload-placeholder svg {
  width: 60px;
  height: 60px;
  color: var(--text-muted);
  margin-bottom: 15px;
}

.upload-placeholder p {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.upload-placeholder span {
  font-size: 13px;
  color: var(--text-muted);
}

.file-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.file-info svg {
  width: 50px;
  height: 50px;
  color: var(--primary-color);
}

.file-details {
  flex: 1;
  text-align: left;
}

.file-name {
  display: block;
  font-weight: 600;
  color: var(--text-color);
}

.file-size {
  font-size: 13px;
  color: var(--text-muted);
}

.remove-btn {
  width: 30px;
  height: 30px;
  border: none;
  background: rgba(239, 68, 68, 0.1);
  color: var(--error-color);
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
}

.button-group {
  display: flex;
  gap: 15px;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex: 1;
  padding: 16px;
  background: var(--surface-color);
  border: 2px solid var(--border-color);
  border-radius: var(--radius-lg);
  font-size: 16px;
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.3s;
}

.back-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.back-btn svg {
  width: 20px;
  height: 20px;
}

.next-btn {
  flex: 2;
}

.mode-icon {
  width: 50px;
  height: 50px;
  background: var(--primary-light);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
}

.mode-icon svg {
  width: 24px;
  height: 24px;
  color: var(--primary-color);
}

.mode-icon.voice {
  background: rgba(124, 58, 237, 0.1);
}

.mode-icon.voice svg {
  color: #7C3AED;
}

.interview-ready {
  text-align: center;
}

.ready-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.ready-icon svg {
  width: 40px;
  height: 40px;
  color: white;
}

.interview-ready h2 {
  margin-bottom: 15px;
}

.interview-ready p {
  color: var(--text-secondary);
  margin-bottom: 10px;
}

.ready-tip {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 25px;
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
  backdrop-filter: blur(5px);
}

.loading-content {
  background: var(--surface-color);
  padding: 50px 60px;
  border-radius: var(--radius-xl);
  text-align: center;
  box-shadow: var(--shadow-xl);
  max-width: 400px;
}

.loading-spinner {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 30px;
}

.spinner-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border: 4px solid transparent;
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 1.2s linear infinite;
}

.spinner-ring:nth-child(2) {
  width: 70%;
  height: 70%;
  top: 15%;
  left: 15%;
  border-top-color: var(--secondary-color);
  animation-delay: -0.15s;
}

.spinner-ring:nth-child(3) {
  width: 40%;
  height: 40%;
  top: 30%;
  left: 30%;
  border-top-color: #F59E0B;
  animation-delay: -0.3s;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-content h3 {
  font-size: 22px;
  color: var(--text-color);
  margin-bottom: 12px;
  font-weight: 600;
}

.loading-content p {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

@media (max-width: 768px) {
  .types-grid {
    grid-template-columns: 1fr;
  }

  .button-group {
    flex-direction: column;
  }

  .back-btn, .next-btn {
    flex: 1;
  }
}
</style>
