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

Work through the same three files shown in the in-browser editor:

1. `AgentMemoryService.java`
   Implement the core AMS SDK calls for working memory, long-term memory, and the health check.
2. `AmsChatMemoryRepository.java`
   Bridge Spring AI conversation state to AMS, including context percentage lookup, conversation loading, deduplicated writes, and TTL setup.
3. `ChatService.java`
   Pass the conversation ID into the advisor chain and enable the working-memory and long-term-memory advisors used by the chat demo.

The editor guidance follows this order: SDK client setup first, repository integration second, advisor wiring last.

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
