import { createRouter, createWebHistory } from 'vue-router'
import { getBasePath } from '../utils/basePath'

const MemoryHome = () => import('../views/MemoryHome.vue')
const MemoryChallenges = () => import('../views/MemoryChallenges.vue')
const MemoryEditor = () => import('../views/MemoryEditor.vue')
const MemoryDemo = () => import('../views/MemoryChat.vue')
const MemoryLearn = () => import('../views/MemoryLearn.vue')

const routes = [
  {
    path: '/',
    name: 'MemoryHome',
    component: MemoryHome
  },
  {
    path: '/challenges',
    name: 'MemoryChallenges',
    component: MemoryChallenges
  },
  {
    path: '/editor',
    name: 'MemoryEditor',
    component: MemoryEditor
  },
  {
    path: '/demo',
    name: 'MemoryDemo',
    component: MemoryDemo
  },
  {
    path: '/learn',
    name: 'MemoryLearn',
    component: MemoryLearn
  }
]

const router = createRouter({
  history: createWebHistory(getBasePath() || '/'),
  routes
})

export default router

