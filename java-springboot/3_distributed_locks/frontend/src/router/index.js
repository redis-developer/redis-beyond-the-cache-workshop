import { createRouter, createWebHistory } from 'vue-router'
import { getBasePath } from '../utils/basePath'

const LocksHome = () => import('../views/LocksHome.vue')
const ReentrantLearn = () => import('../views/ReentrantLearn.vue')
const LocksDemo = () => import('../views/LocksDemo.vue')
const LocksEditor = () => import('../views/LocksEditor.vue')
const LocksImplement = () => import('../views/LocksImplement.vue')

const routes = [
  // Hub / Home
  {
    path: '/',
    name: 'LocksHome',
    component: LocksHome
  },

  // Reentrant Lock module
  {
    path: '/reentrant',
    name: 'ReentrantLearn',
    component: ReentrantLearn
  },
  {
    path: '/reentrant/implement',
    name: 'ReentrantImplement',
    component: LocksImplement
  },
  {
    path: '/reentrant/editor',
    name: 'ReentrantEditor',
    component: LocksEditor
  },
  {
    path: '/reentrant/demo',
    name: 'ReentrantDemo',
    component: LocksDemo
  },

  // Future lock type modules (placeholders)
  // { path: '/fair-lock', ... },
  // { path: '/readwrite', ... },
  // { path: '/semaphore', ... },

  // Legacy redirects
  { path: '/demo', redirect: '/reentrant/demo' },
  { path: '/editor', redirect: '/reentrant/editor' },
  { path: '/implement', redirect: '/reentrant/implement' },
  { path: '/scenario/:index/:step?', redirect: '/reentrant/demo' },
  { path: '/implementation', redirect: '/reentrant/implement' },
  { path: '/complete', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(getBasePath() || '/'),
  routes
})

export default router
