<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useQuery, useMutation } from '@vue/apollo-composable'
import { GET_PRODUCT } from '../graphql/queries'
import { CREATE_ORDER } from '../graphql/mutations'

const route = useRoute()
const productId = computed(() => route.params.id as string)
const quantity = ref(1)
const orderStatus = ref<string | null>(null)
const orderError = ref<string | null>(null)

const { result, loading, error } = useQuery(GET_PRODUCT, () => ({
  id: productId.value
}))

const product = computed(() => result.value?.product)

const categoryEmojis: Record<string, string> = {
  'Electronics': '💻',
  'Audio': '🎧',
  'Wearables': '⌚',
  'Accessories': '⌨️',
  '': '📦'
}

const getEmoji = (category: string) => categoryEmojis[category || ''] || '📦'

const { mutate: createOrder, loading: ordering } = useMutation(CREATE_ORDER)

const placeOrder = async () => {
  orderStatus.value = null
  orderError.value = null
  
  try {
    const response = await createOrder({
      productId: productId.value,
      quantity: quantity.value
    })
    
    if (response?.data?.createOrder) {
      orderStatus.value = response.data.createOrder.status
    }
  } catch (e: any) {
    orderError.value = e.message || 'Failed to place order'
  }
}
</script>

<template>
  <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
    <!-- Back Button -->
    <RouterLink to="/" class="inline-flex items-center text-gray-400 hover:text-white mb-8 transition-colors">
      <span class="mr-2">←</span> Back to Products
    </RouterLink>

    <!-- Loading State -->
    <div v-if="loading" class="grid grid-cols-1 lg:grid-cols-2 gap-12">
      <div class="bg-white/10 rounded-3xl h-96 animate-pulse"></div>
      <div class="space-y-6 animate-pulse">
        <div class="h-10 bg-white/10 rounded w-3/4"></div>
        <div class="h-4 bg-white/10 rounded w-full"></div>
        <div class="h-4 bg-white/10 rounded w-2/3"></div>
        <div class="h-12 bg-white/10 rounded w-1/3"></div>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="text-center py-16">
      <div class="text-6xl mb-4">⚠️</div>
      <h2 class="text-2xl font-bold text-white mb-2">Product not found</h2>
      <p class="text-gray-400">{{ error.message }}</p>
    </div>

    <!-- Product Content -->
    <div v-else-if="product" class="grid grid-cols-1 lg:grid-cols-2 gap-12">
      <!-- Product Image -->
      <div class="bg-gradient-to-br from-violet-600/20 to-fuchsia-600/20 rounded-3xl p-12 flex items-center justify-center">
        <span class="text-9xl">{{ getEmoji(product.category) }}</span>
      </div>

      <!-- Product Details -->
      <div class="space-y-6">
        <h1 class="text-4xl font-bold text-white mb-4">
          {{ product.name }}
        </h1>
        
        <p class="text-gray-400 text-lg leading-relaxed mb-6">
          {{ product.description }}
        </p>

        <p v-if="product.category" class="text-violet-400/70 text-sm font-medium uppercase tracking-wider">
          {{ product.category }}
        </p>

        <!-- Price -->
        <div class="text-5xl font-bold bg-gradient-to-r from-violet-400 to-fuchsia-400 bg-clip-text text-transparent mb-6">
          ${{ product.price.toFixed(2) }}
        </div>

        <!-- Stock Status -->
        <div class="flex items-center space-x-2 mb-8">
          <span 
            class="w-3 h-3 rounded-full" 
            :class="product.stockLevel === 'IN_STOCK' ? 'bg-green-500 animate-pulse' : product.stockLevel === 'OUT_OF_STOCK' ? 'bg-red-500' : 'bg-yellow-500'"
          ></span>
          <span 
            class="font-medium"
            :class="product.stockLevel === 'IN_STOCK' ? 'text-green-400' : product.stockLevel === 'OUT_OF_STOCK' ? 'text-red-400' : 'text-yellow-400'"
          >
            {{ product.stockLevel }}
          </span>
        </div>

        <!-- Quantity Selector -->
        <div class="flex items-center space-x-4 mb-8">
          <span class="text-gray-400">Quantity:</span>
          <div class="flex items-center bg-white/5 rounded-full">
            <button 
              @click="quantity = Math.max(1, quantity - 1)"
              class="w-10 h-10 text-white hover:bg-white/10 rounded-full transition-colors"
            >−</button>
            <span class="w-12 text-center text-white font-semibold">{{ quantity }}</span>
            <button 
              @click="quantity++"
              class="w-10 h-10 text-white hover:bg-white/10 rounded-full transition-colors"
            >+</button>
          </div>
        </div>

        <!-- Order Button -->
        <button
          @click="placeOrder"
          :disabled="ordering || product.stockLevel === 'OUT_OF_STOCK'"
          class="w-full bg-gradient-to-r from-violet-600 to-fuchsia-600 hover:from-violet-700 hover:to-fuchsia-700 disabled:opacity-50 text-white py-4 px-8 rounded-2xl font-semibold text-lg transition-all shadow-lg shadow-purple-500/25 hover:shadow-purple-500/40"
        >
          <span v-if="ordering" class="flex items-center justify-center">
            <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Processing Order...
          </span>
          <span v-else-if="product.stockLevel === 'OUT_OF_STOCK'">Out of Stock</span>
          <span v-else>Place Order 🛒</span>
        </button>

        <!-- Order Status -->
        <div v-if="orderStatus" class="mt-6 p-4 bg-green-500/20 border border-green-500/50 rounded-xl">
          <p class="text-green-400 font-medium">
            ✓ Order placed successfully! Status: <span class="font-bold">{{ orderStatus }}</span>
          </p>
        </div>

        <div v-if="orderError" class="mt-6 p-4 bg-red-500/20 border border-red-500/50 rounded-xl">
          <p class="text-red-400 font-medium">
            ✕ {{ orderError }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
