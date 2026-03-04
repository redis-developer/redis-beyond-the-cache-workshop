<template>
  <WorkshopEditorLayout
    ref="layout"
    title="Agent Memory Workshop"
    :files="files"
    @file-loaded="onFileLoaded"
  >
    <template #instructions>
      <div class="alert">
        <strong>Your Task:</strong> Implement the Agent Memory Service using the official Java SDK to enable working memory and long-term memory for AI agents.
      </div>

      <!-- Progress Tracker -->
      <div class="progress-tracker">
        <div class="progress-header" @click="progressExpanded = !progressExpanded">
          <span class="progress-title">Progress</span>
          <span class="progress-summary">{{ completedSteps }}/15 steps</span>
          <span class="progress-toggle">{{ progressExpanded ? '-' : '+' }}</span>
        </div>
        <div v-if="progressExpanded" class="progress-details">
          <div class="progress-group">
            <span class="progress-label">AgentMemoryService:</span>
            <span class="progress-count" :class="{ complete: stepsCompleted.service >= 9 }">{{ stepsCompleted.service }}/9</span>
          </div>
          <div class="progress-group">
            <span class="progress-label">ChatMemoryRepository:</span>
            <span class="progress-count" :class="{ complete: stepsCompleted.repository >= 3 }">{{ stepsCompleted.repository }}/3</span>
          </div>
          <div class="progress-group">
            <span class="progress-label">ChatService:</span>
            <span class="progress-count" :class="{ complete: stepsCompleted.chat >= 3 }">{{ stepsCompleted.chat }}/3</span>
          </div>
        </div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: (completedSteps / 15 * 100) + '%' }"></div>
        </div>
      </div>

      <h3>Instructions:</h3>
      <p class="note">Click the play button next to any step to automatically apply that change!</p>

      <h4>Step 0: Initialize the SDK Client</h4>
      <ol>
        <li class="step-with-button">
          <span class="step-content">Click on <code>AgentMemoryService.java</code> tab</span>
          <div class="button-group">
            <button class="play-btn" @click="loadFileStep('AgentMemoryService.java', 0)">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>MemoryAPIClient</code> initialization</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="The client is thread-safe with connection pooling. The namespace isolates your memories from other applications.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentClientInit">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">&#9654;</button>
          </div>
        </li>
      </ol>

      <h4>Step 1: Implement Working Memory - GET</h4>
      <ol start="4">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>getWorkingMemory()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Returns the full conversation history including any auto-generated summary if context exceeded the window.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentGetWorkingMemory">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">&#9654;</button>
          </div>
        </li>
      </ol>

      <h4>Step 2: Implement Working Memory - PUT</h4>
      <ol start="6">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>putWorkingMemory()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="PUT replaces the entire working memory. For appending messages, use appendMessagesToWorkingMemory() instead.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentPutWorkingMemory">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">&#9654;</button>
          </div>
        </li>
      </ol>

      <h4>Step 3: Implement Working Memory - DELETE</h4>
      <ol start="8">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>deleteWorkingMemory()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Immediately removes the session. For automatic cleanup, use TTL instead (set during PUT).">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentDeleteWorkingMemory">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">&#9654;</button>
          </div>
        </li>
      </ol>

      <h4>Step 4: Implement Long-Term Memory - CREATE</h4>
      <ol start="10">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>createLongTermMemory()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="AMS automatically generates vector embeddings for semantic search. The memory_type affects retrieval priority.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentCreateLongTermMemory">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">&#9654;</button>
          </div>
        </li>
      </ol>

      <h4>Step 5: Implement Long-Term Memory - SEARCH</h4>
      <ol start="12">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>searchLongTermMemory()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Semantic search finds memories by MEANING, not keywords. Text is converted to a vector and compared via cosine similarity.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentSearchLongTermMemory">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">&#9654;</button>
          </div>
        </li>
      </ol>

      <h4>Step 6: Implement Long-Term Memory - GET</h4>
      <ol start="14">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>getMemory()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Retrieves a specific memory by ID. Useful for updating or displaying memory details.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentGetMemory">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">&#9654;</button>
          </div>
        </li>
      </ol>

      <h4>Step 7: Implement Long-Term Memory - DELETE</h4>
      <ol start="16">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>deleteMemories()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Permanently removes memories. Consider using memory decay/forgetting policies for automatic lifecycle management.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentDeleteMemories">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">&#9654;</button>
          </div>
        </li>
      </ol>

      <h4>Step 8: Implement Health Check</h4>
      <ol start="18">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>healthCheck()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Returns server status and connected Redis info. Use for monitoring and readiness probes.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentHealthCheck">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">&#9654;</button>
          </div>
        </li>
      </ol>

      <h3>Part 2: Spring AI ChatMemoryRepository</h3>
      <p class="note">Implement the bridge between Spring AI's ChatMemory and Agent Memory Server.</p>

      <h4>Step 9: Implement Context Percentage</h4>
      <ol start="20">
        <li class="step-with-button">
          <span class="step-content">Click on <code>AmsChatMemoryRepository.java</code> tab</span>
          <div class="button-group">
            <button class="play-btn" @click="loadFileStep('AmsChatMemoryRepository.java', 9)">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>getContextPercentage()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Returns 0.0-1.0 indicating how full the context window is. When >0.7, AMS may trigger summarization.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentGetContextPercentage">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">&#9654;</button>
          </div>
        </li>
      </ol>

      <h4>Step 10: Implement Find By Conversation ID</h4>
      <ol start="23">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>findByConversationId()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Spring AI calls this before each LLM request to load conversation history. Parses userId from conversationId for multi-user support.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentFindByConversationId">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">&#9654;</button>
          </div>
        </li>
      </ol>

      <h4>Step 11: Implement Save All Messages</h4>
      <ol start="25">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>saveAll()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Called after each LLM response. Implements deduplication to avoid storing duplicate messages, and sets TTL on first message.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentSaveAll">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">&#9654;</button>
          </div>
        </li>
      </ol>

      <h3>Part 3: Spring AI Advisors</h3>
      <p class="note">Advisors intercept chat requests to automatically handle memory loading and retrieval.</p>

      <h4>Step 12: Pass Conversation ID to Advisors</h4>
      <ol start="27">
        <li class="step-with-button">
          <span class="step-content">Click on <code>ChatService.java</code> tab</span>
          <div class="button-group">
            <button class="play-btn" @click="loadFileStep('ChatService.java', 12)">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Add <code>.advisors()</code> to pass conversationId</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Advisors need the conversationId to load/save the correct conversation. This passes it through the advisor chain.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentAdvisorParams">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">&#9654;</button>
          </div>
        </li>
      </ol>

      <h4>Step 13: Add Working Memory Advisor</h4>
      <ol start="30">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>MessageChatMemoryAdvisor</code> configuration</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Automatically loads conversation history before LLM calls and saves new messages after. No manual memory management needed.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentWorkingMemoryAdvisor">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">&#9654;</button>
          </div>
        </li>
      </ol>

      <h4>Step 14: Add Long-Term Memory Advisor</h4>
      <ol start="32">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>LongTermMemoryAdvisor</code> configuration</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Takes user's message, searches long-term memory by meaning, and injects relevant memories into the system prompt.">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentLongTermMemoryAdvisor">&#9654;</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">&#9654;</button>
          </div>
        </li>
      </ol>

      <h4 ref="testStep">Step 15: Restart and Test</h4>
      <ol start="34">
        <li>Go to the <a :href="workshopHubUrl" target="_blank" class="link">Workshop Hub</a> and rebuild the app</li>
        <li><router-link to="/demo" class="link">Go to Demo</router-link> to test your implementation!</li>
      </ol>

      <div v-if="workshopComplete" class="completion-banner">
        All steps completed! Restart the app to test your memory implementation.
      </div>

      <div class="reset-section">
        <h4>Reset Workshop</h4>
        <p>Want to start over? Reset all files to their original state.</p>
        <button
          @click="restartLab"
          class="btn btn-warning"
          :disabled="restartingLab"
        >
          {{ restartingLab ? 'Restoring...' : 'Reset Lab' }}
        </button>
      </div>
    </template>
  </WorkshopEditorLayout>
