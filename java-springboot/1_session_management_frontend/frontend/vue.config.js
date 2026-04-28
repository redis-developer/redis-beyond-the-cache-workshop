const { defineConfig } = require('@vue/cli-service')

// Base path for the workshop - can be overridden via VUE_APP_BASE_PATH env var
const basePath = process.env.VUE_APP_BASE_PATH || '/'
const backendTarget = process.env.WORKSHOP_BACKEND_URL || process.env.VUE_APP_WORKSHOP_BACKEND_URL || 'http://localhost:18080'

module.exports = defineConfig({
  transpileDependencies: true,
  outputDir: '../src/main/resources/static',
  publicPath: basePath,
  devServer: {
    port: 9080,
    proxy: {
      '/api': {
        target: backendTarget,
        changeOrigin: true
      },
      '/login': {
        target: backendTarget,
        changeOrigin: true
      },
      '/logout': {
        target: backendTarget,
        changeOrigin: true
      }
    }
  }
})
