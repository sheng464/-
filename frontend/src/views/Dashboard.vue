<template>
  <div class="dashboard">
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="logo">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
            <path d="M2 17l10 5 10-5"></path>
            <path d="M2 12l10 5 10-5"></path>
          </svg>
        </div>
        <span v-if="!sidebarCollapsed" class="logo-text">AI面试</span>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path), admin: item.admin }"
        >
          <component :is="item.icon" class="nav-icon" />
          <span v-if="!sidebarCollapsed" class="nav-text">{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <button class="collapse-btn" @click="toggleSidebar">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :class="{ rotated: sidebarCollapsed }">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
        </button>
      </div>
    </aside>

    <main class="main-wrapper">
      <header class="topbar">
        <div class="topbar-left">
          <h1 class="page-title">{{ currentPageTitle }}</h1>
        </div>
        <div class="topbar-right">
          <div class="user-info" v-if="isLoggedIn">
            <div class="user-avatar">
              {{ username?.charAt(0)?.toUpperCase() || 'U' }}
            </div>
            <div class="user-details">
              <span class="username">{{ username }}</span>
              <span class="role-badge" :class="role.toLowerCase()">
                {{ role === 'ADMIN' ? '管理员' : '用户' }}
              </span>
            </div>
          </div>
          <button @click="logout" class="logout-btn">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
              <polyline points="16 17 21 12 16 7"></polyline>
              <line x1="21" y1="12" x2="9" y2="12"></line>
            </svg>
          </button>
        </div>
      </header>

      <div class="main-content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script>
const HomeIcon = {
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
    <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
    <polyline points="9 22 9 12 15 12 15 22"></polyline>
  </svg>`
}

const InterviewIcon = {
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
    <polygon points="23 7 16 12 23 17 23 7"></polygon>
    <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
  </svg>`
}

const ResumeIcon = {
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
    <polyline points="14 2 14 8 20 8"></polyline>
    <line x1="16" y1="13" x2="8" y2="13"></line>
    <line x1="16" y1="17" x2="8" y2="17"></line>
  </svg>`
}

const KnowledgeIcon = {
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
  </svg>`
}

const AdminIcon = {
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
    <circle cx="12" cy="12" r="3"></circle>
    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
  </svg>`
}

export default {
  name: 'Dashboard',
  components: { HomeIcon, InterviewIcon, ResumeIcon, KnowledgeIcon, AdminIcon },
  data() {
    return {
      username: '',
      role: 'USER',
      sidebarCollapsed: false,
      navItems: [
        { path: '/', label: '首页', icon: HomeIcon },
        { path: '/interview', label: '虚拟面试', icon: InterviewIcon },
        { path: '/resume', label: '我的简历', icon: ResumeIcon },
        { path: '/knowledge', label: '知识库', icon: KnowledgeIcon },
        { path: '/admin/excellent-answer', label: '优秀问答', icon: AdminIcon }
      ]
    }
  },
  computed: {
    isLoggedIn() {
      return !!localStorage.getItem('token')
    },
    currentRoute() {
      return this.$route.path
    },
    currentPageTitle() {
      const titles = {
        '/': '首页',
        '/interview': '虚拟面试',
        '/interview/start': '开始面试',
        '/interview/records': '面试记录',
        '/resume': '我的简历',
        '/knowledge': '知识库',
        '/evaluation/report': '评估报告',
        '/evaluation/history': '能力成长',
        '/admin/excellent-answer': '优秀问答'
      }
      return titles[this.currentRoute] || 'AI面试系统'
    }
  },
  mounted() {
    this.checkLogin()
  },
  methods: {
    checkLogin() {
      if (this.isLoggedIn) {
        this.username = localStorage.getItem('username') || '用户'
        this.role = localStorage.getItem('role') || 'USER'
      }
    },
    isActive(path) {
      if (path === '/') {
        return this.currentRoute === '/'
      }
      return this.currentRoute.startsWith(path)
    },
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    logout() {
      const token = localStorage.getItem('token')
      if (token) {
        fetch('http://localhost:8080/api/auth/logout', {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`
          }
        }).catch(() => {})
      }
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('role')
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.dashboard {
  display: flex;
  min-height: 100vh;
  background: var(--background-color);
}

.sidebar {
  width: 260px;
  background: var(--surface-color);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  z-index: 100;
}

.sidebar.collapsed {
  width: 72px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
  border-bottom: 1px solid var(--border-color);
}

.logo {
  width: 40px;
  height: 40px;
  min-width: 40px;
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo svg {
  width: 22px;
  height: 22px;
  color: white;
}

.logo-text {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-color);
  white-space: nowrap;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.2s;
  cursor: pointer;
}

.nav-item:hover {
  background: var(--primary-light);
  color: var(--primary-color);
}

.nav-item.active {
  background: var(--primary-color);
  color: white;
}

.nav-item.active.admin {
  background: linear-gradient(135deg, var(--primary-color), #7C3AED);
}

.nav-icon {
  width: 22px;
  height: 22px;
  min-width: 22px;
}

.nav-text {
  font-size: 0.9375rem;
  font-weight: 500;
  white-space: nowrap;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--border-color);
}

.collapse-btn {
  width: 100%;
  padding: 10px;
  background: var(--background-color);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.collapse-btn:hover {
  background: var(--primary-light);
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.collapse-btn svg {
  width: 18px;
  height: 18px;
  transition: transform 0.3s;
}

.collapse-btn svg.rotated {
  transform: rotate(180deg);
}

.main-wrapper {
  flex: 1;
  margin-left: 260px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  transition: margin-left 0.3s ease;
}

.sidebar.collapsed + .main-wrapper {
  margin-left: 72px;
}

.topbar {
  background: var(--surface-color);
  border-bottom: 1px solid var(--border-color);
  padding: 16px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 50;
}

.topbar-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-color);
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 1rem;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.username {
  font-weight: 500;
  color: var(--text-color);
  font-size: 0.875rem;
}

.role-badge {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-weight: 500;
}

.role-badge.admin {
  background: linear-gradient(135deg, var(--primary-color), #7C3AED);
  color: white;
}

.role-badge.user {
  background: var(--primary-light);
  color: var(--primary-color);
}

.logout-btn {
  width: 40px;
  height: 40px;
  background: var(--background-color);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: #FEE2E2;
  border-color: var(--error-color);
  color: var(--error-color);
}

.logout-btn svg {
  width: 18px;
  height: 18px;
}

.main-content {
  flex: 1;
  padding: 32px;
}

@media (max-width: 1024px) {
  .sidebar {
    width: 72px;
  }

  .logo-text,
  .nav-text {
    display: none;
  }

  .nav-item {
    justify-content: center;
    padding: 12px;
  }

  .main-wrapper {
    margin-left: 72px;
  }
}

@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
  }

  .sidebar.collapsed {
    transform: translateX(0);
    width: 72px;
  }

  .main-wrapper {
    margin-left: 0;
  }

  .topbar {
    padding: 12px 16px;
  }

  .main-content {
    padding: 16px;
  }

  .user-details {
    display: none;
  }
}
</style>
