import { createRouter, createWebHistory } from 'vue-router'
import { getBasePath } from '../../../../../../workshop-frontend-shared/src/utils/basePath.js'

const HomeView = () => import('../views/MultiAgentPatternsHome.vue')
const EditorView = () => import('../views/MultiAgentPatternsEditor.vue')

const routes = [
  { path: '/', redirect: '/0' },
  { path: '/0', name: 'MultiAgentPatternsIntro', component: HomeView, props: { pageId: '0' } },
  { path: '/1', name: 'MultiAgentPatternsStageOne', component: HomeView, props: { pageId: '1' } },
  { path: '/2', name: 'MultiAgentPatternsStageTwo', component: HomeView, props: { pageId: '2' } },
  { path: '/3', name: 'MultiAgentPatternsStageThree', component: HomeView, props: { pageId: '3' } },
  { path: '/4', name: 'MultiAgentPatternsStageFour', component: HomeView, props: { pageId: '4' } },
  { path: '/5', name: 'MultiAgentPatternsStageFive', component: HomeView, props: { pageId: '5' } },
  { path: '/6', name: 'MultiAgentPatternsEditor', component: EditorView },
  { path: '/editor', redirect: '/6' }
]

export default createRouter({
  history: createWebHistory(getBasePath() || '/'),
  routes
})
