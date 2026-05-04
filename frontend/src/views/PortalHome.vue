<template>
  <div>
    <main v-if="!portalLoaded" class="portal-loading">
      <p>Loading workshop portal</p>
    </main>
    <WorkshopHub v-else-if="isPortalAuthenticated" />
    <PortalEntry v-else />
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex';
import PortalEntry from '@/views/PortalEntry.vue';
import WorkshopHub from '@/views/WorkshopHub.vue';

export default {
  name: 'PortalHome',
  components: {
    PortalEntry,
    WorkshopHub
  },
  computed: {
    ...mapState(['portalLoaded']),
    ...mapGetters(['isPortalAuthenticated'])
  }
};
</script>

<style scoped>
.portal-loading {
  align-items: center;
  background-color: var(--color-background);
  color: var(--color-text-muted);
  display: flex;
  min-height: 100vh;
  justify-content: center;
}
</style>
