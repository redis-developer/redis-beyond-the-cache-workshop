import { createRouter, createWebHistory } from 'vue-router'
import { getBasePath } from '../../../../../../workshop-frontend-shared/src/utils/basePath.js'

const HomeView = () => import('../views/SpringAiFundamentalsHome.vue')
const EditorView = () => import('../views/SpringAiFundamentalsEditor.vue')

const routes = [
  { path: '/', redirect: '/0' },
  { path: '/0', name: 'SpringAiFundamentalsIntro', component: HomeView, props: { pageId: '0' } },
  { path: '/1', name: 'SpringAiFundamentalsPrompt', component: HomeView, props: { pageId: '1' } },
  { path: '/2', name: 'SpringAiFundamentalsStructuredOutput', component: HomeView, props: { pageId: '2' } },
  { path: '/3', name: 'SpringAiFundamentalsTools', component: HomeView, props: { pageId: '3' } },
  { path: '/4', name: 'SpringAiFundamentalsMemory', component: HomeView, props: { pageId: '4' } },
  { path: '/5', name: 'SpringAiFundamentalsHelper', component: HomeView, props: { pageId: '5' } },
  { path: '/editor', name: 'SpringAiFundamentalsEditor', component: EditorView }
]

export default createRouter({
  history: createWebHistory(getBasePath() || '/'),
  routes
})
