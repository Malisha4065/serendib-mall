<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useMutation } from '@vue/apollo-composable'
import { CREATE_PRODUCT } from '../graphql/mutations'
import { RouterLink } from 'vue-router'

const form = reactive({
  name: '',
  description: '',
  price: 0,
  currency: 'USD',
  category: '',
  initialStock: 0
})

const successMessage = ref<string | null>(null)
const errorMessage = ref<string | null>(null)

const { mutate: createProduct, loading } = useMutation(CREATE_PRODUCT)

const handleSubmit = async () => {
  successMessage.value = null
  errorMessage.value = null

  try {
    const input: Record<string, any> = {
      name: form.name,
      description: form.description,
      price: form.price,
      currency: form.currency,
      category: form.category
    }

    if (form.initialStock > 0) {
      input.initialStock = form.initialStock
    }

    const response = await createProduct({ input })

    if (response?.data?.createProduct) {
      const stockInfo = form.initialStock > 0 ? ` with ${form.initialStock} units in stock` : ''
      successMessage.value = `Product "${response.data.createProduct.name}" created${stockInfo} (ID: ${response.data.createProduct.id})`
      // Reset form
      form.name = ''
      form.description = ''
      form.price = 0
      form.category = ''
      form.initialStock = 0
    }
  } catch (e: any) {
    errorMessage.value = e.message || 'Failed to create product'
  }
}
</script>

<template>
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-4xl font-bold text-white mb-2">Admin Panel</h1>
        <p class="text-gray-400">Create and manage products</p>
      </div>
      <RouterLink
        to="/admin/inventory"
        class="bg-white/5 hover:bg-white/10 border border-white/10 text-white px-5 py-3 rounded-xl font-medium transition-all flex items-center gap-2"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
        </svg>
        Manage Inventory
      </RouterLink>
    </div>

    <!-- Create Product Form -->
    <div class="bg-white/5 backdrop-blur-lg rounded-2xl border border-white/10 p-8">
      <h2 class="text-2xl font-semibold text-white mb-6">Create New Product</h2>

      <form @submit.prevent="handleSubmit" class="space-y-6">
        <div>
          <label class="block text-gray-400 mb-2 font-medium">Product Name *</label>
          <input
            v-model="form.name"
            type="text"
            required
            class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-violet-500 transition-colors"
            placeholder="e.g., Wireless Headphones"
          />
        </div>

        <div>
          <label class="block text-gray-400 mb-2 font-medium">Description</label>
          <textarea
            v-model="form.description"
            rows="3"
            class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-violet-500 transition-colors resize-none"
            placeholder="Product description..."
          ></textarea>
        </div>

        <div class="grid grid-cols-3 gap-6">
          <div>
            <label class="block text-gray-400 mb-2 font-medium">Price *</label>
            <div class="relative">
              <span class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500">$</span>
              <input
                v-model.number="form.price"
                type="number"
                step="0.01"
                min="0"
                required
                class="w-full bg-white/5 border border-white/10 rounded-xl pl-8 pr-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-violet-500 transition-colors"
                placeholder="0.00"
              />
            </div>
          </div>

          <div>
            <label class="block text-gray-400 mb-2 font-medium">Category</label>
            <select
              v-model="form.category"
              class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white focus:outline-none focus:border-violet-500 transition-colors"
            >
              <option value="" class="bg-slate-800">Select category</option>
              <option value="Electronics" class="bg-slate-800">Electronics</option>
              <option value="Accessories" class="bg-slate-800">Accessories</option>
              <option value="Wearables" class="bg-slate-800">Wearables</option>
              <option value="Audio" class="bg-slate-800">Audio</option>
            </select>
          </div>

          <div>
            <label class="block text-gray-400 mb-2 font-medium">Initial Stock</label>
            <input
              v-model.number="form.initialStock"
              type="number"
              min="0"
              class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-violet-500 transition-colors"
              placeholder="0"
            />
            <p class="text-xs text-gray-500 mt-1">Leave at 0 to set stock later</p>
          </div>
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-gradient-to-r from-violet-600 to-fuchsia-600 hover:from-violet-700 hover:to-fuchsia-700 disabled:opacity-50 text-white py-4 px-8 rounded-xl font-semibold text-lg transition-all shadow-lg shadow-purple-500/25 hover:shadow-purple-500/40"
        >
          <span v-if="loading" class="flex items-center justify-center">
            <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Creating...
          </span>
          <span v-else>Create Product</span>
        </button>
      </form>

      <!-- Success Message -->
      <div v-if="successMessage" class="mt-6 p-4 bg-green-500/20 border border-green-500/50 rounded-xl">
        <p class="text-green-400 font-medium">✓ {{ successMessage }}</p>
      </div>

      <!-- Error Message -->
      <div v-if="errorMessage" class="mt-6 p-4 bg-red-500/20 border border-red-500/50 rounded-xl">
        <p class="text-red-400 font-medium">✕ {{ errorMessage }}</p>
      </div>
    </div>
  </div>
</template>
