import { createRouter, createWebHistory } from 'vue-router'
import store from '@/store'

// Lazy-load view components for code splitting
const PortalHome = () => import('../views/PortalHome.vue')

const routes = [
  {
    path: '/',
    name: 'PortalHome',
    component: PortalHome
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

router.beforeEach(async (to) => {
  if (!store.state.portalLoaded) {
    await store.dispatch('loadPortalUser');
  }

  return true;
})

export default router
