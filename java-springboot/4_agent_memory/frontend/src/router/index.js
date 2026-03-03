import { createRouter, createWebHistory } from 'vue-router'
import { getBasePath } from '../utils/basePath'

const MemoryHome = () => import('../views/MemoryHome.vue')
const MemoryChallenges = () => import('../views/MemoryChallenges.vue')
const MemoryLab = () => import('../views/MemoryLab.vue')
const MemoryDemo = () => import('../views/MemoryDemo.vue')
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
    path: '/lab',
    name: 'MemoryLab',
    component: MemoryLab
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

