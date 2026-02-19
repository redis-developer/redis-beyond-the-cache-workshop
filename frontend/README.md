# Workshop Hub Frontend (Vue.js)

This is the Vue.js frontend for the Redis Workshop Hub, following the patterns from the DevEx Content Tracker.

## Tech Stack

- **Vue 3** with Composition API
- **Vuex** for state management
- **Vue Router** for routing
- **Bootstrap 5** for UI components
- **Custom CSS Design Tokens** for theming

## Project Structure

```
frontend/
├── public/              # Static assets
├── src/
│   ├── assets/          # Images, fonts, etc.
│   ├── components/      # Vue components
│   │   ├── base/        # Base/reusable components
│   │   └── workshop/    # Workshop-specific components
│   ├── router/          # Vue Router configuration
│   ├── services/        # API service layer
│   ├── store/           # Vuex store
│   ├── styles/          # CSS design system
│   ├── utils/           # Utility functions
│   ├── views/           # Page-level components
│   ├── App.vue          # Root component
│   └── main.js          # Application entry point
├── package.json
└── vue.config.js        # Vue CLI configuration
```

## Development Setup

### Prerequisites

- Node.js 16+ and npm
- Spring Boot backend running on port 9090

### Install Dependencies

```bash
cd frontend
npm install
```

### Run Development Server

```bash
npm run serve
```

The development server will start on `http://localhost:3000` and proxy API requests to the Spring Boot backend on port 9090.

## Building for Production

### Build the Vue.js App

```bash
npm run build
```

This creates a `dist/` directory with the production build.

### Copy Build to Spring Boot

After building, copy the contents of `dist/` to the Spring Boot static resources:

```bash
# From the frontend directory
rm -rf ../java-springboot/workshop-hub/src/main/resources/static/vue
mkdir -p ../java-springboot/workshop-hub/src/main/resources/static/vue
cp -r dist/* ../java-springboot/workshop-hub/src/main/resources/static/vue/
```

### Rebuild Spring Boot

```bash
cd ../java-springboot/workshop-hub
./gradlew build
```

Now the Spring Boot application will serve the Vue.js SPA at `http://localhost:9090/`.

## Key Features

### Status Polling

The application polls the backend every 5 seconds to update workshop and infrastructure status.

### Restart State Management

When restarting a workshop, the UI tracks the restart state to prevent confusing button swaps:
- Shows "Restarting..." button (disabled)
- Waits for workshop to go down
- Waits for workshop to come back up
- Only then returns to normal state

### Button Visibility Logic

- **Not deployed**: Shows only "Deploy Workshop" button (full width)
- **Deployed & running**: Shows "Restart", "Stop", and "Open Workshop" buttons
- **Restarting**: Shows only "Restarting..." button (disabled)

### Tooltips

- **Status icon**: Hover to see infrastructure status (Redis, Redis Insight, Workshop App)
- **Info icon**: Hover to see workshop description

## API Integration

The frontend communicates with the Spring Boot backend via REST API:

- `GET /manager/api/status` - Get all service statuses
- `POST /manager/api/infrastructure/start` - Start infrastructure
- `POST /manager/api/infrastructure/stop` - Stop infrastructure
- `POST /manager/api/workshop/{id}/start` - Start workshop
- `POST /manager/api/workshop/{id}/stop` - Stop workshop
- `POST /manager/api/workshop/{id}/restart` - Restart workshop

## Design System

The application uses a dark theme with design tokens defined in `src/styles/tokens.css`:

- **Colors**: Teal/cyan primary, dark backgrounds, semantic status colors
- **Typography**: Inter font family, consistent sizing scale
- **Spacing**: 4px-based spacing scale
- **Components**: Consistent button styles, card styles, tooltips

## Development Notes

- The Vue.js dev server proxies `/manager/api` requests to `http://localhost:9090`
- Hot module replacement is enabled for fast development
- All API calls include `credentials: 'include'` for session management
- Status indicators use pulsing animations for "starting" and "restarting" states

