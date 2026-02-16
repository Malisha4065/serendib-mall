<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@vue/apollo-composable'
import { SEARCH_PRODUCTS } from '../graphql/queries'

const router = useRouter()
const searchQuery = ref('')

const { result, loading, error } = useQuery(SEARCH_PRODUCTS, () => ({
  query: searchQuery.value || null,
  page: 0,
  size: 20
}), {
  debounce: 300
})

const products = computed(() => result.value?.products?.products || [])
const totalCount = computed(() => result.value?.products?.totalCount || 0)

const categoryEmojis: Record<string, string> = {
  'Electronics': '💻',
  'Audio': '🎧',
  'Wearables': '⌚',
  'Accessories': '⌨️',
  '': '📦'
}

const getEmoji = (category: string) => categoryEmojis[category] || '📦'

const navigateToProduct = (id: string) => {
  router.push(`/product/${id}`)
}
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
    <!-- Hero Section -->
    <div class="text-center mb-12">
      <h1 class="text-5xl font-bold text-white mb-4">
        Discover <span class="bg-gradient-to-r from-violet-400 to-fuchsia-400 bg-clip-text text-transparent">Premium</span> Products
      </h1>
      <p class="text-gray-400 text-xl max-w-2xl mx-auto mb-8">
        Shop the latest tech gear with lightning-fast checkout powered by microservices
      </p>

      <!-- Search Bar -->
      <div class="max-w-xl mx-auto">
        <div class="relative">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Search products..."
            class="w-full bg-white/5 border border-white/10 rounded-full px-6 py-3 pl-12 text-white placeholder-gray-500 focus:outline-none focus:border-violet-500 transition-colors"
          />
          <span class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500">🔍</span>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
      <div v-for="i in 6" :key="i" class="bg-white/5 rounded-2xl border border-white/10 p-6 animate-pulse">
        <div class="h-48 bg-white/10 rounded-xl mb-6"></div>
        <div class="h-6 bg-white/10 rounded w-3/4 mb-3"></div>
        <div class="h-4 bg-white/10 rounded w-1/2 mb-4"></div>
        <div class="h-8 bg-white/10 rounded w-1/3"></div>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="text-center py-16">
      <div class="text-6xl mb-4">⚠️</div>
      <h2 class="text-2xl font-bold text-white mb-2">Unable to load products</h2>
      <p class="text-gray-400">{{ error.message }}</p>
      <p class="text-gray-500 mt-2 text-sm">Make sure the backend services are running</p>
    </div>

    <!-- Empty State -->
    <div v-else-if="products.length === 0" class="text-center py-16">
      <div class="text-6xl mb-4">🛍️</div>
      <h2 class="text-2xl font-bold text-white mb-2">No products found</h2>
      <p class="text-gray-400">
        {{ searchQuery ? `No results for "${searchQuery}"` : 'Create some products from the Admin panel!' }}
      </p>
    </div>

    <!-- Product Grid -->
    <template v-else>
      <p class="text-gray-400 mb-6">{{ totalCount }} product{{ totalCount !== 1 ? 's' : '' }} found</p>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        <div
          v-for="product in products"
          :key="product.id"
          @click="navigateToProduct(product.id)"
          class="group bg-white/5 backdrop-blur-lg rounded-2xl border border-white/10 p-6 cursor-pointer hover:bg-white/10 hover:border-violet-500/50 transition-all duration-300 hover:-translate-y-2 hover:shadow-xl hover:shadow-purple-500/10"
        >
          <!-- Product Image Placeholder -->
          <div class="h-48 bg-gradient-to-br from-violet-600/20 to-fuchsia-600/20 rounded-xl mb-6 flex items-center justify-center text-6xl">
            {{ getEmoji(product.category) }}
          </div>
          
          <div class="flex items-center gap-2 mb-2">
            <h3 class="text-xl font-semibold text-white group-hover:text-violet-300 transition-colors">
              {{ product.name }}
            </h3>
          </div>
          <p class="text-gray-400 mb-1 line-clamp-2">{{ product.description }}</p>
          <p v-if="product.category" class="text-xs text-violet-400/70 mb-4">{{ product.category }}</p>
          <p v-else class="mb-4"></p>
          
          <div class="flex items-center justify-between">
            <span class="text-2xl font-bold bg-gradient-to-r from-violet-400 to-fuchsia-400 bg-clip-text text-transparent">
              ${{ product.price.toFixed(2) }}
            </span>
            <button class="bg-violet-600/20 hover:bg-violet-600 text-violet-300 hover:text-white px-4 py-2 rounded-full text-sm font-medium transition-all">
              View Details →
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
