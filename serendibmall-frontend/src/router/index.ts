import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ProductView from '../views/ProductView.vue'
import AdminView from '../views/AdminView.vue'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            name: 'home',
            component: HomeView
        },
        {
            path: '/product/:id',
            name: 'product',
            component: ProductView
        },
        {
            path: '/admin',
            name: 'admin',
            component: AdminView
        }
    ]
})

export default router
