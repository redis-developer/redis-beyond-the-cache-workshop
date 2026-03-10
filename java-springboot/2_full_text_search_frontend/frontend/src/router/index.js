import { createRouter, createWebHistory } from 'vue-router'
import { getBasePath } from '../utils/basePath'

const SearchHome = () => import('../views/SearchHome.vue')
const SearchDemo = () => import('../views/SearchDemo.vue')
const SearchEditor = () => import('../views/SearchEditor.vue')

const routes = [
  {
    path: '/',
    name: 'SearchHome',
    component: SearchHome
  },
  {
    path: '/search',
    name: 'SearchDemo',
    component: SearchDemo
  },
  {
    path: '/editor',
    name: 'SearchEditor',
    component: SearchEditor
  }
]

const router = createRouter({
  history: createWebHistory(getBasePath() || '/'),
  routes
})

export default router
