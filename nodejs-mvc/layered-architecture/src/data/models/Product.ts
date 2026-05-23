/**
 * @layer Data Layer
 * @description Data model representing a Product as stored in the persistence layer.
 * In Layered Architecture, the data model is defined at the bottom layer
 * and is directly used by the layers above (Business and Presentation).
 *
 * This is simpler but more coupled than Hexagonal or Clean Architecture,
 * as the same model flows through all layers.
 */
export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  stockQuantity: number;
  sku: string;
  active: boolean;
  createdAt: Date;
  updatedAt: Date;
}
