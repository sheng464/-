import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Dashboard from '../views/Dashboard.vue'
import Home from '../views/Home.vue'
import VirtualInterviewHome from '../views/VirtualInterviewHome.vue'
import InterviewStart from '../views/InterviewStart.vue'
import VirtualInterview from '../views/VirtualInterview.vue'
import InterviewRecords from '../views/InterviewRecords.vue'
import InterviewMessages from '../views/InterviewMessages.vue'
import MyResume from '../views/MyResume.vue'
import AdminKnowledge from '../views/AdminKnowledge.vue'
import EvaluationReport from '../views/EvaluationReport.vue'
import EvaluationHistory from '../views/EvaluationHistory.vue'
import AdminExcellentAnswer from '../views/AdminExcellentAnswer.vue'

const routes = [
  {
    path: '/',
    component: Dashboard,
    children: [
      { path: '', name: 'Home', component: Home },
      { path: 'interview', name: 'VirtualInterviewHome', component: VirtualInterviewHome },
      { path: 'interview/start', name: 'InterviewStart', component: InterviewStart },
      { path: 'interview/records', name: 'InterviewRecords', component: InterviewRecords },
      { path: 'interview/:sessionId/:position/:sessionType', name: 'VirtualInterview', component: VirtualInterview, props: route => ({
          sessionId: route.params.sessionId,
          position: route.params.position,
          sessionType: route.params.sessionType,
          maxQuestions: route.query.maxQuestions ? parseInt(route.query.maxQuestions) : 10,
          maxFollowups: route.query.maxFollowups ? parseInt(route.query.maxFollowups) : 3,
          initialMessage: route.query.initialMessage || ''
        }) },
      { path: 'interview/:sessionId/messages', name: 'InterviewMessages', component: InterviewMessages, props: true },
      { path: 'resume', name: 'MyResume', component: MyResume },
      { path: 'knowledge', name: 'Knowledge', component: AdminKnowledge },
      { path: 'evaluation/report', name: 'EvaluationReport', component: EvaluationReport },
      { path: 'evaluation/history', name: 'EvaluationHistory', component: EvaluationHistory },
      { path: 'admin/excellent-answer', name: 'AdminExcellentAnswer', component: AdminExcellentAnswer }
    ]
  },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router