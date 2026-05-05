import { createRouter, createWebHistory } from 'vue-router'
import { getBasePath } from '../../../../../../workshop-frontend-shared/src/utils/basePath.js'

const HomeView = () => import('../views/BuildingMultiAgentsHome.vue')
const EditorView = () => import('../views/BuildingMultiAgentsEditor.vue')

const routes = [
  { path: '/', redirect: '/0' },
  { path: '/0', name: 'BuildingMultiAgentsIntro', component: HomeView, props: { pageId: '0' } },
  { path: '/1', name: 'BuildingMultiAgentsStructure', component: HomeView, props: { pageId: '1' } },
  { path: '/2', name: 'BuildingMultiAgentsContracts', component: HomeView, props: { pageId: '2' } },
  { path: '/3', name: 'BuildingMultiAgentsMarketData', component: HomeView, props: { pageId: '3' } },
  { path: '/4', name: 'BuildingMultiAgentsCoordinator', component: HomeView, props: { pageId: '4' } },
  { path: '/5', name: 'BuildingMultiAgentsOrchestration', component: HomeView, props: { pageId: '5' } },
  { path: '/6', name: 'BuildingMultiAgentsEditor', component: EditorView },
  { path: '/editor', redirect: '/6' }
]

export default createRouter({
  history: createWebHistory(getBasePath() || '/'),
  routes
})
