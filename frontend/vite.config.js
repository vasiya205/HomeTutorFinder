import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// In development the browser calls /api/... on the Vite server, which forwards to the
// API Gateway. That avoids CORS problems completely. For production builds set
// VITE_API_BASE_URL to the public gateway URL (see docs/DEPLOYMENT.md).
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const gateway = env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080'
  return {
    plugins: [react()],
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': { target: gateway, changeOrigin: true },
      },
    },
    preview: {
      port: 4173,
      proxy: {
        '/api': { target: gateway, changeOrigin: true },
      },
    },
  }
})
