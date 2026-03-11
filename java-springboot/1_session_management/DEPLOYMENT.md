# Session Management Workshop - Deployment Scenarios

This document explains how to run the workshop in different deployment scenarios.

## Scenario 1: Through Workshop Hub (Container)

**Use Case**: Production deployment with Docker-in-Docker

**How to Run**:
```bash
# From repository root
docker-compose up -d workshop-hub

# Wait for Hub to start, then visit:
# http://localhost:9000
# Click "Open Workshop" for Session Management
```

**Configuration**:
- Workshop accessed at: `/workshop/session-management/`
- Frontend built with: `VUE_APP_BASE_PATH=/workshop/session-management/`
- API calls go to: `/workshop/session-management/api/*`
- Redis Insight at: `http://localhost:5540`

---

## Scenario 2: Workshop Hub Running Locally

**Use Case**: Development of the Hub itself

**How to Run**:
```bash
# IMPORTANT: Run from the repository root, not from java-springboot directory
cd /path/to/redis-beyond-the-cache-workshop

# Build and run the Hub locally
./gradlew :workshop-hub:bootRun

# Visit: http://localhost:9000
```

**Configuration**:
- Same as Scenario 1, but Hub runs on host
- Workshops still run in Docker containers
- Uses `docker-compose.internal.yml` to start workshops
- Internal compose file path: `java-springboot/workshop-hub/docker-compose.internal.yml` (relative to repo root)
- Workshop root path: `.` (current directory = repository root)

**Important Notes**:
- ⚠️ **Must run from repository root** (where `docker-compose.yml` is located)
- Make sure Docker is running on your host machine
- The Hub will use your host's Docker daemon to start workshop containers

---

## Scenario 3: Workshop Standalone (Container)

**Use Case**: Running a single workshop in Docker without the Hub

**How to Run**:
```bash
# From the workshop directory
cd java-springboot/1_session_management
docker-compose up -d

# Visit: http://localhost:18080
```

**Configuration**:
- Workshop accessed at: `/`
- Frontend built with: `VUE_APP_BASE_PATH=/` (via build arg)
- API calls go to: `/api/*`
- Redis Insight at: `http://localhost:5540`

**Important**: The Dockerfile accepts a build argument to set the base path:
```yaml
build:
  args:
    VUE_APP_BASE_PATH: /  # Standalone mode
```

---

## Scenario 4: Workshop Running Locally

**Use Case**: Development of the workshop itself

**How to Run**:

### Option A: Spring Boot Only (Recommended for Backend Development)
```bash
# Start Redis first
docker run -d -p 6379:6379 redis:latest

# Run Spring Boot
cd java-springboot
./gradlew :1_session_management:bootRun

# Visit: http://localhost:18080
```

### Option B: Vue Dev Server + Spring Boot (Recommended for Frontend Development)
```bash
# Terminal 1: Start Redis
docker run -d -p 6379:6379 redis:latest

# Terminal 2: Start Spring Boot
cd java-springboot
./gradlew :1_session_management:bootRun

# Terminal 3: Start Vue dev server
cd java-springboot/1_session_management_frontend/frontend
npm install
npm run serve

# Visit: http://localhost:9080 (Vue dev server with hot reload)
```

**Configuration**:
- Frontend uses: `VUE_APP_BASE_PATH=/` (default in vue.config.js)
- Vue dev server proxies `/api`, `/login`, `/logout` to Spring Boot on port 18080
- API calls go to: `/api/*`
- Hot reload enabled for frontend development

---

## Session Validation

All scenarios now include proper session validation:

1. **Navigation Guard** (in `router/index.js`):
   - Checks authentication before every route change
   - Calls `/api/session-info` to verify session
   - Redirects to `/login` if session is invalid (401/403)

2. **Component-Level Check** (in `SessionWelcome.vue`):
   - Checks session when fetching data
   - Handles session expiration during use
   - Redirects to login if needed

3. **Backend Validation**:
   - Returns 401 for invalid sessions on API endpoints
   - Properly configured Spring Security exception handling

---

## Testing Session Validation

To test that session validation works correctly:

1. Login to the workshop
2. Restart the workshop container/application
3. Refresh the page
4. **Expected**: You should be automatically redirected to the login page

This works because:
- The session data is lost on restart (in-memory by default)
- The navigation guard detects the invalid session
- You're redirected to login before the page loads

---

## Build Arguments

The workshop Dockerfile accepts the following build argument:

- `VUE_APP_BASE_PATH`: Sets the base path for the Vue.js frontend
  - Default: `/workshop/session-management/` (for Hub proxy)
  - Standalone: `/` (for direct access)

Example:
```bash
docker build \
  --build-arg VUE_APP_BASE_PATH=/ \
  -f java-springboot/1_session_management/Dockerfile \
  .
```
