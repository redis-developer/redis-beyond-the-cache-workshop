<template>
  <WorkshopEditorLayout
    ref="layout"
    title="Stage 2: Enable Redis"
    :files="files"
    @file-loaded="onFileLoaded"
  >
    <template #instructions>
      <div class="alert">
        <strong>Your Task:</strong> Uncomment the necessary code to enable Redis session management.
      </div>

      <h3>Instructions:</h3>
      <p class="note">
        Click the play button (▶) next to any step to automatically apply that change!
      </p>

      <h4>Step 1: Add Redis Dependencies</h4>
      <ol>
        <li class="step-with-button">
          <span class="step-content">Click on <code>build.gradle.kts</code> tab above</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Opens the Gradle build file where dependencies are managed">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="loadFileStep('build.gradle.kts')">▶</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Uncomment the Redis dependencies:
            <ul>
              <li><code>spring-boot-starter-data-redis</code></li>
              <li><code>spring-session-data-redis</code></li>
            </ul>
          </span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="These libraries provide Redis connectivity and Spring Session integration, allowing your app to store sessions in Redis instead of memory">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentGradleDependencies">▶</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong> button</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Saves your changes to the build file so they can be applied when the app rebuilds">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="saveFile">▶</button>
          </div>
        </li>
      </ol>

      <h4>Step 2: Configure Application Properties</h4>
      <ol start="4">
        <li class="step-with-button">
          <span class="step-content">Click on <code>application.properties</code> tab above</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Opens the Spring Boot configuration file where application settings are defined">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="loadFileStep('application.properties')">▶</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Change <code>spring.session.store-type=none</code> to <code>spring.session.store-type=redis</code></span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Tells Spring Session to use Redis as the storage backend instead of in-memory storage">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="changeStoreType">▶</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Uncomment the 3 Redis session configuration lines below it</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Configures Redis session namespace, flush mode, and repository type for organizing and managing session data in Redis">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentRedisConfig">▶</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong> button</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Saves your configuration changes so they take effect when the app restarts">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="saveFile">▶</button>
          </div>
        </li>
      </ol>

      <h4>Step 3: Configure Spring Security</h4>
      <ol start="8">
        <li class="step-with-button">
          <span class="step-content">Click on <code>SecurityConfig.java</code> tab above</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Opens the Spring Security configuration file where authentication and session management are configured">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="loadFileStep('SecurityConfig.java')">▶</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>HttpSessionSecurityContextRepository</code> configuration (imports, bean, and filter chain config)</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Configures Spring Security to store authentication details in HTTP sessions (which will now be backed by Redis), enabling session persistence across restarts">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentSecurityConfig">▶</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong> button</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Saves your security configuration changes">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="saveFile">▶</button>
          </div>
        </li>
      </ol>

      <h4>Step 4: Restart and Verify</h4>
      <ol start="11">
        <li>Go to the <a :href="workshopHubUrl" target="_blank" class="link">Workshop Hub</a> and rebuild and restart the workshop backend</li>
        <li><router-link to="/welcome" class="link">Return to workshop</router-link> to verify!</li>
      </ol>
    </template>
  </WorkshopEditorLayout>
</template>

<script>
import { WorkshopEditorLayout } from '../../../../../workshop-frontend-shared/src/index.js';
import { getWorkshopHubUrl } from '../utils/basePath';

export default {
  name: 'SessionEditor',
  components: { WorkshopEditorLayout },
  data() {
    return {
      files: [
        'build.gradle.kts',
        'application.properties',
        'SecurityConfig.java'
      ],
      currentFile: null,
      fileContent: ''
    };
  },
  computed: {
    workshopHubUrl() {
      return this.$refs.layout?.workshopHubUrl || getWorkshopHubUrl();
    }
  },
  methods: {
    onFileLoaded({ fileName, content }) {
      this.currentFile = fileName;
      this.fileContent = content;
    },
    async loadFileStep(fileName) {
      await this.$refs.layout.loadFile(fileName);
    },
    saveFile() {
      this.$refs.layout.save();
    },
    uncommentGradleDependencies() {
      if (this.currentFile !== 'build.gradle.kts') {
        this.$refs.layout.showStatus('Please open build.gradle.kts first!', 'error');
        return;
      }
      let content = this.$refs.layout.getCurrentContent();
      content = content.replace(
        '// implementation("org.springframework.boot:spring-boot-starter-data-redis")',
        'implementation("org.springframework.boot:spring-boot-starter-data-redis")'
      );
      content = content.replace(
        '// implementation("org.springframework.session:spring-session-data-redis")',
        'implementation("org.springframework.session:spring-session-data-redis")'
      );
      this.$refs.layout.updateContent(content);
      this.$refs.layout.showStatus('Redis dependencies uncommented! Click Save!', 'success');
    },
    changeStoreType() {
      if (this.currentFile !== 'application.properties') {
        this.$refs.layout.showStatus('Please open application.properties first!', 'error');
        return;
      }
      let content = this.$refs.layout.getCurrentContent();
      content = content.replace('spring.session.store-type=none', 'spring.session.store-type=redis');
      this.$refs.layout.updateContent(content);
      this.$refs.layout.showStatus('Store type changed to redis! Click Save!', 'success');
    },
    uncommentRedisConfig() {
      if (this.currentFile !== 'application.properties') {
        this.$refs.layout.showStatus('Please open application.properties first!', 'error');
        return;
      }
      let content = this.$refs.layout.getCurrentContent();
      content = content.replace('#spring.session.redis.namespace=spring:session', 'spring.session.redis.namespace=spring:session');
      content = content.replace('#spring.session.redis.flush-mode=immediate', 'spring.session.redis.flush-mode=immediate');
      content = content.replace('#spring.session.redis.repository-type=default', 'spring.session.redis.repository-type=default');
      this.$refs.layout.updateContent(content);
      this.$refs.layout.showStatus('Redis config uncommented! Click Save!', 'success');
    },
    uncommentSecurityConfig() {
      if (this.currentFile !== 'SecurityConfig.java') {
        this.$refs.layout.showStatus('Please open SecurityConfig.java first!', 'error');
        return;
      }
      let content = this.$refs.layout.getCurrentContent();
      content = content.replace(
        '// import org.springframework.security.web.context.HttpSessionSecurityContextRepository;',
        'import org.springframework.security.web.context.HttpSessionSecurityContextRepository;'
      );
      content = content.replace(
        '// import org.springframework.security.web.context.SecurityContextRepository;',
        'import org.springframework.security.web.context.SecurityContextRepository;'
      );
      content = content.replace(
        '            // .securityContext(context -> context\n            //     .securityContextRepository(securityContextRepository())\n            // )',
        '            .securityContext(context -> context\n                .securityContextRepository(securityContextRepository())\n            )'
      );
      content = content.replace(
        '    // @Bean\n    // public SecurityContextRepository securityContextRepository() {\n    //     return new HttpSessionSecurityContextRepository();\n    // }',
        '    @Bean\n    public SecurityContextRepository securityContextRepository() {\n        return new HttpSessionSecurityContextRepository();\n    }'
      );
      this.$refs.layout.updateContent(content);
      this.$refs.layout.showStatus('Security config uncommented! Click Save!', 'success');
    }
  }
};
</script>

<style scoped>
/* All styling is now provided by WorkshopEditorLayout */
</style>
