<template>
  <div id="app">
    <router-view/>
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: var(--font-family-base);
  background-color: var(--color-background);
  color: var(--color-text);
  line-height: var(--line-height-normal);
}

#app {
  min-height: 100vh;
}
</style>

