<template>
  <div class="resume-container">
    <div class="page-header">
      <h1>我的简历</h1>
      <p>管理您的简历信息</p>
    </div>

    <div class="resume-content">
      <div class="resume-card">
        <div v-if="hasResume" class="resume-header">
          <div class="resume-avatar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
          </div>
          <div class="resume-info">
            <h2>{{ resumeData.name || '我的简历' }}</h2>
            <p>最后更新: {{ resumeData.updateTime || '未知' }}</p>
          </div>
        </div>

        <div v-if="hasResume" class="resume-body">
          <div class="resume-section" v-if="resumeData.basicInfo">
            <h3>基本信息</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">姓名</span>
                <span class="value">{{ resumeData.basicInfo.name || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">性别</span>
                <span class="value">{{ resumeData.basicInfo.gender || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">年龄</span>
                <span class="value">{{ resumeData.basicInfo.age || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">学历</span>
                <span class="value">{{ resumeData.basicInfo.education || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="resume-section" v-if="resumeData.contact">
            <h3>联系方式</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">手机</span>
                <span class="value">{{ resumeData.contact.phone || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">邮箱</span>
                <span class="value">{{ resumeData.contact.email || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="resume-section" v-if="resumeData.intention">
            <h3>求职意向</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">期望岗位</span>
                <span class="value">{{ resumeData.intention.position || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">期望薪资</span>
                <span class="value">{{ resumeData.intention.salary || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">工作地点</span>
                <span class="value">{{ resumeData.intention.location || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">入职时间</span>
                <span class="value">{{ resumeData.intention.joinTime || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="resume-section" v-if="resumeData.skills && resumeData.skills.length > 0">
            <h3>技能标签</h3>
            <div class="skill-tags">
              <span class="skill-tag" v-for="(skill, index) in resumeData.skills" :key="index">
                {{ skill }}
              </span>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
          </svg>
          <p>暂无简历信息</p>
          <span>请上传您的简历开始使用</span>
        </div>

        <div class="resume-actions" v-if="hasResume">
          <button class="btn btn-primary">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
            </svg>
            编辑简历
          </button>
          <button class="btn btn-secondary" @click="downloadResume">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="7 10 12 15 17 10"></polyline>
              <line x1="12" y1="15" x2="12" y2="3"></line>
            </svg>
            下载简历
          </button>
        </div>
      </div>

      <div class="upload-card">
        <h3>上传简历</h3>
        <p>支持 PDF、Word 格式，最大 10MB</p>

        <div class="upload-area" @click="triggerUpload">
          <input type="file" ref="fileInput" @change="handleFileChange" accept=".pdf,.doc,.docx" style="display: none" />
          <div v-if="!uploadFile" class="upload-placeholder">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="17 8 12 3 7 8"></polyline>
              <line x1="12" y1="3" x2="12" y2="15"></line>
            </svg>
            <p>点击上传简历</p>
          </div>
          <div v-else class="file-preview">
            <span>{{ uploadFile.name }}</span>
            <button @click.stop="removeFile">×</button>
          </div>
        </div>

        <button class="btn btn-primary btn-full" :disabled="!uploadFile || loading" @click="uploadResume">
          <span v-if="loading" class="btn-loading">
            <span class="spinner"></span>
            {{ uploadProgress > 0 ? '上传中...' : 'AI正在解析简历...' }}
          </span>
          <span v-else>上传简历</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'MyResume',
  data() {
    return {
      uploadFile: null,
      hasResume: false,
      resumeData: {},
      loading: false,
      uploadProgress: 0
    }
  },
  mounted() {
    this.fetchResume()
  },
  methods: {
    async fetchResume() {
      try {
        const token = localStorage.getItem('token')
        const response = await axios.get('http://localhost:8080/api/resume/info', {
          headers: { Authorization: `Bearer ${token}` }
        })
        if (response.data.data) {
          this.resumeData = response.data.data
          this.hasResume = true
        }
      } catch (err) {
        console.log('暂无简历')
      }
    },
    triggerUpload() {
      this.$refs.fileInput.click()
    },
    handleFileChange(event) {
      const file = event.target.files[0]
      if (file && file.size < 10 * 1024 * 1024) {
        this.uploadFile = file
      } else if (file) {
        alert('文件大小不能超过10MB')
      }
    },
    removeFile() {
      this.uploadFile = null
      this.$refs.fileInput.value = ''
    },
    async downloadResume() {
      try {
        const token = localStorage.getItem('token')
        const response = await axios.get('http://localhost:8080/api/resume/download', {
          headers: { Authorization: `Bearer ${token}` },
          responseType: 'blob'
        })

        const blob = new Blob([response.data])
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = this.resumeData.fileName || 'resume.pdf'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (err) {
        alert('下载失败')
      }
    },
    async uploadResume() {
      if (!this.uploadFile) return

      this.loading = true
      this.uploadProgress = 0
      try {
        const token = localStorage.getItem('token')
        const formData = new FormData()
        formData.append('file', this.uploadFile)

        const response = await axios.post(
          'http://localhost:8080/api/resume/upload',
          formData,
          {
            headers: {
              Authorization: `Bearer ${token}`,
              'Content-Type': 'multipart/form-data'
            },
            onUploadProgress: (progressEvent) => {
              if (progressEvent.total) {
                this.uploadProgress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
              }
            }
          }
        )

        if (response.data.success) {
          this.resumeData = response.data.data
          this.hasResume = true
          this.uploadFile = null
          this.$refs.fileInput.value = ''
          alert('简历上传成功！')
        }
      } catch (err) {
        alert(err.response?.data?.message || '上传失败')
      } finally {
        this.loading = false
        this.uploadProgress = 0
      }
    }
  }
}
</script>

<style scoped>
.resume-container {
  max-width: 1000px;
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

.resume-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.resume-card, .upload-card {
  background: var(--surface-color);
  border-radius: var(--radius-xl);
  padding: 28px;
  border: 1px solid var(--border-color);
}

.resume-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 24px;
}

.resume-avatar {
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.resume-avatar svg {
  width: 36px;
  height: 36px;
  color: white;
}

.resume-info h2 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 4px;
}

.resume-info p {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.empty-state {
  text-align: center;
  padding: 48px 20px;
}

.empty-state svg {
  width: 64px;
  height: 64px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.empty-state p {
  font-size: 1rem;
  font-weight: 500;
  color: var(--text-color);
  margin-bottom: 8px;
}

.empty-state span {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.resume-section {
  margin-bottom: 24px;
}

.resume-section:last-child {
  margin-bottom: 0;
}

.resume-section h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-color);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item .label {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.info-item .value {
  font-size: 0.9375rem;
  color: var(--text-color);
  font-weight: 500;
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.skill-tag {
  padding: 6px 14px;
  background: var(--primary-light);
  color: var(--primary-color);
  border-radius: var(--radius-full);
  font-size: 0.8125rem;
  font-weight: 500;
}

.resume-actions {
  display: flex;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
  margin-top: 24px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
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

.btn-secondary {
  background: var(--surface-color);
  color: var(--text-color);
  border: 1px solid var(--border-color);
}

.btn-secondary:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.upload-card h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 8px;
}

.upload-card > p {
  font-size: 0.8125rem;
  color: var(--text-muted);
  margin-bottom: 20px;
}

.upload-area {
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-lg);
  padding: 32px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 16px;
}

.upload-area:hover {
  border-color: var(--primary-color);
  background: var(--primary-light);
}

.upload-placeholder svg {
  width: 48px;
  height: 48px;
  color: var(--text-muted);
  margin-bottom: 12px;
}

.upload-placeholder p {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.file-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--background-color);
  border-radius: var(--radius-md);
}

.file-preview span {
  font-size: 0.875rem;
  color: var(--text-color);
  font-weight: 500;
}

.file-preview button {
  width: 24px;
  height: 24px;
  border: none;
  background: rgba(239, 68, 68, 0.1);
  color: var(--error-color);
  border-radius: 50%;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-full {
  width: 100%;
}

.btn-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .resume-content {
    grid-template-columns: 1fr;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .resume-actions {
    flex-direction: column;
  }

  .btn {
    width: 100%;
  }
}
</style>
