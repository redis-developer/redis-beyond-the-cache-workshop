import { createRouter, createWebHistory } from 'vue-router'

// Lazy-load view components for code splitting
const WorkshopHub = () => import('../views/WorkshopHub.vue')

const routes = [
  {
    path: '/',
    name: 'WorkshopHub',
    component: WorkshopHub
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

export default router

