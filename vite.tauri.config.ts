import { fileURLToPath, URL } from 'node:url'
import { dirname, resolve } from 'node:path'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import IconsResolver from 'unplugin-icons/resolver'
import { PrimeVueResolver } from 'unplugin-vue-components/resolvers'
import Icons from 'unplugin-icons/vite'
import Components from 'unplugin-vue-components/vite'
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite'
import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'
import 'dotenv/config'

const PROJECT_ROOT = dirname(fileURLToPath(import.meta.url))
const APP_ROOT = 'src/ui/setup/pages/SocialGlowz'
const APP_ROOT_ABS = resolve(PROJECT_ROOT, APP_ROOT)

// https://vitejs.dev/config/
export default defineConfig({
  root: APP_ROOT_ABS,
  envDir: PROJECT_ROOT,
  publicDir: resolve(PROJECT_ROOT, 'public'),
  // Tauri expects a fixed port and no browser auto-open
  clearScreen: false,
  server: {
    port: 1420,
    strictPort: true,
    watch: {
      // Watch Tauri src-tauri folder for Rust changes
      ignored: ['**/src-tauri/**'],
    },
  },

  resolve: {
    alias: {
      '@': fileURLToPath(new URL('src', import.meta.url)),
      '~': fileURLToPath(new URL('src', import.meta.url)),
    },
  },

  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern',
      },
    },
  },

  plugins: [
    tailwindcss(),

    vue(),

    VueI18nPlugin({
      include: resolve(PROJECT_ROOT, './src/locales/**'),
      globalSFCScope: true,
      compositionOnly: true,
    }),

    AutoImport({
      imports: [
        'vue',
        'vue-router',
        '@vueuse/core',
        'pinia',
        {
          'vue-i18n': ['useI18n'],
        },
      ],
      dts: resolve(APP_ROOT_ABS, 'types/auto-imports.d.ts'),
      dirs: [
        resolve(PROJECT_ROOT, 'src/composables'),
        resolve(PROJECT_ROOT, 'src/stores'),
        resolve(PROJECT_ROOT, 'src/utils'),
      ],
      vueTemplate: true,
    }),

    Components({
      // Prefer SocialGlowz-local component variants when names overlap.
      dirs: [resolve(PROJECT_ROOT, 'src/components'), resolve(APP_ROOT_ABS, 'components')],
      dts: resolve(APP_ROOT_ABS, 'types/components.d.ts'),
      resolvers: [IconsResolver(), PrimeVueResolver()],
      allowOverrides: true,
    }),

    Icons({
      autoInstall: true,
      compiler: 'vue3',
      scale: 1.5,
    }),
  ],

  build: {
    // Tauri supports ES2021
    target: ['es2021', 'chrome100', 'safari13'],
    minify: !process.env.TAURI_DEBUG ? 'esbuild' : false,
    sourcemap: !!process.env.TAURI_DEBUG,
    outDir: resolve(PROJECT_ROOT, 'dist/tauri'),
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (/\/(vue|vue-router|pinia|@vueuse)\//.test(id)) return 'vendor-vue'
            if (/\/(primevue|primeicons|primeflex)\//.test(id)) return 'vendor-ui'
          }
        },
      },
    },
  },
})
