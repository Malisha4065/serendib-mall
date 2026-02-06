<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
    <!-- Navigation -->
    <nav class="bg-black/30 backdrop-blur-md border-b border-white/10">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">
          <RouterLink to="/" class="flex items-center space-x-3">
            <div class="w-10 h-10 bg-gradient-to-r from-violet-500 to-fuchsia-500 rounded-xl flex items-center justify-center">
              <span class="text-white font-bold text-xl">S</span>
            </div>
            <span class="text-white font-semibold text-xl tracking-tight">SerendibMall</span>
          </RouterLink>
          
          <div class="flex items-center space-x-6">
            <RouterLink to="/" class="text-gray-300 hover:text-white transition-colors font-medium">
              Products
            </RouterLink>
            <RouterLink to="/admin" class="text-gray-300 hover:text-white transition-colors font-medium">
              Admin
            </RouterLink>
            <button class="bg-gradient-to-r from-violet-600 to-fuchsia-600 hover:from-violet-700 hover:to-fuchsia-700 text-white px-5 py-2 rounded-full font-medium transition-all shadow-lg shadow-purple-500/25 hover:shadow-purple-500/40">
              🛒 Cart
            </button>
          </div>
        </div>
      </div>
    </nav>

    <!-- Main Content -->
    <main>
      <RouterView />
    </main>

    <!-- Footer -->
    <footer class="bg-black/30 backdrop-blur-md border-t border-white/10 mt-16">
      <div class="max-w-7xl mx-auto px-4 py-8 text-center text-gray-400">
        <p>© 2026 SerendibMall. Built with Vue.js + GraphQL</p>
      </div>
    </footer>
  </div>
</template>
