import { getBasePath } from '../../../../../workshop-frontend-shared/src/utils/basePath.js'

// eslint-disable-next-line no-undef
__webpack_public_path__ = `${getBasePath()}/`.replace(/\/+$/, '/')

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

import '../../../../../workshop-frontend-shared/src/styles/tokens.css'
import '../../../../../workshop-frontend-shared/src/styles/dark-theme.css'
import '../../../../../workshop-frontend-shared/src/styles/components.css'
import '../../../../../workshop-frontend-shared/src/styles/content-renderer.css'

createApp(App).use(router).mount('#app')
