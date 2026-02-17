<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import keycloak from './keycloak'

const isAuthenticated = ref(false)
const username = ref('')
const isAdmin = ref(false)

onMounted(() => {
  updateAuthState()
})

function updateAuthState() {
  isAuthenticated.value = !!keycloak.authenticated
  if (keycloak.tokenParsed) {
    username.value = keycloak.tokenParsed.preferred_username || ''
    const roles: string[] = keycloak.tokenParsed.realm_access?.roles || []
    isAdmin.value = roles.includes('admin')
  }
}

function login() {
  keycloak.login()
}

function logout() {
  keycloak.logout({ redirectUri: window.location.origin })
}

function register() {
  keycloak.register()
}
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
          
          <div class="flex items-center space-x-5">
            <RouterLink to="/" class="text-gray-300 hover:text-white transition-colors font-medium">
              Products
            </RouterLink>

            <!-- Admin link — only visible to admins -->
            <RouterLink
              v-if="isAdmin"
              to="/admin"
              class="text-gray-300 hover:text-white transition-colors font-medium"
            >
              Admin
            </RouterLink>

            <!-- Auth buttons -->
            <template v-if="isAuthenticated">
              <div class="flex items-center space-x-3">
                <div class="flex items-center space-x-2 bg-white/5 border border-white/10 rounded-full px-4 py-1.5">
                  <div class="w-6 h-6 bg-gradient-to-r from-violet-500 to-fuchsia-500 rounded-full flex items-center justify-center">
                    <span class="text-white text-xs font-bold">{{ username.charAt(0).toUpperCase() }}</span>
                  </div>
                  <span class="text-gray-300 text-sm font-medium">{{ username }}</span>
                  <span v-if="isAdmin" class="text-[10px] bg-fuchsia-500/30 text-fuchsia-300 px-1.5 py-0.5 rounded-full font-semibold uppercase tracking-wide">Admin</span>
                </div>
                <button
                  @click="logout"
                  class="text-gray-400 hover:text-white text-sm font-medium transition-colors"
                >
                  Logout
                </button>
              </div>
            </template>
            <template v-else>
              <button
                @click="login"
                class="bg-gradient-to-r from-violet-600 to-fuchsia-600 hover:from-violet-700 hover:to-fuchsia-700 text-white px-5 py-2 rounded-full font-medium transition-all shadow-lg shadow-purple-500/25 hover:shadow-purple-500/40"
              >
                Login
              </button>
              <button
                @click="register"
                class="text-gray-300 hover:text-white text-sm font-medium transition-colors"
              >
                Register
              </button>
            </template>
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
