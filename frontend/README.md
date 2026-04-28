# Platform Control Plane Frontend

Vue frontend for the Redis workshop control plane. It is built into `platform_control_plane` and uses the control plane APIs directly.

## Development

```bash
cd frontend
npm install
npm run serve
```

The development server runs on `http://localhost:3000` and proxies `/api` and `/session` to `http://localhost:9001`.

## Build

```bash
cd frontend
npm run build
```

The Docker build for `java-springboot/platform_control_plane` copies `frontend/dist` into the Spring Boot static resources during image build.

## API Surface

The frontend uses:

1. `GET /api/catalog/workshops`
2. `GET /api/sessions`
3. `POST /api/sessions`
4. `POST /api/sessions/{sessionId}/restart`
5. `DELETE /api/sessions/{sessionId}`
