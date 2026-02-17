<script setup lang="ts">
import { ref, computed } from 'vue'
import { useQuery, useMutation } from '@vue/apollo-composable'
import { GET_INVENTORY } from '../graphql/queries'
import { SET_STOCK, UPDATE_STOCK } from '../graphql/mutations'
import { RouterLink } from 'vue-router'

interface InventoryItem {
  productId: string
  productName: string
  quantity: number
  isAvailable: boolean
}

const currentPage = ref(0)
const pageSize = 20

const { result, loading, refetch } = useQuery(GET_INVENTORY, () => ({
  page: currentPage.value,
  size: pageSize
}))

const { mutate: setStockMutate, loading: settingStock } = useMutation(SET_STOCK)
const { mutate: updateStockMutate, loading: updatingStock } = useMutation(UPDATE_STOCK)

const items = computed<InventoryItem[]>(() => result.value?.inventory?.items ?? [])
const totalCount = computed(() => result.value?.inventory?.totalCount ?? 0)
const totalPages = computed(() => Math.ceil(totalCount.value / pageSize))

// Inline editing state
const editingId = ref<string | null>(null)
const editQuantity = ref(0)

// Restock modal state
const restockId = ref<string | null>(null)
const restockName = ref('')
const restockDelta = ref(0)

const successMessage = ref<string | null>(null)
const errorMessage = ref<string | null>(null)

function clearMessages() {
  setTimeout(() => {
    successMessage.value = null
    errorMessage.value = null
  }, 4000)
}

function startEdit(item: InventoryItem) {
  editingId.value = item.productId
  editQuantity.value = item.quantity
}

function cancelEdit() {
  editingId.value = null
}

async function saveEdit(productId: string) {
  try {
    await setStockMutate({ productId, quantity: editQuantity.value })
    editingId.value = null
    successMessage.value = 'Stock updated successfully'
    await refetch()
    clearMessages()
  } catch (e: any) {
    errorMessage.value = e.message || 'Failed to update stock'
    clearMessages()
  }
}

function openRestock(item: InventoryItem) {
  restockId.value = item.productId
  restockName.value = item.productName
  restockDelta.value = 0
}

function closeRestock() {
  restockId.value = null
}

async function submitRestock() {
  if (!restockId.value || restockDelta.value === 0) return
  try {
    await updateStockMutate({ productId: restockId.value, delta: restockDelta.value })
    const action = restockDelta.value > 0 ? 'Restocked' : 'Adjusted'
    successMessage.value = `${action} ${restockName.value} by ${restockDelta.value > 0 ? '+' : ''}${restockDelta.value} units`
    closeRestock()
    await refetch()
    clearMessages()
  } catch (e: any) {
    errorMessage.value = e.message || 'Failed to update stock'
    clearMessages()
  }
}

function stockStatus(qty: number): { label: string; color: string; bg: string } {
  if (qty === 0) return { label: 'Out of Stock', color: 'text-red-400', bg: 'bg-red-500/20' }
  if (qty <= 10) return { label: 'Low Stock', color: 'text-amber-400', bg: 'bg-amber-500/20' }
  return { label: 'In Stock', color: 'text-emerald-400', bg: 'bg-emerald-500/20' }
}

function prevPage() {
  if (currentPage.value > 0) currentPage.value--
}

function nextPage() {
  if (currentPage.value < totalPages.value - 1) currentPage.value++
}
</script>

