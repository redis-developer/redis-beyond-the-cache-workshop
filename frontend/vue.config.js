module.exports = {
  publicPath: '/',
  lintOnSave: false,
  outputDir: 'dist',
  devServer: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:9001',
        changeOrigin: true
      },
      '/session': {
        target: 'http://localhost:9001',
        changeOrigin: true
      }
    }
  }
}
