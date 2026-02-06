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
