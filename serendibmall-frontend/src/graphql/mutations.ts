import gql from 'graphql-tag'

export const CREATE_PRODUCT = gql`
  mutation CreateProduct($input: CreateProductInput!) {
    createProduct(input: $input) {
      id
      name
      description
      price
    }
  }
`

export const CREATE_ORDER = gql`
  mutation CreateOrder($productId: ID!, $quantity: Int!) {
    createOrder(productId: $productId, quantity: $quantity) {
      id
      status
      productId
    }
  }
`

export const SET_STOCK = gql`
  mutation SetStock($productId: ID!, $quantity: Int!) {
    setStock(productId: $productId, quantity: $quantity) {
      productId
      productName
      quantity
      isAvailable
    }
  }
`

export const UPDATE_STOCK = gql`
  mutation UpdateStock($productId: ID!, $delta: Int!) {
    updateStock(productId: $productId, delta: $delta) {
      productId
      productName
      quantity
      isAvailable
    }
  }
`
