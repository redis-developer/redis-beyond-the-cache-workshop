<template>
  <WorkshopEditorLayout
    ref="layout"
    title="Agent Memory Workshop"
    :files="files"
    @file-loaded="onFileLoaded"
  >
    <template #instructions>
      <div class="memory-editor-instructions">
        <WorkshopContentRenderer
          v-if="content"
          :content="content"
          :show-title="false"
          :show-summary="true"
          :show-stage-title="false"
          :action-handlers="actionHandlers"
          :widgets="widgets"
          :widget-props="widgetProps"
        />

        <div v-else-if="loadingContent" class="content-status">
          Loading workshop content...
        </div>

        <div v-else class="content-status content-status--error">
          {{ loadError }}
        </div>
      </div>
    </template>
  </WorkshopEditorLayout>
</template>

<script>
import {
  WorkshopContentRenderer,
  WorkshopEditorLayout,
  getBasePath,
  getWorkshopHubUrl
} from '../../../../../workshop-frontend-shared/src/index.js';
import MemoryEditorCompletionWidget from '../components/widgets/MemoryEditorCompletionWidget.vue';
import MemoryEditorProgressWidget from '../components/widgets/MemoryEditorProgressWidget.vue';
import { loadWorkshopContentView } from '../utils/workshopContent';

const STORAGE_KEY = 'agentMemoryWorkshop';
const OPEN_FILE_STEPS = {
  'open-agent-memory-service': 0,
  'open-chat-memory-repository': 9,
  'open-chat-service': 12
};
const EDITOR_STEP_HANDLERS = {
  'memory-editor.uncommentClientInit': 'uncommentClientInit',
  'memory-editor.uncommentGetWorkingMemory': 'uncommentGetWorkingMemory',
  'memory-editor.uncommentPutWorkingMemory': 'uncommentPutWorkingMemory',
  'memory-editor.uncommentDeleteWorkingMemory': 'uncommentDeleteWorkingMemory',
  'memory-editor.uncommentCreateLongTermMemory': 'uncommentCreateLongTermMemory',
  'memory-editor.uncommentSearchLongTermMemory': 'uncommentSearchLongTermMemory',
  'memory-editor.uncommentGetMemory': 'uncommentGetMemory',
  'memory-editor.uncommentDeleteMemories': 'uncommentDeleteMemories',
  'memory-editor.uncommentHealthCheck': 'uncommentHealthCheck',
  'memory-editor.uncommentGetContextPercentage': 'uncommentGetContextPercentage',
  'memory-editor.uncommentFindByConversationId': 'uncommentFindByConversationId',
  'memory-editor.uncommentSaveAll': 'uncommentSaveAll',
  'memory-editor.uncommentAdvisorParams': 'uncommentAdvisorParams',
  'memory-editor.uncommentWorkingMemoryAdvisor': 'uncommentWorkingMemoryAdvisor',
  'memory-editor.uncommentLongTermMemoryAdvisor': 'uncommentLongTermMemoryAdvisor'
};

