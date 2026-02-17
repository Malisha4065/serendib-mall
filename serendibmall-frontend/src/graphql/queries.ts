import gql from 'graphql-tag'

export const GET_PRODUCT = gql`
  query GetProduct($id: ID!) {
    product(id: $id) {
      id
      name
      description
      price
      currency
      category
      stockLevel
    }
  }
`

export const SEARCH_PRODUCTS = gql`
  query SearchProducts($query: String, $page: Int, $size: Int) {
    products(query: $query, page: $page, size: $size) {
      products {
        id
        name
        description
        price
        currency
        category
      }
      totalCount
      totalPages
    }
  }
`

export const GET_INVENTORY = gql`
  query GetInventory($page: Int, $size: Int) {
    inventory(page: $page, size: $size) {
      items {
        productId
        productName
        quantity
        isAvailable
      }
      totalCount
    }
  }
`
