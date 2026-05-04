const { defineConfig } = require('@vue/cli-service')

const basePath = process.env.VUE_APP_BASE_PATH || '/'
const backendTarget = process.env.WORKSHOP_BACKEND_URL

module.exports = defineConfig({
  transpileDependencies: true,
  outputDir: '../src/main/resources/static',
  publicPath: basePath,
  devServer: {
    port: process.env.FRONTEND_PORT ? Number(process.env.FRONTEND_PORT) : undefined,
    proxy: backendTarget ? {
      '/api': {
        target: backendTarget,
        changeOrigin: true
      }
    } : undefined
  }
})
