import { createRouter, createWebHistory } from 'vue-router'
import { getBasePath } from '../utils/basePath'

const SessionHome = () => import('../views/SessionHome.vue')
const SessionEditor = () => import('../views/SessionEditor.vue')
const RedisInsightView = () => import('../views/RedisInsightView.vue')

const HOME_ROUTE = '/0'
const EDITOR_ROUTE = '/4'
const REDIS_INSIGHT_ROUTE = '/redis-insight-view'

const routes = [
  {
    path: '/',
    redirect: HOME_ROUTE
  },
  ...['0', '1', '2', '3'].map(pageId => ({
    path: `/${pageId}`,
    name: `SessionPage${pageId}`,
    component: SessionHome,
    props: { pageId }
  })),
  {
    path: EDITOR_ROUTE,
    name: 'SessionEditor',
    component: SessionEditor
  },
  {
    path: REDIS_INSIGHT_ROUTE,
    name: 'RedisInsightView',
    component: RedisInsightView
  },
  {
    path: '/welcome',
    redirect: HOME_ROUTE
  },
  {
    path: '/editor',
    redirect: EDITOR_ROUTE
  }
]

const router = createRouter({
  history: createWebHistory(getBasePath() || '/'),
  routes
})

// Navigation guard to check authentication before accessing protected routes
router.beforeEach(async (to, from, next) => {
  // Public routes that don't require authentication
  const publicRoutes = ['/0', '/1', '/2', '/3', '/4', REDIS_INSIGHT_ROUTE, '/welcome']

  if (publicRoutes.includes(to.path)) {
    next()
    return
  }

  // For protected routes, check if user is authenticated
  try {
    const basePath = `${getBasePath()}/`.replace(/\/+$/, '/')
    const response = await fetch(`${basePath}api/session-info`, {
      credentials: 'include'
    })

    if (response.status === 401 || response.status === 403) {
      next(HOME_ROUTE)
    } else if (response.ok) {
      // Authenticated, allow navigation
      next()
    } else {
      next(HOME_ROUTE)
    }
  } catch (error) {
    console.error('Auth check failed:', error)
    next(HOME_ROUTE)
  }
})

export default router
