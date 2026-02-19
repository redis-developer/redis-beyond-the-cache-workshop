import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// Import styles from shared package using relative path
import '../../../../workshop-frontend-shared/src/styles/tokens.css'
import '../../../../workshop-frontend-shared/src/styles/dark-theme.css'

createApp(App).use(router).mount('#app')

