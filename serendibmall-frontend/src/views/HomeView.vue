<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// Mock products for now since we don't have a list query
const products = ref([
  { id: '1', name: 'Wireless Headphones', description: 'Premium noise-canceling', price: 299.99, image: '🎧' },
  { id: '2', name: 'Smart Watch', description: 'Fitness tracking & more', price: 449.99, image: '⌚' },
  { id: '3', name: 'Laptop Stand', description: 'Ergonomic aluminum design', price: 89.99, image: '💻' },
  { id: '4', name: 'Mechanical Keyboard', description: 'RGB with Cherry MX switches', price: 159.99, image: '⌨️' },
  { id: '5', name: 'USB-C Hub', description: '7-in-1 multiport adapter', price: 69.99, image: '🔌' },
  { id: '6', name: 'Webcam 4K', description: 'Auto-focus with built-in mic', price: 129.99, image: '📷' },
])

const navigateToProduct = (id: string) => {
  router.push(`/product/${id}`)
}
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
    <!-- Hero Section -->
    <div class="text-center mb-16">
      <h1 class="text-5xl font-bold text-white mb-4">
        Discover <span class="bg-gradient-to-r from-violet-400 to-fuchsia-400 bg-clip-text text-transparent">Premium</span> Products
      </h1>
      <p class="text-gray-400 text-xl max-w-2xl mx-auto">
        Shop the latest tech gear with lightning-fast checkout powered by microservices
      </p>
    </div>

    <!-- Product Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
      <div
        v-for="product in products"
        :key="product.id"
        @click="navigateToProduct(product.id)"
        class="group bg-white/5 backdrop-blur-lg rounded-2xl border border-white/10 p-6 cursor-pointer hover:bg-white/10 hover:border-violet-500/50 transition-all duration-300 hover:-translate-y-2 hover:shadow-xl hover:shadow-purple-500/10"
      >
        <!-- Product Image Placeholder -->
        <div class="h-48 bg-gradient-to-br from-violet-600/20 to-fuchsia-600/20 rounded-xl mb-6 flex items-center justify-center text-6xl">
          {{ product.image }}
        </div>
        
        <h3 class="text-xl font-semibold text-white mb-2 group-hover:text-violet-300 transition-colors">
          {{ product.name }}
        </h3>
        <p class="text-gray-400 mb-4">{{ product.description }}</p>
        
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
  </div>
</template>
