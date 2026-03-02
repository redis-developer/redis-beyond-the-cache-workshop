import { getBasePath } from './utils/basePath'

// Ensure dynamic chunks load from the correct base path when proxied by the Hub.
// eslint-disable-next-line no-undef
__webpack_public_path__ = `${getBasePath()}/`.replace(/\/+$/, '/')

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// Import styles from shared package using relative path
import '../../../../workshop-frontend-shared/src/styles/tokens.css'
import '../../../../workshop-frontend-shared/src/styles/dark-theme.css'
import '../../../../workshop-frontend-shared/src/styles/components.css'

createApp(App).use(router).mount('#app')
