<template>
  <div class="admin-knowledge-container">
    <div class="page-header">
      <h1>⭐ 优秀问答管理</h1>
      <p>管理优秀面试问答对，用于面试评价参考</p>
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
        <span class="btn-icon">➕</span> 添加优秀问答
      </button>
      <div class="filter-group">
        <select v-model="filterCategory" class="filter-select" @change="loadList">
          <option value="">全部分类</option>
          <option value="Java后端">Java后端</option>
          <option value="Web前端">Web前端</option>
        </select>
      </div>
    </div>

    <div class="knowledge-list">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="items.length === 0" class="empty-state">
        <p>暂无优秀问答</p>
        <button class="btn btn-primary" @click="showAddModal = true">添加第一条</button>
      </div>
      <div v-else class="knowledge-grid">
        <div v-for="item in items" :key="item.id" class="knowledge-card">
          <div class="card-header">
            <span class="category-tag">{{ item.category }}</span>
            <span class="status-badge" :class="item.status === 'ACTIVE' ? 'active' : 'inactive'">
              {{ item.status === 'ACTIVE' ? '已索引' : '未索引' }}
            </span>
          </div>
          <h3 class="card-title">{{ item.title }}</h3>
          <div class="qa-container">
            <div class="qa-item">
              <span class="qa-content">{{ truncateContent(item.content) }}</span>
            </div>
          </div>
          <div class="card-footer">
            <span class="chunk-info">评分: {{ item.score }}</span>
            <span class="date-info">{{ formatDate(item.createdAt) }}</span>
          </div>
          <div class="card-actions">
            <button class="btn btn-small" @click="viewDetail(item)">查看</button>
            <button v-if="isAdmin" class="btn btn-small btn-danger" @click="confirmDelete(item)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showAddModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <div class="modal-header">
          <h2>添加优秀问答</h2>
          <button class="close-btn" @click="closeModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>标题</label>
            <input v-model="formData.title" type="text" class="form-input" placeholder="输入标题" />
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
            <textarea v-model="formData.content" class="form-textarea" rows="8" placeholder="输入优秀回答内容"></textarea>
          </div>
          <div class="form-group">
            <label>或上传文件 (txt/md)</label>
            <input type="file" class="form-file" accept=".txt,.md" @change="handleFileUpload" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn" @click="closeModal">取消</button>
          <button class="btn btn-primary" @click="submitForm" :disabled="submitting">
            {{ submitting ? '提交中...' : '提交' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showDeleteModal" class="modal-overlay" @click.self="showDeleteModal = false">
      <div class="modal">
        <div class="modal-header">
          <h2>确认删除</h2>
          <button class="close-btn" @click="showDeleteModal = false">×</button>
        </div>
        <div class="modal-body">
          <p>确定要删除这条优秀回答吗？</p>
          <div class="delete-preview">
            <p><strong>标题：</strong>{{ deleteTarget?.title }}</p>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn" @click="showDeleteModal = false">取消</button>
          <button class="btn btn-danger" @click="deleteItem" :disabled="deleting">
            {{ deleting ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showDetailModal" class="modal-overlay" @click.self="showDetailModal = false">
      <div class="modal modal-large">
        <div class="modal-header">
          <h2>优秀问答详情</h2>
          <button class="close-btn" @click="showDetailModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-row">
            <span class="detail-label">分类：</span>
            <span class="detail-value">{{ detailItem?.category }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">评分：</span>
            <span class="detail-value">{{ detailItem?.score }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">状态：</span>
            <span class="detail-value">{{ detailItem?.status }}</span>
          </div>
          <div class="detail-section">
            <h3>内容</h3>
            <p class="detail-content">{{ detailItem?.content }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminExcellentAnswer',
  data() {
    return {
      items: [],
      categories: [],
      statistics: {
        totalCount: 0,
        activeCount: 0
      },
      loading: false,
      submitting: false,
      deleting: false,
      showAddModal: false,
      showDeleteModal: false,
      showDetailModal: false,
      filterCategory: '',
      deleteTarget: null,
      detailItem: null,
        formData: {
          title: '',
          content: '',
          category: ''
        }
    }
  },
  computed: {
    isAdmin() {
      return localStorage.getItem('role') === 'ADMIN'
    }
  },
  mounted() {
    this.loadList()
    this.loadCategories()
    this.loadStatistics()
  },
  methods: {
    async loadList() {
      this.loading = true
      try {
        const token = localStorage.getItem('token')
        const response = await fetch(`http://localhost:8080/api/admin/excellent-answer/list?page=0&size=100`, {
          headers: { 'Authorization': `Bearer ${token}` }
        })
        const result = await response.json()
        if (result.success === true) {
          this.items = result.data?.items || []
        }
      } catch (error) {
        console.error('加载失败:', error)
      } finally {
        this.loading = false
      }
    },
    async loadCategories() {
      try {
        const token = localStorage.getItem('token')
        const response = await fetch('http://localhost:8080/api/admin/excellent-answer/categories', {
          headers: { 'Authorization': `Bearer ${token}` }
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
        const token = localStorage.getItem('token')
        const response = await fetch('http://localhost:8080/api/admin/excellent-answer/statistics', {
          headers: { 'Authorization': `Bearer ${token}` }
        })
        const result = await response.json()
        if (result.success === true) {
          this.statistics = result.data || { totalCount: 0, activeCount: 0 }
        }
      } catch (error) {
        console.error('加载统计失败:', error)
      }
    },
    async submitForm() {
      if (!this.formData.title || !this.formData.content || !this.formData.category) {
        alert('请填写所有字段')
        return
      }
      this.submitting = true
      try {
        const token = localStorage.getItem('token')
        const params = new URLSearchParams({
          title: this.formData.title,
          content: this.formData.content,
          category: this.formData.category
        })
        const response = await fetch('http://localhost:8080/api/admin/excellent-answer/add', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          body: params
        })
        const result = await response.json()
        if (result.success === true) {
          this.closeModal()
          this.loadList()
          this.loadStatistics()
          alert('添加成功')
        } else {
          alert('添加失败: ' + (result.message || '未知错误'))
        }
      } catch (error) {
        console.error('提交失败:', error)
        alert('提交失败: ' + error.message)
      } finally {
        this.submitting = false
      }
    },
    viewDetail(item) {
      this.detailItem = item
      this.showDetailModal = true
    },
    confirmDelete(item) {
      this.deleteTarget = item
      this.showDeleteModal = true
    },
    async deleteItem() {
      if (!this.deleteTarget) return
      this.deleting = true
      try {
        const token = localStorage.getItem('token')
        const response = await fetch(`http://localhost:8080/api/admin/excellent-answer/${this.deleteTarget.id}`, {
          method: 'DELETE',
          headers: { 'Authorization': `Bearer ${token}` }
        })
        const result = await response.json()
        if (result.success === true) {
          this.showDeleteModal = false
          this.loadList()
          this.loadStatistics()
          alert('删除成功')
        } else {
          alert('删除失败: ' + (result.message || '未知错误'))
        }
      } catch (error) {
        console.error('删除失败:', error)
        alert('删除失败: ' + error.message)
      } finally {
        this.deleting = false
      }
    },
    handleFileUpload(event) {
      const file = event.target.files[0]
      if (file) {
        const reader = new FileReader()
        reader.onload = (e) => {
          const content = e.target.result
          try {
            const parsed = JSON.parse(content)
            if (parsed.content) {
              this.formData.content = parsed.content
              if (parsed.title && !this.formData.title) {
                this.formData.title = parsed.title
              }
              if (parsed.category && !this.formData.category) {
                this.formData.category = parsed.category
              }
            } else {
              this.formData.content = content
            }
          } catch {
            this.formData.content = content
          }
        }
        reader.readAsText(file)
      }
    },
    closeModal() {
      this.showAddModal = false
      this.formData = {
        title: '',
        content: '',
        category: ''
      }
    },
    truncateContent(content) {
      if (!content) return ''
      return content.length > 100 ? content.substring(0, 100) + '...' : content
    },
    formatDate(dateStr) {
      if (!dateStr) return ''
      return dateStr.substring(0, 10)
    }
  }
}
</script>

<style scoped>
.admin-knowledge-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  margin-bottom: 8px;
}

.page-header p {
  color: #666;
}

.stats-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.stat-icon {
  font-size: 24px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
}

.stat-label {
  font-size: 12px;
  color: #666;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-primary {
  background: #1890ff;
  color: white;
}

.btn-danger {
  background: #ff4d4f;
  color: white;
}

.btn-warning {
  background: #faad14;
  color: white;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-small {
  padding: 4px 12px;
  font-size: 12px;
}

.filter-group {
  display: flex;
  gap: 12px;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.knowledge-list {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.loading, .empty-state {
  text-align: center;
  padding: 40px;
  color: #666;
}

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 16px;
}

.knowledge-card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.category-tag {
  background: #e6f7ff;
  color: #1890ff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-badge.active {
  background: #d9f7be;
  color: #52c41a;
}

.status-badge.inactive {
  background: #fff1e6;
  color: #fa8c16;
}

.card-title {
  font-size: 16px;
  margin-bottom: 8px;
}

.card-content {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
}

.qa-container {
  margin-bottom: 12px;
}

.qa-item {
  margin-bottom: 8px;
}

.qa-label {
  font-weight: bold;
  color: #333;
}

.qa-content {
  color: #666;
  font-size: 14px;
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
  justify-content: flex-end;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  border-radius: 8px;
  width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-large {
  width: 700px;
}

.modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
}

.form-input, .form-textarea, .form-file {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-textarea {
  resize: vertical;
}

.delete-preview {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  margin-top: 12px;
}

.detail-row {
  display: flex;
  margin-bottom: 12px;
}

.detail-label {
  font-weight: 500;
  margin-right: 8px;
}

.detail-section {
  margin-top: 16px;
}

.detail-section h3 {
  margin-bottom: 8px;
  font-size: 14px;
}

.detail-content {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  white-space: pre-wrap;
}
</style>