</template>

<script>
import { WorkshopEditorLayout, getBasePath } from '../../../../../workshop-frontend-shared/src/index.js';

const STORAGE_KEY = 'agentMemoryWorkshop';

export default {
  name: 'MemoryEditor',
  components: { WorkshopEditorLayout },
  data() {
    return {
      files: ['AgentMemoryService.java', 'ChatService.java', 'AmsChatMemoryRepository.java'],
      currentFile: null,
      fileContent: '',
      currentStep: 0,
      workshopComplete: false,
      fileContents: {},
      restartingLab: false,
      progressExpanded: false,
      completedStepsSet: new Set()
    };
  },
  async mounted() {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        const data = JSON.parse(saved);
        this.currentStep = data.currentStep || 0;
        this.completedStepsSet = new Set(data.completedSteps || []);
      } catch (e) { console.error('Failed to load saved progress', e); }
    }
    await this.checkWorkshopCompletion();
  },
  computed: {
    basePath() { return getBasePath(); },
    workshopHubUrl() {
      return `${window.location.protocol}//${window.location.hostname}:9000`;
    },
    completedSteps() {
      return this.completedStepsSet.size;
    },
    stepsCompleted() {
      const steps = Array.from(this.completedStepsSet);
      return {
        service: steps.filter(s => s <= 8).length,
        repository: steps.filter(s => s >= 9 && s <= 11).length,
        chat: steps.filter(s => s >= 12 && s <= 14).length
      };
    }
  },
  methods: {
    onFileLoaded({ fileName, content }) {
      this.currentFile = fileName;
      this.fileContent = content;
    },
    saveProgress(step) {
      this.currentStep = step;
      this.completedStepsSet.add(step);
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        currentStep: step,
        completedSteps: Array.from(this.completedStepsSet)
      }));
    },
    async fetchFileContent(fileName) {
      try {
        const url = `${this.basePath}/api/editor/file/${fileName}`;
        const response = await fetch(url, { credentials: 'include' });
        const data = await response.json();
        return data.content || '';
      } catch (e) { return ''; }
    },
    async checkWorkshopCompletion() {
      this.fileContents['AgentMemoryService.java'] = await this.fetchFileContent('AgentMemoryService.java');
      this.fileContents['ChatService.java'] = await this.fetchFileContent('ChatService.java');
      const amsContent = this.fileContents['AgentMemoryService.java'] || '';
      const chatContent = this.fileContents['ChatService.java'] || '';
      const checks = [
        !amsContent.includes('// TODO: Step 0 - Initialize the MemoryAPIClient'),
        !amsContent.includes('// TODO: Step 1 - Use the SDK to get working memory'),
        !amsContent.includes('// TODO: Step 2 - Use the SDK to save working memory'),
        !amsContent.includes('// TODO: Step 3 - Use the SDK to delete working memory'),
        !amsContent.includes('// TODO: Step 4 - Use the SDK to create long-term memories'),
        !amsContent.includes('// TODO: Step 5 - Use the SDK to search long-term memories'),
        !amsContent.includes('// TODO: Step 6 - Use the SDK to get a specific memory'),
        !chatContent.includes('// TODO: Step 7 - Create conversationId'),
        !chatContent.includes('// TODO: Step 8 - Pass conversationId to advisors'),
        !chatContent.includes('// TODO: Step 9 - Add MessageChatMemoryAdvisor'),
        !chatContent.includes('// TODO: Step 10 - Add LongTermMemoryAdvisor')
      ];
      this.workshopComplete = checks.every(Boolean);
      if (this.workshopComplete) {
        this.saveProgress(25);
        this.$nextTick(() => { this.scrollToTestStep(); });
      }
    },
    scrollToTestStep() {
      const testStep = this.$el.querySelector('h4:last-of-type');
      if (testStep) {
        testStep.scrollIntoView({ behavior: 'smooth', block: 'start' });
        this.$refs.layout.showStatus('Workshop complete! Restart the app and test your implementation.', 'success');
      }
    },
    async loadFileStep(fileName, step = null) {
      await this.$refs.layout.loadFile(fileName);
      if (step !== null) this.saveProgress(step);
    },
    saveFile() { this.$refs.layout.save(); },
    getContent() { return this.$refs.layout.getCurrentContent(); },
    setContent(content) { this.$refs.layout.updateContent(content); },
    showStatus(msg, type) { this.$refs.layout.showStatus(msg, type); },

    uncommentClientInit() {
      if (this.currentFile !== 'AgentMemoryService.java') { this.showStatus('Please open AgentMemoryService.java first!', 'error'); return; }
      let c = this.getContent();
      // Step 1: Replace the field declaration placeholder
      c = c.replace(
        /\/\/ TODO: Step 0 - Initialize the client field\n\s*\/\/ private final MemoryAPIClient client;\n\s*private final MemoryAPIClient client = null; \/\/ Placeholder until initialized/,
        `private final MemoryAPIClient client;`
      );
      // Step 2: Replace the constructor initialization
      c = c.replace(
        /\/\/ TODO: Step 0 - Initialize the MemoryAPIClient\n\s*\/\/ this\.client = MemoryAPIClient\.builder\(serverUrl\)\n\s*\/\/\s+\.defaultNamespace\(namespace\)\n\s*\/\/\s+\.timeout\(30\.0\)\n\s*\/\/\s+\.build\(\);/,
        `this.client = MemoryAPIClient.builder(serverUrl)
                .defaultNamespace(namespace)
                .timeout(30.0)
                .build();`
      );
      this.setContent(c);
      this.showStatus('Client initialized! The SDK handles connection pooling and thread safety. Click Save!', 'success');
      this.saveProgress(1);
    },
    uncommentGetWorkingMemory() {
      if (this.currentFile !== 'AgentMemoryService.java') { this.showStatus('Please open AgentMemoryService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 1[^\n]*\n\s*\/\/\s*try \{\n\s*\/\/\s+return client\.workingMemory\(\)\.getWorkingMemory\(sessionId\);\n\s*\/\/\s*\} catch \(Exception e\) \{\n\s*\/\/\s+throw new RuntimeException\("Failed to get working memory", e\);\n\s*\/\/\s*\}\n\s*return null; \/\/ Placeholder/,
        `try {
            return client.workingMemory().getWorkingMemory(sessionId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get working memory", e);
        }`
      );
      this.setContent(c);
      this.showStatus('GET working memory done! Returns messages + auto-generated summary if context was full. Click Save!', 'success');
      this.saveProgress(2);
    },
    uncommentPutWorkingMemory() {
      if (this.currentFile !== 'AgentMemoryService.java') { this.showStatus('Please open AgentMemoryService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 2[^\n]*\n\s*\/\/\s*try \{\n\s*\/\/\s+return client\.workingMemory\(\)\.putWorkingMemory\(sessionId, memory\);\n\s*\/\/\s*\} catch \(Exception e\) \{\n\s*\/\/\s+throw new RuntimeException\("Failed to put working memory", e\);\n\s*\/\/\s*\}\n\s*return null; \/\/ Placeholder/,
        `try {
            return client.workingMemory().putWorkingMemory(sessionId, memory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to put working memory", e);
        }`
      );
      this.setContent(c);
      this.showStatus('PUT working memory done! This replaces the entire session. Click Save!', 'success');
      this.saveProgress(3);
    },
    uncommentDeleteWorkingMemory() {
      if (this.currentFile !== 'AgentMemoryService.java') { this.showStatus('Please open AgentMemoryService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 3[^\n]*\n\s*\/\/\s*try \{\n\s*\/\/\s+return client\.workingMemory\(\)\.deleteWorkingMemory\(sessionId\);\n\s*\/\/\s*\} catch \(Exception e\) \{\n\s*\/\/\s+throw new RuntimeException\("Failed to delete working memory", e\);\n\s*\/\/\s*\}\n\s*return null; \/\/ Placeholder/,
        `try {
            return client.workingMemory().deleteWorkingMemory(sessionId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete working memory", e);
        }`
      );
      this.setContent(c);
      this.showStatus('DELETE working memory done! Session is immediately removed. Click Save!', 'success');
      this.saveProgress(4);
    },
    uncommentCreateLongTermMemory() {
      if (this.currentFile !== 'AgentMemoryService.java') { this.showStatus('Please open AgentMemoryService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 4[^\n]*\n\s*\/\/\s*try \{\n\s*\/\/\s+return client\.longTermMemory\(\)\.createLongTermMemories\(memories\);\n\s*\/\/\s*\} catch \(Exception e\) \{\n\s*\/\/\s+throw new RuntimeException\("Failed to create long-term memory", e\);\n\s*\/\/\s*\}\n\s*return null; \/\/ Placeholder/,
        `try {
            return client.longTermMemory().createLongTermMemories(memories);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create long-term memory", e);
        }`
      );
      this.setContent(c);
      this.showStatus('CREATE long-term memory done! Memories are vectorized and stored for semantic search. Click Save!', 'success');
      this.saveProgress(5);
    },
    uncommentSearchLongTermMemory() {
      if (this.currentFile !== 'AgentMemoryService.java') { this.showStatus('Please open AgentMemoryService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 5[^\n]*\n\s*\/\/\s*try \{\n\s*\/\/\s+SearchRequest request = SearchRequest\.builder\(\)\n\s*\/\/\s+\.text\(text\)\n\s*\/\/\s+\.userId\(userId\)\n\s*\/\/\s+\.limit\(limit\)\n\s*\/\/\s+\.build\(\);\n\s*\/\/\s+return client\.longTermMemory\(\)\.searchLongTermMemories\(request\);\n\s*\/\/\s*\} catch \(Exception e\) \{\n\s*\/\/\s+throw new RuntimeException\("Failed to search long-term memory", e\);\n\s*\/\/\s*\}\n\s*return null; \/\/ Placeholder/,
        `try {
            SearchRequest request = SearchRequest.builder()
                    .text(text)
                    .userId(userId)
                    .limit(limit)
                    .build();
            return client.longTermMemory().searchLongTermMemories(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to search long-term memory", e);
        }`
      );
      this.setContent(c);
      this.showStatus('SEARCH long-term memory done! Uses vector similarity to find memories by meaning, not keywords. Click Save!', 'success');
      this.saveProgress(6);
    },
    uncommentGetMemory() {
      if (this.currentFile !== 'AgentMemoryService.java') { this.showStatus('Please open AgentMemoryService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 6[^\n]*\n\s*\/\/\s*try \{\n\s*\/\/\s+return client\.longTermMemory\(\)\.getLongTermMemory\(memoryId\);\n\s*\/\/\s*\} catch \(Exception e\) \{\n\s*\/\/\s+throw new RuntimeException\("Failed to get memory", e\);\n\s*\/\/\s*\}\n\s*return null; \/\/ Placeholder/,
        `try {
            return client.longTermMemory().getLongTermMemory(memoryId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get memory", e);
        }`
      );
      this.setContent(c);
      this.showStatus('GET memory by ID done! Useful for displaying or editing specific memories. Click Save!', 'success');
      this.saveProgress(7);
    },
    uncommentDeleteMemories() {
      if (this.currentFile !== 'AgentMemoryService.java') { this.showStatus('Please open AgentMemoryService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 7[^\n]*\n\s*\/\/\s*try \{\n\s*\/\/\s+return client\.longTermMemory\(\)\.deleteLongTermMemories\(memoryIds\);\n\s*\/\/\s*\} catch \(Exception e\) \{\n\s*\/\/\s+throw new RuntimeException\("Failed to delete memories", e\);\n\s*\/\/\s*\}\n\s*return null; \/\/ Placeholder/,
        `try {
            return client.longTermMemory().deleteLongTermMemories(memoryIds);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete memories", e);
        }`
      );
      this.setContent(c);
      this.showStatus('DELETE memories done! Bulk deletion for cleanup or GDPR compliance. Click Save!', 'success');
      this.saveProgress(8);
    },
    uncommentHealthCheck() {
      if (this.currentFile !== 'AgentMemoryService.java') { this.showStatus('Please open AgentMemoryService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Implement using client\.health\(\)\.healthCheck\(\)\n\s*\/\/\s*try \{\n\s*\/\/\s+return client\.health\(\)\.healthCheck\(\);\n\s*\/\/\s*\} catch \(Exception e\) \{\n\s*\/\/\s+throw new RuntimeException\("Health check failed", e\);\n\s*\/\/\s*\}\n\s*return null; \/\/ Placeholder/,
        `try {
            return client.health().healthCheck();
        } catch (Exception e) {
            throw new RuntimeException("Health check failed", e);
        }`
      );
      this.setContent(c);
      this.showStatus('Health check done! Use this for monitoring AMS availability. Click Save!', 'success');
      this.saveProgress(9);
    },

    // ==================== AmsChatMemoryRepository Steps ====================

    uncommentGetContextPercentage() {
      if (this.currentFile !== 'AmsChatMemoryRepository.java') { this.showStatus('Please open AmsChatMemoryRepository.java first!', 'error'); return; }
      let c = this.getContent();
      // Match the TODO block and replace with actual implementation
      c = c.replace(
        /\/\/ TODO: Step 9 - Implement getContextPercentage\n\s*\/\/[\s\S]*?return null; \/\/ Placeholder/,
        `try {
            // Use conversationId with model name to get token percentage
            WorkingMemoryResponse response = client.workingMemory().getWorkingMemory(
                    parseSessionId(conversationId),
                    parseUserId(conversationId),
                    namespace,      // namespace
                    "gpt-4o-mini",  // modelName - enables token calculation
                    null            // contextWindowMax
            );
            return response != null ? response.getContextPercentageTotalUsed() : null;
        } catch (Exception e) {
            return null;
        }`
      );
      this.setContent(c);
      this.showStatus('Context percentage done! When >70%, AMS may summarize older messages automatically. Click Save!', 'success');
      this.saveProgress(10);
    },
    uncommentFindByConversationId() {
      if (this.currentFile !== 'AmsChatMemoryRepository.java') { this.showStatus('Please open AmsChatMemoryRepository.java first!', 'error'); return; }
      let c = this.getContent();
      // Match the TODO block and replace with actual implementation
      c = c.replace(
        /\/\/ TODO: Step 10 - Implement findByConversationId\n\s*\/\/[\s\S]*?return List\.of\(\); \/\/ Placeholder/,
        `System.out.println("[AMS] findByConversationId - conversationId: " + conversationId);
        try {
            WorkingMemoryResponse response = client.workingMemory().getWorkingMemory(
                    parseSessionId(conversationId),
                    parseUserId(conversationId),      // userId
                    namespace,           // namespace
                    null,           // modelName
                    null            // contextWindowMax
            );
            if (response == null || response.getMessages() == null) {
                System.out.println("[AMS] No messages found");
                return List.of();
            }

            System.out.println("[AMS] Found " + response.getMessages().size() + " messages in AMS");
            List<Message> messages = new ArrayList<>();
            for (MemoryMessage msg : response.getMessages()) {
                Message springMessage = convertToSpringMessage(msg);
                if (springMessage != null) {
                    messages.add(springMessage);
                }
            }
            return messages;
        } catch (Exception e) {
            System.out.println("[AMS] findByConversationId error: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }`
      );
      this.setContent(c);
      this.showStatus('findByConversationId done! Loads history before each LLM call. Click Save!', 'success');
      this.saveProgress(11);
    },
    uncommentSaveAll() {
      if (this.currentFile !== 'AmsChatMemoryRepository.java') { this.showStatus('Please open AmsChatMemoryRepository.java first!', 'error'); return; }
      let c = this.getContent();
      // Match the TODO block and replace with actual implementation
      c = c.replace(
        /\/\/ TODO: Step 11 - Implement saveAll\n\s*\/\/[\s\S]*?\n[ ]{4}\}/,
        `// Use full conversationId (userId:sessionId) as the AMS session key
        System.out.println("[AMS] saveAll - conversationId: " + conversationId);
        System.out.println("[AMS] Incoming messages count: " + messages.size());



        String userId = parseUserId(conversationId);
        String sessionId = parseSessionId(conversationId);

        try {
            // Check if session exists and load existing messages for deduplication
            List<MemoryMessage> existingMessages = new ArrayList<>();
            boolean sessionExists = false;

            try {
                WorkingMemoryResponse existing = client.workingMemory().getWorkingMemory(
                        sessionId,
                        userId,
                        namespace,
                        null,           // modelName
                        null            // contextWindowMax
                );
                if (existing != null) {
                    sessionExists = true;  // Session exists (even if empty)
                    if (existing.getMessages() != null) {
                        existingMessages.addAll(existing.getMessages());
                    }
                }
            } catch (Exception e) {
                // Session doesn't exist yet
                sessionExists = false;
            }

            System.out.println("[AMS] Session exists: " + sessionExists + ", existing messages: " + existingMessages.size());

            // Filter out messages that already exist
            List<MemoryMessage> newMessages = new ArrayList<>();
            for (Message msg : messages) {
                MemoryMessage amsMsg = convertToAmsMessage(msg);
                if (amsMsg != null && !isDuplicate(amsMsg, existingMessages)) {
                    newMessages.add(amsMsg);
                }
            }

            System.out.println("[AMS] New messages to append: " + newMessages.size());

            if (!newMessages.isEmpty()) {
                boolean isFirstMessage = existingMessages.isEmpty();

                // Always use append - it works reliably
                client.workingMemory().appendMessagesToWorkingMemory(
                        sessionId,
                        newMessages,
                        namespace,      // namespace
                        "gpt-4o-mini",  // modelName
                        null,           // contextWindowMax
                        userId            // userId
                );
                System.out.println("[AMS] Appended " + newMessages.size() + " messages");

                // Set TTL on first message by updating session metadata
                if (isFirstMessage) {
                    try {
                        // Get the session we just created (with namespace)
                        WorkingMemoryResponse current = client.workingMemory().getWorkingMemory(
                                sessionId,
                                userId,
                                namespace,           // namespace
                                null,           // modelName
                                null            // contextWindowMax
                        );
                        if (current != null && current.getMessages() != null) {
                            // Update with TTL
                            WorkingMemory withTtl = WorkingMemory.builder()
                                    .namespace(namespace)
                                    .sessionId(sessionId)
                                    .messages(current.getMessages())
                                    .userId(userId)
                                    .ttlSeconds(DEFAULT_TTL_SECONDS)
                                    .build();
                            client.workingMemory().putWorkingMemory(
                                    sessionId,
                                    withTtl,
                                    userId,
                                    namespace,           // namespace
                                    "gpt-4o-mini",  // modelName
                                    null            // contextWindowMax
                            );
                            System.out.println("[AMS] Set TTL: " + DEFAULT_TTL_SECONDS + "s");
                        }
                    } catch (Exception e) {
                        System.out.println("[AMS] Warning: Could not set TTL: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("[AMS] No new messages to append (all duplicates)");
            }
        } catch (Exception e) {
            System.out.println("[AMS] Save failed: " + e.getMessage());
            throw new RuntimeException("Failed to save conversation to AMS", e);
        }
    }`
      );
      this.setContent(c);
      this.showStatus('saveAll done! Handles deduplication (skips existing messages) and sets TTL on first message. Click Save!', 'success');
      this.saveProgress(12);
    },

    // ==================== Advisor Steps (ChatService.java) ====================

    uncommentAdvisorParams() {
      if (this.currentFile !== 'ChatService.java') { this.showStatus('Please open ChatService.java first!', 'error'); return; }
      let c = this.getContent();
      // Step 12: Add .advisors() to the prompt chain
      c = c.replace(
        /\/\/ TODO: Step 12[^\n]*\n\s*\/\/[^\n]*\n\s*String response = chatClient\.prompt\(\)\n\s*\.system\(SYSTEM_PROMPT\)\n\s*\.user\(request\.message\(\)\)\n\s*\.call\(\)\n\s*\.content\(\);/,
        `String response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(request.message())
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();`
      );
      this.setContent(c);
      this.showStatus('Advisor params done! conversationId flows through the advisor chain for memory isolation. Click Save!', 'success');
      this.saveProgress(13);
    },
    uncommentWorkingMemoryAdvisor() {
      if (this.currentFile !== 'ChatService.java') { this.showStatus('Please open ChatService.java first!', 'error'); return; }
      let c = this.getContent();
      // Step 13: Add MessageChatMemoryAdvisor to ChatClient builder
      // Pattern: 4 comment lines then the actual code line
      c = c.replace(
        /\/\/ TODO: Step 13[^\n]*\n\s*\/\/[^\n]*\n\s*\/\/[^\n]*\n\s*\/\/[^\n]*MessageChatMemoryAdvisor[^\n]*\n\s*var builder = ChatClient\.builder\(chatModel\);/,
        `var builder = ChatClient.builder(chatModel)
                    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());`
      );
      this.setContent(c);
      this.showStatus('Working Memory Advisor added! Auto-loads history before LLM calls, auto-saves after. Click Save!', 'success');
      this.saveProgress(14);
    },
    uncommentLongTermMemoryAdvisor() {
      if (this.currentFile !== 'ChatService.java') { this.showStatus('Please open ChatService.java first!', 'error'); return; }
      let c = this.getContent();
      // Step 14: Uncomment the LongTermMemoryAdvisor block
      c = c.replace(
        /\/\/ TODO: Step 14[^\n]*\n\s*\/\/\s*if \(useLongTermMemory\) \{\n\s*\/\/\s+builder\.defaultAdvisors\(longTermMemoryAdvisor\);\n\s*\/\/\s*\}/,
        `if (useLongTermMemory) {
                builder.defaultAdvisors(longTermMemoryAdvisor);
            }`
      );
      this.setContent(c);
      this.showStatus('Long-Term Memory Advisor added! Searches memories by meaning and injects them into context. Click Save!', 'success');
      this.saveProgress(15);
    },
    async restartLab() {
      if (!confirm('Are you sure you want to reset the lab? This will restore all files to their original state. You will need to rebuild and restart the application after this.')) {
        return;
      }
      this.restartingLab = true;
      try {
        const response = await fetch(`${this.basePath}/api/editor/restore`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include'
        });
        const data = await response.json();
        if (data.success) {
          localStorage.removeItem(STORAGE_KEY);
          this.currentStep = 0;
          this.workshopComplete = false;
          alert(`Lab reset! ${data.filesRestored} files restored.\n\nPlease go to the Workshop Hub and rebuild the app, then refresh this page to start from the beginning.`);
          // Reload current file to show restored content
          if (this.currentFile) {
            await this.$refs.layout.loadFile(this.currentFile);
          }
        } else {
          alert('Error: ' + (data.error || 'Failed to restore files'));
        }
      } catch (error) {
        console.error('Error restarting lab:', error);
        alert('Failed to restore files. Please try again.');
      } finally {
        this.restartingLab = false;
      }
    }
  }
};
</script>

<style scoped>
/* Progress Tracker */
.progress-tracker {
  background: #1a1a2e;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
  border: 1px solid #333;
}
.progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  user-select: none;
}
.progress-title {
  font-weight: bold;
  color: #e0e0e0;
}
.progress-summary {
  color: #10b981;
  font-size: 0.9rem;
}
.progress-toggle {
  color: #888;
  font-size: 1.2rem;
  width: 20px;
  text-align: center;
}
.progress-details {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #333;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.progress-group {
  display: flex;
  align-items: center;
  gap: 8px;
}
.progress-label {
  color: #888;
  font-size: 0.85rem;
}
.progress-count {
  background: #333;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.85rem;
  color: #e0e0e0;
}
.progress-count.complete {
  background: #10b981;
  color: white;
}
.progress-bar {
  margin-top: 12px;
  height: 4px;
  background: #333;
  border-radius: 2px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981, #3b82f6);
  border-radius: 2px;
  transition: width 0.3s ease;
}

/* Reset Section */
.reset-section {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #444;
}
.reset-section h4 {
  margin-bottom: 0.5rem;
  color: #b45309;
}
.reset-section p {
  margin-bottom: 1rem;
  color: #999;
  font-size: 0.9rem;
}
</style>

