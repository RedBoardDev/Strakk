import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import basicSsl from '@vitejs/plugin-basic-ssl'
import { VitePWA } from 'vite-plugin-pwa'

// Strakk PWA prototype — premium iOS-feel mockup, mock data only.
// `pnpm dev:https` (vite --mode https) serves over HTTPS (self-signed) so the
// PWA / service worker can be exercised on a phone over the LAN.
export default defineConfig(({ mode }) => ({
  plugins: [
    react(),
    tailwindcss(),
    mode === 'https' ? basicSsl() : null,
    VitePWA({
      registerType: 'autoUpdate',
      // No service worker for the prototype: the self-destroying SW caused an
      // infinite reload loop (unregister → reload → re-register) that showed a
      // blank screen in the installed standalone webclip. `injectRegister: false`
      // means nothing ever registers a SW; the manifest below still makes the app
      // installable. (Revisit if offline support is needed for production.)
      injectRegister: false,
      includeAssets: ['icons/icon-192.png', 'icons/icon-512.png', 'icons/maskable-512.png'],
      manifest: {
        name: 'Strakk',
        short_name: 'Strakk',
        description: 'Premium nutrition & weekly check-in coach',
        lang: 'en',
        dir: 'ltr',
        display: 'standalone',
        orientation: 'portrait',
        start_url: '/',
        scope: '/',
        background_color: '#050918',
        theme_color: '#050918',
        categories: ['health', 'fitness', 'lifestyle'],
        icons: [
          { src: 'icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icons/icon-512.png', sizes: '512x512', type: 'image/png' },
          { src: 'icons/maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,png,woff2}'],
      },
      // SW disabled in dev — the cache was serving stale builds to the installed
      // PWA. Re-enable for production (offline + Android install prompt).
      devOptions: { enabled: false },
    }),
  ],
  server: {
    host: true,
    port: 4173,
    // Accept any Host header (LAN IP, Bonjour name, or the *.trycloudflare.com
    // domain the tunnel serves under).
    allowedHosts: true,
    // `--mode tunnel`: HMR runs over the tunnel's HTTPS/443, so the injected
    // client must connect back on wss://<tunnel-host>:443, not the dev port.
    // This keeps hot-reload working on the phone through the tunnel.
    ...(mode === 'tunnel' ? { hmr: { clientPort: 443 } } : {}),
  },
  // Preview (production build) must accept the LAN IP and the stable Bonjour
  // hostname (RedBoard.local) so an installed webclip survives DHCP IP changes.
  preview: { host: true, port: 4173, allowedHosts: true },
}))
