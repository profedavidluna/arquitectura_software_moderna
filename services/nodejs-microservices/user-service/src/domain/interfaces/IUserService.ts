/**
 * User Service Interface
 * 
 * Defines the contract for user business operations.
 * This interface lives in the domain layer, ensuring that
 * the application layer depends on abstractions, not implementations
 * (Dependency Inversion Principle).
 */
import { User, Address } from '../models/User';

export interface CreateUserDTO {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface UpdateUserDTO {
  firstName?: string;
  lastName?: string;
  phone?: string;
  status?: string;
}

export interface CreateAddressDTO {
  street: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  isDefault?: boolean;
}

export interface IUserService {
  createUser(dto: CreateUserDTO): Promise<User>;
  getUserById(id: string): Promise<User | null>;
  getUserByEmail(email: string): Promise<User | null>;
  updateUser(id: string, dto: UpdateUserDTO): Promise<User | null>;
  deleteUser(id: string): Promise<boolean>;
  listUsers(page: number, limit: number): Promise<User[]>;

  // Address operations
  addAddress(userId: string, dto: CreateAddressDTO): Promise<Address>;
  getAddresses(userId: string): Promise<Address[]>;
  deleteAddress(userId: string, addressId: string): Promise<boolean>;
}