export default {
  name: 'MemoryEditor',
  components: { WorkshopContentRenderer, WorkshopEditorLayout },
  data() {
    return {
      files: ['AgentMemoryService.java', 'AmsChatMemoryRepository.java', 'ChatService.java'],
      currentFile: null,
      fileContent: '',
      currentStep: 0,
      workshopComplete: false,
      fileContents: {},
      restartingLab: false,
      progressExpanded: false,
      completedStepsSet: new Set(),
      content: null,
      loadingContent: true,
      loadError: ''
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
    await this.loadContent();
    await this.checkWorkshopCompletion();
  },
  computed: {
    basePath() { return getBasePath(); },
    workshopHubUrl() {
      return getWorkshopHubUrl();
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
    },
    widgets() {
      return {
        'memory-editor.progress': MemoryEditorProgressWidget,
        'memory-editor.completion-banner': MemoryEditorCompletionWidget
      };
    },
    widgetProps() {
      return {
        'memory-editor.progress': {
          completedSteps: this.completedSteps,
          totalSteps: 15,
          stepsCompleted: this.stepsCompleted,
          expanded: this.progressExpanded,
          toggle: this.toggleProgress
        },
        'memory-editor.completion-banner': {
          visible: this.workshopComplete
        }
      };
    },
    actionHandlers() {
      return {
        applyEditorStep: ({ args }) => this.applyEditorStep(args?.stepId),
        openFile: payload => this.handleOpenFileAction(payload),
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRoute: ({ args }) => {
          if (args?.route) {
            this.$router.push(args.route);
          }
        },
        restartLab: () => this.restartLab(),
        saveFile: () => this.saveFile()
      };
    }
  },
  methods: {
    async loadContent() {
      this.loadingContent = true;
      this.loadError = '';

      try {
        this.content = await loadWorkshopContentView('memory-editor');
      } catch (error) {
        this.loadError = error.message || 'Failed to load workshop content.';
      } finally {
        this.loadingContent = false;
      }
    },
    onFileLoaded({ fileName, content }) {
      this.currentFile = fileName;
      this.fileContent = content;
    },
    toggleProgress() {
      this.progressExpanded = !this.progressExpanded;
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
      this.fileContents['AmsChatMemoryRepository.java'] = await this.fetchFileContent('AmsChatMemoryRepository.java');
      this.fileContents['ChatService.java'] = await this.fetchFileContent('ChatService.java');
      const amsContent = this.fileContents['AgentMemoryService.java'] || '';
      const repositoryContent = this.fileContents['AmsChatMemoryRepository.java'] || '';
      const chatContent = this.fileContents['ChatService.java'] || '';
      const checks = [
        !amsContent.includes('// TODO: Step 0 - Initialize the MemoryAPIClient'),
        !amsContent.includes('// TODO: Step 1 - Implement using client.workingMemory().getWorkingMemory(sessionId)'),
        !amsContent.includes('// TODO: Step 2 - Implement using client.workingMemory().putWorkingMemory(sessionId, memory)'),
        !amsContent.includes('// TODO: Step 3 - Implement using client.workingMemory().deleteWorkingMemory(sessionId)'),
        !amsContent.includes('// TODO: Step 4 - Implement using client.longTermMemory().createLongTermMemories(memories)'),
        !amsContent.includes('// TODO: Step 5 - Implement using client.longTermMemory().searchLongTermMemories(searchRequest)'),
        !amsContent.includes('// TODO: Step 6 - Implement using client.longTermMemory().getLongTermMemory(memoryId)'),
        !amsContent.includes('// TODO: Step 7 - Implement using client.longTermMemory().deleteLongTermMemories(memoryIds)'),
        !amsContent.includes('// TODO: Implement using client.health().healthCheck()'),
        !repositoryContent.includes('// TODO: Step 9 - Implement getContextPercentage'),
        !repositoryContent.includes('// TODO: Step 10 - Implement findByConversationId'),
        !repositoryContent.includes('// TODO: Step 11 - Implement saveAll'),
        !chatContent.includes('// TODO: Step 12 - Add advisor params to pass conversationId'),
        !chatContent.includes('// TODO: Step 13 - Add MessageChatMemoryAdvisor to enable working memory'),
        !chatContent.includes('// TODO: Step 14 - Add LongTermMemoryAdvisor for semantic memory search')
      ];
      this.workshopComplete = checks.every(Boolean);
      if (this.workshopComplete) {
        this.saveProgress(25);
        this.$nextTick(() => { this.scrollToTestStep(); });
      }
    },
    scrollToTestStep() {
      const finalSection = this.$el.querySelector('.memory-editor-instructions .content-section:last-of-type');
      if (finalSection) {
        finalSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        this.$refs.layout.showStatus('Workshop complete! Restart the app and test your implementation.', 'success');
      }
    },
    handleOpenFileAction({ args, item }) {
      const step = item?.itemId ? OPEN_FILE_STEPS[item.itemId] : null;
      return this.loadFileStep(args?.file, step ?? null);
    },
    applyEditorStep(stepId) {
      const handlerName = EDITOR_STEP_HANDLERS[stepId];
      if (handlerName && typeof this[handlerName] === 'function') {
        this[handlerName]();
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
          this.completedStepsSet = new Set();
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
.memory-editor-instructions {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.content-status {
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  background: rgba(59, 130, 246, 0.12);
  color: var(--color-text);
}

.content-status--error {
  background: rgba(239, 68, 68, 0.18);
}

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
