import gql from 'graphql-tag'

export const GET_PRODUCT = gql`
  query GetProduct($id: ID!) {
    product(id: $id) {
      id
      name
      description
      price
      stockLevel
    }
  }
`
