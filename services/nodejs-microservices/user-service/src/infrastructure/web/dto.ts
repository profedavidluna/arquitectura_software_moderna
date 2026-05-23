/**
 * Data Transfer Objects (DTOs)
 * 
 * DTOs define the shape of data exchanged over the API.
 * They decouple the internal domain model from the external API contract,
 * allowing each to evolve independently.
 */
import { User, Address } from '../../domain/models/User';

export interface CreateUserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  status?: string;
}

export interface CreateAddressRequest {
  street: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  isDefault?: boolean;
}

export interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface AddressResponse {
  id: string;
  userId: string;
  street: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  isDefault: boolean;
}

// Mapper functions - transform domain models to API responses
export function toUserResponse(user: User): UserResponse {
  return {
    id: user.id,
    email: user.email,
    firstName: user.firstName,
    lastName: user.lastName,
    phone: user.phone,
    status: user.status,
    createdAt: user.createdAt.toISOString(),
    updatedAt: user.updatedAt.toISOString(),
  };
}

export function toAddressResponse(address: Address): AddressResponse {
  return {
    id: address.id,
    userId: address.userId,
    street: address.street,
    city: address.city,
    state: address.state,
    zipCode: address.zipCode,
    country: address.country,
    isDefault: address.isDefault,
  };
}
