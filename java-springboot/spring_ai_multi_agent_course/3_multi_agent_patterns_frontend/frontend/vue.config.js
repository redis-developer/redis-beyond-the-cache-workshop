const path = require('path')

module.exports = {
  outputDir: '../src/main/resources/static',
  publicPath: process.env.VUE_APP_BASE_PATH || '/',
  configureWebpack: {
    resolve: {
      alias: {
        '@redis-workshop/shared': path.resolve(__dirname, '../../../../workshop-frontend-shared/src')
      }
    }
  }
}
