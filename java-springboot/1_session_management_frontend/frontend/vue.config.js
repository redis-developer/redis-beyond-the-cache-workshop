const { defineConfig } = require('@vue/cli-service')

// Base path for the workshop - can be overridden via VUE_APP_BASE_PATH env var
const basePath = process.env.VUE_APP_BASE_PATH || '/'

module.exports = defineConfig({
  transpileDependencies: true,
  outputDir: '../src/main/resources/static',
  publicPath: basePath,
  devServer: {
    port: 9080,
    proxy: {
      '/api': {
        target: 'http://localhost:18080',
        changeOrigin: true
      },
      '/login': {
        target: 'http://localhost:18080',
        changeOrigin: true
      },
      '/logout': {
        target: 'http://localhost:18080',
        changeOrigin: true
      }
    }
  }
})
