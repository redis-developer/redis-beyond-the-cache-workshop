const webpack = require('webpack');

module.exports = {
  publicPath: '/',
  lintOnSave: false,
  outputDir: 'dist',
  devServer: {
    port: 3000,
    proxy: {
      '/manager/api': {
        target: 'http://localhost:9000',
        changeOrigin: true
      }
    }
  },
  configureWebpack: {
    plugins: [
      new webpack.DefinePlugin({
        'process.env': {
          VUE_APP_API_URL: JSON.stringify('http://localhost:9000')
        }
      })
    ]
  }
}

