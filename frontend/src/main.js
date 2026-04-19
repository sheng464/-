import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'
import './assets/main.css'

let isRedirectingToLogin = false

const originalFetch = window.fetch
window.fetch = async function(...args) {
  if (isRedirectingToLogin) {
    return new Response(JSON.stringify({ error: 'redirecting' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json' }
    })
  }

  try {
    const response = await originalFetch.apply(this, args)

    if (response.status === 401) {
      isRedirectingToLogin = true
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('role')
      router.push('/login')
      isRedirectingToLogin = false
    }

    return response
  } catch (error) {
    console.error('Fetch error:', error)
    throw error
  }
}

axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

const app = createApp(App)
app.use(router)
app.mount('#app')