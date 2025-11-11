# vite.config.ts
# PURPOSE: Build and development server configuration for React app
#
# WHY THIS FILE:
# - Vite needs configuration for build optimization, dev server, and plugins
# - Defines TypeScript support, CSS preprocessing, build output
# - Configures environment variables, proxies, aliases
#
# KEY SECTIONS:
# - plugins: Vue/React plugin, auto-imports
# - server: Dev server configuration (port, proxy, HMR)
# - build: Production build optimization (minify, chunk size, etc)
# - resolve: Path aliases (@/ for src/)
# - env: Environment variable handling
#
# RATIONALE:
# - Vite is much faster than webpack (HMR instant vs 5+ seconds)
# - ESM-based development improves debugging
# - Smaller bundle size with automatic code splitting

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],

  server: {
    port: 3000,
    strictPort: false,
    proxy: {
      '/api': {
        target: process.env.VITE_API_URL || 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api'),
      }
    }
  },

  build: {
    target: 'esnext',
    minify: 'terser',
    sourcemap: false,
    outDir: 'dist',
    assetsDir: 'assets',
    cssCodeSplit: true,
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor': ['react', 'react-dom', 'react-router-dom'],
          'utils': ['axios', 'zustand'],
        }
      }
    }
  },

  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    }
  }
})