<template>
  <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
    <!-- Header -->
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-4xl font-bold text-white mb-2">Inventory Management</h1>
        <p class="text-gray-400">{{ totalCount }} products tracked</p>
      </div>
      <RouterLink
        to="/admin"
        class="bg-white/5 hover:bg-white/10 border border-white/10 text-white px-5 py-3 rounded-xl font-medium transition-all flex items-center gap-2"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
        </svg>
        Back to Admin
      </RouterLink>
    </div>

    <!-- Notifications -->
    <transition name="fade">
      <div v-if="successMessage" class="mb-6 p-4 bg-green-500/20 border border-green-500/50 rounded-xl">
        <p class="text-green-400 font-medium">✓ {{ successMessage }}</p>
      </div>
    </transition>
    <transition name="fade">
      <div v-if="errorMessage" class="mb-6 p-4 bg-red-500/20 border border-red-500/50 rounded-xl">
        <p class="text-red-400 font-medium">✕ {{ errorMessage }}</p>
      </div>
    </transition>

    <!-- Loading State -->
    <div v-if="loading" class="flex items-center justify-center py-20">
      <svg class="animate-spin h-8 w-8 text-violet-500" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
    </div>

    <!-- Inventory Table -->
    <div v-else class="bg-white/5 backdrop-blur-lg rounded-2xl border border-white/10 overflow-hidden">
      <!-- Table Header -->
      <div class="grid grid-cols-12 gap-4 px-6 py-4 border-b border-white/10 text-gray-400 text-sm font-medium uppercase tracking-wider">
        <div class="col-span-5">Product</div>
        <div class="col-span-2 text-center">Stock</div>
        <div class="col-span-2 text-center">Status</div>
        <div class="col-span-3 text-right">Actions</div>
      </div>

      <!-- Empty State -->
      <div v-if="items.length === 0" class="px-6 py-16 text-center">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 mx-auto text-gray-600 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
        </svg>
        <p class="text-gray-400 text-lg">No inventory entries yet</p>
        <p class="text-gray-500 mt-1">Create products to start tracking inventory</p>
      </div>

      <!-- Inventory Rows -->
      <div
        v-for="item in items"
        :key="item.productId"
        class="grid grid-cols-12 gap-4 px-6 py-4 items-center border-b border-white/5 hover:bg-white/[0.02] transition-colors"
      >
        <!-- Product Name -->
        <div class="col-span-5">
          <p class="text-white font-medium">{{ item.productName }}</p>
          <p class="text-gray-500 text-xs font-mono mt-0.5">{{ item.productId.substring(0, 8) }}...</p>
        </div>

        <!-- Stock Quantity -->
        <div class="col-span-2 text-center">
          <template v-if="editingId === item.productId">
            <input
              v-model.number="editQuantity"
              type="number"
              min="0"
              class="w-24 bg-white/10 border border-violet-500 rounded-lg px-3 py-1.5 text-white text-center text-sm focus:outline-none"
              @keyup.enter="saveEdit(item.productId)"
              @keyup.escape="cancelEdit"
              autofocus
            />
          </template>
          <template v-else>
            <span class="text-white text-xl font-semibold">{{ item.quantity }}</span>
          </template>
        </div>

        <!-- Status Badge -->
        <div class="col-span-2 text-center">
          <span
            :class="[stockStatus(item.quantity).color, stockStatus(item.quantity).bg]"
            class="inline-block px-3 py-1 rounded-full text-xs font-semibold"
          >
            {{ stockStatus(item.quantity).label }}
          </span>
        </div>

        <!-- Actions -->
        <div class="col-span-3 flex items-center justify-end gap-2">
          <template v-if="editingId === item.productId">
            <button
              @click="saveEdit(item.productId)"
              :disabled="settingStock"
              class="bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
            >
              Save
            </button>
            <button
              @click="cancelEdit"
              class="bg-white/5 hover:bg-white/10 text-gray-400 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
            >
              Cancel
            </button>
          </template>
          <template v-else>
            <button
              @click="startEdit(item)"
              class="bg-white/5 hover:bg-white/10 border border-white/10 text-gray-300 hover:text-white px-3 py-1.5 rounded-lg text-sm font-medium transition-all"
            >
              Edit
            </button>
            <button
              @click="openRestock(item)"
              class="bg-violet-600/20 hover:bg-violet-600/30 border border-violet-500/30 text-violet-400 hover:text-violet-300 px-3 py-1.5 rounded-lg text-sm font-medium transition-all"
            >
              Restock
            </button>
          </template>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="px-6 py-4 flex items-center justify-between border-t border-white/10">
        <p class="text-gray-400 text-sm">
          Page {{ currentPage + 1 }} of {{ totalPages }}
        </p>
        <div class="flex gap-2">
          <button
            @click="prevPage"
            :disabled="currentPage === 0"
            class="bg-white/5 hover:bg-white/10 disabled:opacity-30 disabled:cursor-not-allowed border border-white/10 text-white px-4 py-2 rounded-lg text-sm transition-colors"
          >
            Previous
          </button>
          <button
            @click="nextPage"
            :disabled="currentPage >= totalPages - 1"
            class="bg-white/5 hover:bg-white/10 disabled:opacity-30 disabled:cursor-not-allowed border border-white/10 text-white px-4 py-2 rounded-lg text-sm transition-colors"
          >
            Next
          </button>
        </div>
      </div>
    </div>

    <!-- Restock Modal -->
    <teleport to="body">
      <transition name="fade">
        <div v-if="restockId" class="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center" @click.self="closeRestock">
          <div class="bg-slate-800 border border-white/10 rounded-2xl p-8 w-full max-w-md shadow-2xl">
            <h3 class="text-xl font-semibold text-white mb-2">Restock / Adjust</h3>
            <p class="text-gray-400 mb-6">{{ restockName }}</p>

            <div class="space-y-4">
              <div>
                <label class="block text-gray-400 mb-2 text-sm font-medium">Quantity Change</label>
                <input
                  v-model.number="restockDelta"
                  type="number"
                  class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white text-lg font-semibold text-center focus:outline-none focus:border-violet-500 transition-colors"
                  placeholder="0"
                />
                <p class="text-xs text-gray-500 mt-2">
                  Use <span class="text-emerald-400">positive</span> numbers to restock (e.g. +50),
                  <span class="text-red-400">negative</span> to reduce (e.g. -10)
                </p>
              </div>

              <div class="flex gap-3 pt-2">
                <button
                  @click="submitRestock"
                  :disabled="updatingStock || restockDelta === 0"
                  class="flex-1 bg-gradient-to-r from-violet-600 to-fuchsia-600 hover:from-violet-700 hover:to-fuchsia-700 disabled:opacity-50 text-white py-3 rounded-xl font-semibold transition-all"
                >
                  <span v-if="updatingStock">Updating...</span>
                  <span v-else>Apply Change</span>
                </button>
                <button
                  @click="closeRestock"
                  class="bg-white/5 hover:bg-white/10 text-gray-400 py-3 px-6 rounded-xl font-medium transition-colors"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
