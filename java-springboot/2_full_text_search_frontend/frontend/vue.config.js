const { defineConfig } = require('@vue/cli-service')

// Base path for the workshop - can be overridden via VUE_APP_BASE_PATH env var
const basePath = process.env.VUE_APP_BASE_PATH || '/'

module.exports = defineConfig({
  transpileDependencies: true,
  outputDir: '../src/main/resources/static',
  publicPath: basePath,
  devServer: {
    port: 8081,
    proxy: {
      '/api': {
        target: 'http://localhost:18081',
        changeOrigin: true
      }
    }
  }
})
