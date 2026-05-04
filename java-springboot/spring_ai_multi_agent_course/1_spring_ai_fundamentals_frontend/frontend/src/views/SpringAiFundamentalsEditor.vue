<template>
  <WorkshopEditorLayout
    title="Module 1: Spring AI Fundamentals Editor"
    :files="files"
    show-session-restart-controls
  >
    <template #instructions>
      <WorkshopContentRenderer
        v-if="content"
        :content="content"
        :show-title="false"
        :show-summary="true"
        :show-stage-title="false"
      />
    </template>
  </WorkshopEditorLayout>
</template>

<script>
import {
  WorkshopContentRenderer,
  WorkshopEditorLayout
} from '../../../../../../workshop-frontend-shared/src/index.js'
import { fetchWorkshopContent } from '../utils/workshopContent'

export default {
  name: 'SpringAiFundamentalsEditor',
  components: {
    WorkshopContentRenderer,
    WorkshopEditorLayout
  },
  data() {
    return {
      files: [
        'build.gradle.kts',
        'application.properties',
        'SpringAiFundamentalsAiConfig.java',
        'SpringAiDemoService.java',
        'MarketDataTools.java'
      ],
      content: null
    }
  },
  async mounted() {
    this.content = await fetchWorkshopContent('6')
  }
}
</script>
