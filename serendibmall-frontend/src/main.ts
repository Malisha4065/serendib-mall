import { createApp, h, provide } from 'vue'
import { createPinia } from 'pinia'
import { DefaultApolloClient } from '@vue/apollo-composable'
import { ApolloClient, InMemoryCache, createHttpLink, ApolloLink } from '@apollo/client/core'
import { setContext } from '@apollo/client/link/context'
import './style.css'
import App from './App.vue'
import router from './router'
import keycloak from './keycloak'

// Auth link — attaches Bearer token to every GraphQL request
const authLink = setContext((_, { headers }) => {
    const token = keycloak.token
    return {
        headers: {
            ...headers,
            ...(token ? { Authorization: `Bearer ${token}` } : {})
        }
    }
})

// HTTP link to BFF GraphQL endpoint
const httpLink = createHttpLink({
    uri: '/graphql'
})

const apolloClient = new ApolloClient({
    link: ApolloLink.from([authLink, httpLink]),
    cache: new InMemoryCache(),
    defaultOptions: {
        watchQuery: {
            fetchPolicy: 'cache-and-network'
        }
    }
})

// Initialize Keycloak, then mount the Vue app
// onLoad: 'check-sso' silently checks if user is already logged in without forcing a redirect
keycloak.init({
    onLoad: 'check-sso',
    silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
    pkceMethod: 'S256'
}).then((authenticated) => {
    console.log(`Keycloak initialized. Authenticated: ${authenticated}`)

    // Auto-refresh token before it expires
    setInterval(() => {
        keycloak.updateToken(30).catch(() => {
            console.warn('Token refresh failed')
        })
    }, 10000)

    const app = createApp({
        setup() {
            provide(DefaultApolloClient, apolloClient)
        },
        render: () => h(App)
    })

    app.use(createPinia())
    app.use(router)
    app.mount('#app')
}).catch((err) => {
    console.error('Keycloak init failed:', err)

    // Still mount the app even if Keycloak fails (public pages still work)
    const app = createApp({
        setup() {
            provide(DefaultApolloClient, apolloClient)
        },
        render: () => h(App)
    })

    app.use(createPinia())
    app.use(router)
    app.mount('#app')
})
