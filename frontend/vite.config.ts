import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
import { VitePWA } from "vite-plugin-pwa";

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: "autoUpdate",
      includeAssets: ["favicon.svg", "google-g.svg"],
      manifest: {
        name: "Vocavista",
        short_name: "Vocavista",
        description: "Build your German dictionary with pronunciation media and spaced repetition.",
        start_url: "/",
        scope: "/",
        display: "standalone",
        background_color: "#090b0f",
        theme_color: "#ff9c6a",
        icons: [
          {
            src: "/favicon.svg",
            sizes: "any",
            type: "image/svg+xml",
            purpose: "any maskable"
          }
        ]
      },
      workbox: {
        navigateFallback: "/index.html",
        navigateFallbackDenylist: [/^\/api\//, /^\/actuator\//, /^\/oauth2\//, /^\/login\//, /^\/logout$/],
        runtimeCaching: [
          {
            urlPattern: /\/api\/v1\/media\/pronunciations\/[^/]+\/video$/,
            handler: "CacheFirst",
            options: {
              cacheName: "pronunciation-videos",
              expiration: {
                maxEntries: 40,
                maxAgeSeconds: 60 * 60 * 24 * 30
              },
              cacheableResponse: {
                statuses: [200]
              }
            }
          }
        ]
      }
    })
  ],
  build: {
    outDir: "../backend/target/generated-frontend/static",
    emptyOutDir: true
  },
  server: {
    proxy: {
      "/api": "http://localhost:8080",
      "/login": "http://localhost:8080",
      "/logout": "http://localhost:8080",
      "/oauth2": "http://localhost:8080"
    }
  }
});
