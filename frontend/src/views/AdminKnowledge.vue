<template>
  <div class="admin-knowledge-container">
    <div class="page-header">
      <h1>📚 知识库</h1>
      <p>学习面试知识库内容，提升您的面试竞争力</p>
    </div>

    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon">📊</div>
        <div class="stat-info">
          <span class="stat-value">{{ statistics.totalCount }}</span>
          <span class="stat-label">总条目</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">✅</div>
        <div class="stat-info">
          <span class="stat-value">{{ statistics.activeCount }}</span>
          <span class="stat-label">已索引</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📁</div>
        <div class="stat-info">
          <span class="stat-value">{{ categories.length }}</span>
          <span class="stat-label">分类</span>
        </div>
      </div>
    </div>

    <div class="action-bar">
      <button v-if="isAdmin" class="btn btn-primary" @click="showAddModal = true">
        <span class="btn-icon">➕</span> 添加知识条目
      </button>
      <div class="filter-group">
        <select v-model="filterCategory" class="filter-select" @change="loadKnowledgeList">
          <option value="">全部分类</option>
          <option value="Java后端">Java后端</option>
          <option value="Web前端">Web前端</option>
        </select>
      </div>
    </div>

    <div class="knowledge-list">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="knowledgeItems.length === 0" class="empty-state">
        <p>暂无知识库条目</p>
        <button class="btn btn-primary" @click="showAddModal = true">添加第一条</button>
      </div>
      <div v-else class="knowledge-grid">
        <div v-for="item in knowledgeItems" :key="item.id" class="knowledge-card">
          <div class="card-header">
            <span class="category-tag">{{ item.category }}</span>
            <span class="status-badge" :class="item.status === 'ACTIVE' ? 'active' : 'inactive'">
              {{ item.status === 'ACTIVE' ? '已索引' : '未索引' }}
            </span>
          </div>
          <h3 class="card-title">{{ item.title }}</h3>
          <p class="card-content">{{ truncateContent(item.content) }}</p>
          <div class="card-footer">
            <span class="chunk-info">分块: {{ item.chunkCount }}</span>
            <span class="date-info">{{ formatDate(item.createdAt) }}</span>
          </div>
          <div class="card-actions">
            <button class="btn btn-small" @click="viewDetail(item)">查看</button>
            <button v-if="isAdmin" class="btn btn-small btn-warning" @click="editItem(item)">编辑</button>
            <button v-if="isAdmin" class="btn btn-small btn-danger" @click="confirmDelete(item)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showAddModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <div class="modal-header">
          <h2>{{ editingItem ? '编辑知识条目' : '添加知识条目' }}</h2>
          <button class="close-btn" @click="closeModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>标题</label>
            <input v-model="formData.title" type="text" class="form-input" placeholder="输入知识库标题" />
          </div>
          <div class="form-group">
            <label>分类</label>
            <select v-model="formData.category" class="form-input">
              <option value="">请选择分类</option>
              <option value="Java后端">Java后端</option>
              <option value="Web前端">Web前端</option>
            </select>
          </div>
          <div class="form-group">
            <label>内容</label>
            <textarea v-model="formData.content" class="form-textarea" rows="10" placeholder="输入知识库内容，支持多段落..."></textarea>
          </div>
          <div class="form-group">
            <label>或上传文件 (txt/md)</label>
            <input type="file" class="form-file" accept=".txt,.md" @change="handleFileUpload" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeModal">取消</button>
          <button class="btn btn-primary" @click="submitForm" :disabled="submitting">
            {{ submitting ? '提交中...' : '确认添加' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showDetailModal" class="modal-overlay" @click.self="showDetailModal = false">
      <div class="modal modal-large">
        <div class="modal-header">
          <h2>{{ viewingItem?.title }}</h2>
          <button class="close-btn" @click="showDetailModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-meta">
            <span class="category-tag">{{ viewingItem?.category }}</span>
            <span>创建于: {{ formatDate(viewingItem?.createdAt) }}</span>
            <span>分块数: {{ viewingItem?.chunkCount }}</span>
            <span>索引时间: {{ formatDate(viewingItem?.indexedAt) }}</span>
          </div>
          <div class="detail-content">
            <pre>{{ viewingItem?.content }}</pre>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showDetailModal = false">关闭</button>
        </div>
      </div>
    </div>

    <div v-if="showDeleteModal" class="modal-overlay" @click.self="showDeleteModal = false">
      <div class="modal modal-small">
        <div class="modal-header">
          <h2>确认删除</h2>
          <button class="close-btn" @click="showDeleteModal = false">×</button>
        </div>
        <div class="modal-body">
          <p>确定要删除知识条目 "{{ deletingItem?.title }}" 吗？</p>
          <p class="warning-text">此操作不可恢复</p>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showDeleteModal = false">取消</button>
          <button class="btn btn-danger" @click="deleteItem" :disabled="deleting">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminKnowledge',
  data() {
    return {
      knowledgeItems: [],
      categories: [],
      statistics: {
        totalCount: 0,
        activeCount: 0
      },
      loading: false,
      submitting: false,
      deleting: false,
      filterCategory: '',
      showAddModal: false,
      showDetailModal: false,
      showDeleteModal: false,
      editingItem: null,
      viewingItem: null,
      deletingItem: null,
      formData: {
        title: '',
        category: '',
        content: ''
      },
      uploadedFile: null
    }
  },
  computed: {
    isAdmin() {
      return localStorage.getItem('role') === 'ADMIN'
    }
  },
  mounted() {
    this.loadKnowledgeList()
    this.loadCategories()
    this.loadStatistics()
  },
  methods: {
    checkAdminRole() {
      const role = localStorage.getItem('role')
      if (role !== 'ADMIN') {
        this.$router.push('/')
      }
    },
    getToken() {
      return localStorage.getItem('token')
    },
    async loadKnowledgeList() {
      this.loading = true
      try {
        const params = new URLSearchParams()
        if (this.filterCategory) {
          params.append('category', this.filterCategory)
        }
        params.append('page', '0')
        params.append('size', '100')

        const response = await fetch(`http://localhost:8080/api/admin/knowledge/list?${params}`, {
          headers: {
            'Authorization': `Bearer ${this.getToken()}`
          }
        })
        const result = await response.json()
        if (result.success === true) {
          this.knowledgeItems = result.data?.items || []
        }
      } catch (error) {
        console.error('加载知识库列表失败:', error)
      } finally {
        this.loading = false
      }
    },
    async loadCategories() {
      try {
        const response = await fetch('http://localhost:8080/api/admin/knowledge/categories', {
          headers: {
            'Authorization': `Bearer ${this.getToken()}`
          }
        })
        const result = await response.json()
        if (result.success === true) {
          this.categories = result.data || []
        }
      } catch (error) {
        console.error('加载分类失败:', error)
      }
    },
    async loadStatistics() {
      try {
        const response = await fetch('http://localhost:8080/api/admin/knowledge/statistics', {
          headers: {
            'Authorization': `Bearer ${this.getToken()}`
          }
        })
        const result = await response.json()
        if (result.success === true) {
          this.statistics = result.data
        }
      } catch (error) {
        console.error('加载统计失败:', error)
      }
    },
    handleFileUpload(event) {
      const file = event.target.files[0]
      if (file) {
        this.uploadedFile = file
        const reader = new FileReader()
        reader.onload = (e) => {
          this.formData.content = e.target.result
          if (!this.formData.title) {
            this.formData.title = file.name.replace(/\.(txt|md)$/i, '')
          }
        }
        reader.readAsText(file)
      }
    },
    editItem(item) {
      this.editingItem = item
      this.formData = {
        title: item.title,
        category: item.category,
        content: item.content
      }
      this.showAddModal = true
    },
    viewDetail(item) {
      this.viewingItem = item
      this.showDetailModal = true
    },
    confirmDelete(item) {
      this.deletingItem = item
      this.showDeleteModal = true
    },
    async deleteItem() {
      this.deleting = true
      try {
        const response = await fetch(`http://localhost:8080/api/admin/knowledge/delete/${this.deletingItem.id}`, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${this.getToken()}`
          }
        })
        const result = await response.json()
        if (result.success === true) {
          this.showDeleteModal = false
          this.loadKnowledgeList()
          this.loadStatistics()
        } else {
          alert('删除失败: ' + result.msg)
        }
      } catch (error) {
        alert('删除失败: ' + error.message)
      } finally {
        this.deleting = false
      }
    },
    closeModal() {
      this.showAddModal = false
      this.editingItem = null
      this.formData = {
        title: '',
        category: '',
        content: ''
      }
      this.uploadedFile = null
    },
    async submitForm() {
      if (!this.formData.title || !this.formData.category || !this.formData.content) {
        alert('请填写完整的标题、分类和内容')
        return
      }

      this.submitting = true
      try {
        let url = 'http://localhost:8080/api/admin/knowledge/add'
        let method = 'POST'

        const formData = new FormData()
        formData.append('title', this.formData.title)
        formData.append('category', this.formData.category)
        formData.append('content', this.formData.content)

        if (this.uploadedFile) {
          url = 'http://localhost:8080/api/admin/knowledge/upload'
          formData.set('file', this.uploadedFile)
        }

        if (this.editingItem) {
          formData.append('id', this.editingItem.id)
          url = 'http://localhost:8080/api/admin/knowledge/update'
        }

        const response = await fetch(url, {
          method: method,
          headers: {
            'Authorization': `Bearer ${this.getToken()}`
          },
          body: formData
        })

        const text = await response.text()

        if (!response.ok) {
          console.error('HTTP Error:', response.status, text)
          let errorMsg = '服务器错误: ' + response.status
          try {
            const errorJson = JSON.parse(text)
            errorMsg = errorJson.message || errorJson.msg || errorMsg
          } catch (e) {}
          alert('操作失败: ' + errorMsg)
          return
        }

        let result
        try {
          result = JSON.parse(text)
        } catch (e) {
          alert('操作失败: 服务器返回了无效的响应')
          return
        }

        if (result && result.success === true) {
          this.closeModal()
          this.loadKnowledgeList()
          this.loadCategories()
          this.loadStatistics()
        } else {
          const errorMsg = (result && (result.message || result.msg)) || '未知错误'
          alert('操作失败: ' + errorMsg)
        }
      } catch (error) {
        console.error('提交失败:', error)
        let errorMsg = '网络错误，请检查后端服务是否启动'
        if (error.message) {
          errorMsg = error.message
        } else if (error.name === 'TypeError' && error.message === 'Failed to fetch') {
          errorMsg = '无法连接到服务器，请检查后端服务是否启动'
        }
        alert('操作失败: ' + errorMsg)
      } finally {
        this.submitting = false
      }
    },
    truncateContent(content) {
      if (!content) return ''
      return content.length > 150 ? content.substring(0, 150) + '...' : content
    },
    formatDate(dateStr) {
      if (!dateStr) return '-'
      return dateStr.substring(0, 10)
    }
  }
}
</script>

<style scoped>
.admin-knowledge-container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 32px;
  font-weight: 700;
  color: white;
  margin-bottom: 10px;
}

.page-header p {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  font-size: 40px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 16px;
}

.filter-group {
  display: flex;
  gap: 12px;
}

.filter-select {
  padding: 10px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  font-size: 14px;
  background: white;
  cursor: pointer;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: #e0e0e0;
  color: #666;
}

.btn-danger {
  background: #e74c3c;
  color: white;
}

.btn-warning {
  background: #f39c12;
  color: white;
}

.btn-small {
  padding: 6px 12px;
  font-size: 12px;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.knowledge-list {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  min-height: 300px;
}

.loading, .empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.empty-state p {
  margin-bottom: 20px;
  font-size: 16px;
}

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.knowledge-card {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s;
}

.knowledge-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.category-tag {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.status-badge.active {
  background: #d4edda;
  color: #155724;
}

.status-badge.inactive {
  background: #fff3cd;
  color: #856404;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.card-content {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 16px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
  margin-bottom: 12px;
}

.card-actions {
  display: flex;
  gap: 8px;
  border-top: 1px solid #e0e0e0;
  padding-top: 12px;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal {
  background: white;
  border-radius: 20px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-large {
  max-width: 800px;
}

.modal-small {
  max-width: 400px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e0e0e0;
}

.modal-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #999;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.close-btn:hover {
  color: #333;
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e0e0e0;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.form-input, .form-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-input:focus, .form-textarea:focus {
  outline: none;
  border-color: #667eea;
}

.form-textarea {
  resize: vertical;
  font-family: inherit;
  line-height: 1.6;
}

.form-file {
  width: 100%;
  padding: 12px;
  border: 2px dashed #e0e0e0;
  border-radius: 10px;
  background: #f8f9fa;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.detail-meta span {
  font-size: 14px;
  color: #666;
}

.detail-content {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 20px;
}

.detail-content pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.8;
  color: #333;
  margin: 0;
}

.warning-text {
  color: #e74c3c;
  font-size: 14px;
  margin-top: 10px;
}
</style>
