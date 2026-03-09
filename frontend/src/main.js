import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'

// Import Bootstrap CSS and JS
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import 'bootstrap-icons/font/bootstrap-icons.css'

// Import Design System from shared package
import '@redis-workshop/shared/styles/tokens.css'
import '@redis-workshop/shared/styles/dark-theme.css'
import './styles/utilities.css'

// Create and mount the Vue application
createApp(App)
  .use(store)
  .use(router)
  .mount('#app')

