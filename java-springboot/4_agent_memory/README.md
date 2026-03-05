# Workshop 4: Agent Memory Server

Give your AI agents persistent memory with Redis-powered Agent Memory Server (AMS).

## What You'll Learn

- Working memory for session context (auto-summarization, TTL)
- Long-term memory for persistent facts (semantic search)
- How Redis stores and retrieves AI memories

## Prerequisites

- **OpenAI API Key** - Required for embeddings and chat

## Run with Docker

```bash
# From repository root
cd java-springboot/workshop-hub

# Start Redis + Workshop
docker compose -f docker-compose.local.yml --profile infrastructure up -d
docker compose -f docker-compose.local.yml --profile workshop-4_agent_memory up -d
```

Open **http://localhost:8083**

Enter your OpenAI API key when prompted.

## Workshop Flow

| Stage | What You Do |
|-------|-------------|
| 1. Problem | Understand why AI agents forget between sessions |
| 2. Solution | Implement working + long-term memory |
| 3. Demo | 8 guided tests to verify memory behavior |

## Guided Tests

1. **Working Memory Basics** - AI remembers within a session
2. **Context Window Growth** - Watch context % increase
3. **Store Long-Term Memory** - Save facts that persist
4. **Semantic Search** - Find memories by meaning
5. **Clear Working Memory** - Session resets, facts remain
6. **Cross-Session Recall** - New session retrieves old facts
7. **TTL and Lifecycle** - Working memory expires
8. **Redis Insight** - Explore memory data structures

## Your Tasks

### 1. `AmsChatMemoryRepository.java` - Implement AMS client calls

```java
// Save working memory
client.workingMemory().appendMessagesToWorkingMemory(
    sessionId, messages, namespace, model, null, userId);

// Get working memory
return client.workingMemory()
    .getWorkingMemory(sessionId, userId, namespace, model, windowSize);

// Store long-term memory
MemoryRecord record = MemoryRecord.builder()
    .text(text)
    .userId(userId)
    .memoryType(MemoryType.SEMANTIC)
    .build();
client.longTermMemory().createLongTermMemories(List.of(record));

// Search memories
SearchRequest request = SearchRequest.builder()
    .text(query)
    .userId(userId)
    .limit(5)
    .build();
return client.longTermMemory().searchLongTermMemory(request);
```

### 2. `ChatService.java` - Wire up the advisor

```java
// Build system prompt with retrieved memories
String systemPrompt = buildSystemPromptWithMemories(userId, userMessage);

// Call LLM with memory context
ChatResponse response = chatClient.prompt()
    .system(systemPrompt)
    .user(userMessage)
    .call()
    .chatResponse();
```

## Memory Types

| Type | Purpose | Example |
|------|---------|---------|
| SEMANTIC | Facts and preferences | "User prefers dark mode" |
| EPISODIC | Events with time context | "User went hiking on Saturday" |
| MESSAGE | Conversation excerpts | "User mentioned they work in tech" |

## View in Redis Insight

Open **http://localhost:5540**

**Working Memory:**
```
KEYS working_memory:workshop:*
HGETALL working_memory:workshop:session-default
TTL working_memory:workshop:session-default
```

**Long-Term Memory:**
```
KEYS long_term_memory:workshop:*
FT.INFO idx:long_term_memory:workshop
```

## Key Concepts

- **Working Memory** - Session-scoped, auto-summarizes at 70% context, has TTL (1800s default)
- **Long-Term Memory** - Persistent, vector-indexed for semantic search
- **Namespace** - Isolates memories between applications
- **Session ID** - Groups working memory for a conversation

## Stopping

```bash
docker compose -f docker-compose.local.yml --profile workshop-4_agent_memory down
```

## Resources

- [Agent Memory Server Docs](https://redis.github.io/agent-memory-server/)
- [Java SDK Guide](https://redis.github.io/agent-memory-server/api/sdks/java-sdk/)
- [Redis Vector Search](https://redis.io/docs/latest/develop/interact/search-and-query/query/vector-search/)

