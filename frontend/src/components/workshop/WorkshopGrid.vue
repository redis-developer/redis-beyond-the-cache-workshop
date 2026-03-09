<template>
  <div class="workshop-grid">
    <WorkshopCard 
      v-for="workshop in workshops" 
      :key="workshop.id" 
      :workshop="workshop"
    />
  </div>
</template>

<script>
import WorkshopCard from './WorkshopCard.vue';

export default {
  name: 'WorkshopGrid',
  components: {
    WorkshopCard
  },
  props: {
    workshops: {
      type: Array,
      required: true
    }
  }
};
</script>

<style scoped>
.workshop-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 2rem;
  margin-bottom: 3rem;
  overflow: visible;
}

@media (max-width: 768px) {
  .workshop-grid {
    grid-template-columns: 1fr;
  }
}
</style>

