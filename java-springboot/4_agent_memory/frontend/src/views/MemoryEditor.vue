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

      <h3>Instructions:</h3>
      <p class="note">Click the play button next to any step to automatically apply that change!</p>

      <h4>Step 0: Initialize the SDK Client</h4>
      <ol>
        <li class="step-with-button">
          <span class="step-content">Click on <code>AgentMemoryService.java</code> tab</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="The service that wraps the Agent Memory Server Java SDK">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="loadFileStep('AgentMemoryService.java', 0)">*</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>MemoryAPIClient</code> initialization in the constructor</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="The SDK uses a builder pattern: MemoryAPIClient.builder(url).defaultNamespace(...).build()">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentClientInit">*</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">*</button>
          </div>
        </li>
      </ol>

      <h4>Step 1: Implement Working Memory - GET</h4>
      <ol start="4">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>getWorkingMemory()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="SDK: client.workingMemory().getWorkingMemory(sessionId)">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentGetWorkingMemory">*</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">*</button>
          </div>
        </li>
      </ol>

      <h4>Step 2: Implement Working Memory - PUT</h4>
      <ol start="6">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>putWorkingMemory()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="SDK: client.workingMemory().putWorkingMemory(sessionId, memory)">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentPutWorkingMemory">*</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">*</button>
          </div>
        </li>
      </ol>

      <h4>Step 3: Implement Working Memory - DELETE</h4>
      <ol start="8">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>deleteWorkingMemory()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="SDK: client.workingMemory().deleteWorkingMemory(sessionId)">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentDeleteWorkingMemory">*</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">*</button>
          </div>
        </li>
      </ol>

      <h4>Step 4: Implement Long-Term Memory - CREATE</h4>
      <ol start="10">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>createLongTermMemory()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="SDK: client.longTermMemory().createLongTermMemories(memories)">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentCreateLongTermMemory">*</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">*</button>
          </div>
        </li>
      </ol>

      <h4>Step 5: Implement Long-Term Memory - SEARCH</h4>
      <ol start="12">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>searchLongTermMemory()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="SDK: client.longTermMemory().searchLongTermMemories(searchRequest) - uses vector similarity!">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentSearchLongTermMemory">*</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">*</button>
          </div>
        </li>
      </ol>

      <h4>Step 6: Implement Long-Term Memory - GET</h4>
      <ol start="14">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>getMemory()</code> implementation</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="SDK: client.longTermMemory().getLongTermMemory(memoryId)">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentGetMemory">*</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">*</button>
          </div>
        </li>
      </ol>

      <h3>Part 2: Spring AI Advisors</h3>
      <p class="note">Advisors intercept chat requests to automatically handle memory loading and retrieval.</p>

      <h4>Step 7: Configure Conversation ID</h4>
      <ol start="16">
        <li class="step-with-button">
          <span class="step-content">Click on <code>ChatService.java</code> tab</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="The chat service wires Spring AI advisors for automatic memory handling">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="loadFileStep('ChatService.java', 7)">*</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>conversationId</code> creation with userId:sessionId format</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Encodes userId into the conversationId so advisors can access it">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentConversationId">*</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">*</button>
          </div>
        </li>
      </ol>

      <h4>Step 8: Pass Conversation ID to Advisors</h4>
      <ol start="19">
        <li class="step-with-button">
          <span class="step-content">Uncomment the advisor params in the chat prompt</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Passes conversationId to advisors via .advisors(spec -> spec.param(...))">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentAdvisorParams">*</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">*</button>
          </div>
        </li>
      </ol>

      <h4>Step 9: Add Working Memory Advisor</h4>
      <ol start="21">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>MessageChatMemoryAdvisor</code> configuration</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Automatically loads/saves conversation history before/after LLM calls">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentWorkingMemoryAdvisor">*</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">*</button>
          </div>
        </li>
      </ol>

      <h4>Step 10: Add Long-Term Memory Advisor</h4>
      <ol start="23">
        <li class="step-with-button">
          <span class="step-content">Uncomment the <code>LongTermMemoryAdvisor</code> configuration</span>
          <div class="button-group">
            <span class="tooltip-wrapper" data-tooltip="Searches AMS for relevant memories using semantic search and injects them into the prompt">
              <span class="info-icon">i</span>
            </span>
            <button class="play-btn" @click="uncommentLongTermMemoryAdvisor">*</button>
          </div>
        </li>
        <li class="step-with-button">
          <span class="step-content">Click <strong>Save Changes</strong></span>
          <div class="button-group">
            <button class="play-btn" @click="saveFile">*</button>
          </div>
        </li>
      </ol>

      <h4 ref="testStep">Step 11: Restart and Test</h4>
      <ol start="25">
        <li>Go to the <a :href="workshopHubUrl" target="_blank" class="link">Workshop Hub</a> and rebuild the app</li>
        <li><router-link to="/demo" class="link">Go to Demo</router-link> to test your implementation!</li>
      </ol>

      <div v-if="workshopComplete" class="completion-banner">
        All steps completed! Restart the app to test your memory implementation.
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
      files: ['AgentMemoryService.java', 'ChatService.java'],
      currentFile: null,
      fileContent: '',
      currentStep: 0,
      workshopComplete: false,
      fileContents: {}
    };
  },
  async mounted() {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        const data = JSON.parse(saved);
        this.currentStep = data.currentStep || 0;
      } catch (e) { console.error('Failed to load saved progress', e); }
    }
    await this.checkWorkshopCompletion();
  },
  computed: {
    basePath() { return getBasePath(); },
    workshopHubUrl() {
      return `${window.location.protocol}//${window.location.hostname}:9000`;
    }
  },
  methods: {
    onFileLoaded({ fileName, content }) {
      this.currentFile = fileName;
      this.fileContent = content;
    },
    saveProgress(step) {
      this.currentStep = step;
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ currentStep: step }));
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
      // Match the TODO block for Step 0
      c = c.replace(
        /\/\/ TODO: Step 0[^\n]*\n\s*\/\/\s*this\.client = MemoryAPIClient\.builder\(serverUrl\)\n\s*\/\/\s*\.defaultNamespace\(namespace\)\n\s*\/\/\s*\.timeout\(30\.0\)\n\s*\/\/\s*\.build\(\);\n\s*this\.client = null;/,
        `this.client = MemoryAPIClient.builder(serverUrl)
                .defaultNamespace(namespace)
                .timeout(30.0)
                .build();`
      );
      this.setContent(c);
      this.showStatus('MemoryAPIClient initialized! Click Save!', 'success');
      this.saveProgress(1);
    },
    uncommentGetWorkingMemory() {
      if (this.currentFile !== 'AgentMemoryService.java') { this.showStatus('Please open AgentMemoryService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 1[^\n]*\n\s*\/\/\s*try \{\n\s*\/\/\s*return client\.workingMemory\(\)\.getWorkingMemory\(sessionId\);\n\s*\/\/\s*\} catch \(Exception e\) \{\n\s*\/\/\s*throw new RuntimeException\("Failed to get working memory", e\);\n\s*\/\/\s*\}\n\s*return null;/,
        `try {
            return client.workingMemory().getWorkingMemory(sessionId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get working memory", e);
        }`
      );
      this.setContent(c);
      this.showStatus('getWorkingMemory() implemented! Click Save!', 'success');
      this.saveProgress(2);
    },
    uncommentPutWorkingMemory() {
      if (this.currentFile !== 'AgentMemoryService.java') { this.showStatus('Please open AgentMemoryService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 2[^\n]*\n\s*\/\/\s*try \{\n\s*\/\/\s*return client\.workingMemory\(\)\.putWorkingMemory\(sessionId, memory\);\n\s*\/\/\s*\} catch \(Exception e\) \{\n\s*\/\/\s*throw new RuntimeException\("Failed to put working memory", e\);\n\s*\/\/\s*\}\n\s*return null;/,
        `try {
            return client.workingMemory().putWorkingMemory(sessionId, memory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to put working memory", e);
        }`
      );
      this.setContent(c);
      this.showStatus('putWorkingMemory() implemented! Click Save!', 'success');
      this.saveProgress(3);
    },
    uncommentDeleteWorkingMemory() {
      if (this.currentFile !== 'AgentMemoryService.java') { this.showStatus('Please open AgentMemoryService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 3[^\n]*\n\s*\/\/\s*try \{\n\s*\/\/\s*return client\.workingMemory\(\)\.deleteWorkingMemory\(sessionId\);\n\s*\/\/\s*\} catch \(Exception e\) \{\n\s*\/\/\s*throw new RuntimeException\("Failed to delete working memory", e\);\n\s*\/\/\s*\}\n\s*return null;/,
        `try {
            return client.workingMemory().deleteWorkingMemory(sessionId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete working memory", e);
        }`
      );
      this.setContent(c);
      this.showStatus('deleteWorkingMemory() implemented! Click Save!', 'success');
      this.saveProgress(4);
    },
    uncommentCreateLongTermMemory() {
      if (this.currentFile !== 'AgentMemoryService.java') { this.showStatus('Please open AgentMemoryService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 4[^\n]*\n\s*\/\/\s*try \{\n\s*\/\/\s*return client\.longTermMemory\(\)\.createLongTermMemories\(memories\);\n\s*\/\/\s*\} catch \(Exception e\) \{\n\s*\/\/\s*throw new RuntimeException\("Failed to create long-term memory", e\);\n\s*\/\/\s*\}\n\s*return null;/,
        `try {
            return client.longTermMemory().createLongTermMemories(memories);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create long-term memory", e);
        }`
      );
      this.setContent(c);
      this.showStatus('createLongTermMemory() implemented! Click Save!', 'success');
      this.saveProgress(5);
    },
    uncommentSearchLongTermMemory() {
      if (this.currentFile !== 'AgentMemoryService.java') { this.showStatus('Please open AgentMemoryService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 5[^\n]*\n\s*\/\/\s*try \{\n\s*\/\/\s*SearchRequest request = SearchRequest\.builder\(\)\n\s*\/\/\s*\.text\(text\)\n\s*\/\/\s*\.userId\(userId\)\n\s*\/\/\s*\.limit\(limit\)\n\s*\/\/\s*\.build\(\);\n\s*\/\/\s*return client\.longTermMemory\(\)\.searchLongTermMemories\(request\);\n\s*\/\/\s*\} catch \(Exception e\) \{\n\s*\/\/\s*throw new RuntimeException\("Failed to search long-term memory", e\);\n\s*\/\/\s*\}\n\s*return null;/,
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
      this.showStatus('searchLongTermMemory() implemented! Click Save!', 'success');
      this.saveProgress(6);
    },
    uncommentGetMemory() {
      if (this.currentFile !== 'AgentMemoryService.java') { this.showStatus('Please open AgentMemoryService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 6[^\n]*\n\s*\/\/\s*try \{\n\s*\/\/\s*return client\.longTermMemory\(\)\.getLongTermMemory\(memoryId\);\n\s*\/\/\s*\} catch \(Exception e\) \{\n\s*\/\/\s*throw new RuntimeException\("Failed to get memory", e\);\n\s*\/\/\s*\}\n\s*return null;/,
        `try {
            return client.longTermMemory().getLongTermMemory(memoryId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get memory", e);
        }`
      );
      this.setContent(c);
      this.showStatus('getMemory() implemented! Click Save!', 'success');
      this.saveProgress(7);
    },

    // ==================== Advisor Steps (ChatService.java) ====================

    uncommentConversationId() {
      if (this.currentFile !== 'ChatService.java') { this.showStatus('Please open ChatService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 7[^\n]*\n\s*\/\/\s*String conversationId = AmsChatMemoryRepository\.createConversationId\(\n\s*\/\/\s*request\.userId\(\) != null \? request\.userId\(\) : "anonymous",\n\s*\/\/\s*request\.sessionId\(\)\n\s*\/\/\s*\);\n\s*String conversationId = request\.sessionId\(\);/,
        `String conversationId = AmsChatMemoryRepository.createConversationId(
                request.userId() != null ? request.userId() : "anonymous",
                request.sessionId()
        );`
      );
      this.setContent(c);
      this.showStatus('ConversationId configured! Click Save!', 'success');
      this.saveProgress(8);
    },
    uncommentAdvisorParams() {
      if (this.currentFile !== 'ChatService.java') { this.showStatus('Please open ChatService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 8[^\n]*\n\s*\/\/[^\n]*\n\s*\/\/\s*String response = chatClient\.prompt\(\)\n\s*\/\/\s*\.system\(SYSTEM_PROMPT\)\n\s*\/\/\s*\.user\(request\.message\(\)\)\n\s*\/\/\s*\.advisors\(spec -> spec\.param\(ChatMemory\.CONVERSATION_ID, conversationId\)\)\n\s*\/\/\s*\.call\(\)\n\s*\/\/\s*\.content\(\);\n\s*String response = chatClient\.prompt\(\)\n\s*\.system\(SYSTEM_PROMPT\)\n\s*\.user\(request\.message\(\)\)\n\s*\.call\(\)\n\s*\.content\(\);/,
        `String response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(request.message())
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();`
      );
      this.setContent(c);
      this.showStatus('Advisor params configured! Click Save!', 'success');
      this.saveProgress(9);
    },
    uncommentWorkingMemoryAdvisor() {
      if (this.currentFile !== 'ChatService.java') { this.showStatus('Please open ChatService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 9[^\n]*\n\s*\/\/[^\n]*\n\s*\/\/[^\n]*\n\s*\/\/\s*var builder = ChatClient\.builder\(chatModel\)\n\s*\/\/\s*\.defaultAdvisors\(MessageChatMemoryAdvisor\.builder\(chatMemory\)\.build\(\)\);\n\s*var builder = ChatClient\.builder\(chatModel\);/,
        `var builder = ChatClient.builder(chatModel)
                    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());`
      );
      this.setContent(c);
      this.showStatus('MessageChatMemoryAdvisor added! Click Save!', 'success');
      this.saveProgress(10);
    },
    uncommentLongTermMemoryAdvisor() {
      if (this.currentFile !== 'ChatService.java') { this.showStatus('Please open ChatService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace(
        /\/\/ TODO: Step 10[^\n]*\n\s*\/\/[^\n]*\n\s*\/\/[^\n]*\n\s*\/\/\s*if \(useLongTermMemory\) \{\n\s*\/\/\s*builder\.defaultAdvisors\(longTermMemoryAdvisor\);\n\s*\/\/\s*\}/,
        `if (useLongTermMemory) {
                builder.defaultAdvisors(longTermMemoryAdvisor);
            }`
      );
      this.setContent(c);
      this.showStatus('LongTermMemoryAdvisor added! Click Save!', 'success');
      this.saveProgress(11);
    }
  }
};
</script>

<style scoped>
/* All styling is now provided by WorkshopEditorLayout */
</style>

