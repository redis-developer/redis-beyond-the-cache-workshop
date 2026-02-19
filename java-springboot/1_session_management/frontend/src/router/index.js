import { createRouter, createWebHistory } from 'vue-router'

const SessionLogin = () => import('../views/SessionLogin.vue')
const SessionWelcome = () => import('../views/SessionWelcome.vue')
const SessionEditor = () => import('../views/SessionEditor.vue')

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'SessionLogin',
    component: SessionLogin
  },
  {
    path: '/welcome',
    name: 'SessionWelcome',
    component: SessionWelcome
  },
  {
    path: '/editor',
    name: 'SessionEditor',
    component: SessionEditor
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

// Navigation guard to check authentication before accessing protected routes
router.beforeEach(async (to, from, next) => {
  // Public routes that don't require authentication
  const publicRoutes = ['/login']

  if (publicRoutes.includes(to.path)) {
    next()
    return
  }

  // For protected routes, check if user is authenticated
  try {
    const basePath = process.env.BASE_URL || '/'
    const response = await fetch(`${basePath}api/session-info`, {
      credentials: 'include'
    })

    if (response.status === 401 || response.status === 403) {
      // Not authenticated, redirect to login
      next('/login')
    } else if (response.ok) {
      // Authenticated, allow navigation
      next()
    } else {
      // Other error, redirect to login to be safe
      next('/login')
    }
  } catch (error) {
    console.error('Auth check failed:', error)
    // On error, redirect to login
    next('/login')
  }
})

export default router

