import { createApp, h, provide } from 'vue'
import { createPinia } from 'pinia'
import { DefaultApolloClient } from '@vue/apollo-composable'
import { ApolloClient, InMemoryCache, createHttpLink } from '@apollo/client/core'
import './style.css'
import App from './App.vue'
import router from './router'

// Apollo Client setup
const httpLink = createHttpLink({
    uri: '/graphql',
    credentials: 'include'
})

const apolloClient = new ApolloClient({
    link: httpLink,
    cache: new InMemoryCache(),
    defaultOptions: {
        watchQuery: {
            fetchPolicy: 'cache-and-network'
        }
    }
})

const app = createApp({
    setup() {
        provide(DefaultApolloClient, apolloClient)
    },
    render: () => h(App)
})

app.use(createPinia())
app.use(router)
app.mount('#app')
