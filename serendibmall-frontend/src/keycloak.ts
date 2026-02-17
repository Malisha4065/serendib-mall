import Keycloak from 'keycloak-js'

const keycloak = new Keycloak({
    url: 'http://localhost:9090',
    realm: 'serendibmall',
    clientId: 'serendibmall-frontend'
})

export default keycloak
