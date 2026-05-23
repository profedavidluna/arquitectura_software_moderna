/**
 * User Service Implementation
 * 
 * Contains the business logic for user operations.
 * This is the Application Layer - it orchestrates domain objects
 * and infrastructure services to fulfill use cases.
 * 
 * Pattern: Service Layer
 * - Encapsulates business logic
 * - Coordinates between repository and messaging
 * - Publishes domain events after state changes
 */
import { v4 as uuidv4 } from 'uuid';
import { User, Address, UserStatus } from '../domain/models/User';
import { IUserService, CreateUserDTO, UpdateUserDTO, CreateAddressDTO } from '../domain/interfaces/IUserService';
import { UserRepository } from '../infrastructure/persistence/UserRepository';
import { KafkaProducer } from '../infrastructure/messaging/KafkaProducer';
import { USER_EVENTS_TOPIC, UserEventType } from '../infrastructure/messaging/events';

export class UserServiceImpl implements IUserService {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly kafkaProducer: KafkaProducer
  ) {}

  async createUser(dto: CreateUserDTO): Promise<User> {
    // Check for duplicate email
    const existing = await this.userRepository.findByEmail(dto.email);
    if (existing) {
      throw new Error('User with this email already exists');
    }

    // In production, use bcrypt for password hashing
    const passwordHash = Buffer.from(dto.password).toString('base64');

    const user: Omit<User, 'createdAt' | 'updatedAt'> = {
      id: uuidv4(),
      email: dto.email,
      passwordHash,
      firstName: dto.firstName,
      lastName: dto.lastName,
      phone: dto.phone,
      status: UserStatus.ACTIVE,
    };

    const created = await this.userRepository.create(user);

    // Publish domain event - other services react to this
    await this.kafkaProducer.publish(USER_EVENTS_TOPIC, {
      type: UserEventType.USER_CREATED,
      data: { id: created.id, email: created.email, firstName: created.firstName, lastName: created.lastName },
    });

    return created;
  }

  async getUserById(id: string): Promise<User | null> {
    return this.userRepository.findById(id);
  }

  async getUserByEmail(email: string): Promise<User | null> {
    return this.userRepository.findByEmail(email);
  }

  async updateUser(id: string, dto: UpdateUserDTO): Promise<User | null> {
    const existing = await this.userRepository.findById(id);
    if (!existing) return null;

    const updated = await this.userRepository.update(id, {
      firstName: dto.firstName,
      lastName: dto.lastName,
      phone: dto.phone,
      status: dto.status as UserStatus,
    });

    if (updated) {
      await this.kafkaProducer.publish(USER_EVENTS_TOPIC, {
        type: UserEventType.USER_UPDATED,
        data: { id: updated.id, email: updated.email, firstName: updated.firstName, lastName: updated.lastName },
      });
    }

    return updated;
  }

  async deleteUser(id: string): Promise<boolean> {
    const deleted = await this.userRepository.delete(id);
    if (deleted) {
      await this.kafkaProducer.publish(USER_EVENTS_TOPIC, {
        type: UserEventType.USER_DELETED,
        data: { id },
      });
    }
    return deleted;
  }

  async listUsers(page: number, limit: number): Promise<User[]> {
    const offset = (page - 1) * limit;
    return this.userRepository.findAll(offset, limit);
  }

  async addAddress(userId: string, dto: CreateAddressDTO): Promise<Address> {
    // Verify user exists
    const user = await this.userRepository.findById(userId);
    if (!user) {
      throw new Error('User not found');
    }

    const address: Omit<Address, 'createdAt'> = {
      id: uuidv4(),
      userId,
      street: dto.street,
      city: dto.city,
      state: dto.state,
      zipCode: dto.zipCode,
      country: dto.country,
      isDefault: dto.isDefault || false,
    };

    const created = await this.userRepository.createAddress(address);

    await this.kafkaProducer.publish(USER_EVENTS_TOPIC, {
      type: UserEventType.ADDRESS_ADDED,
      data: { userId, addressId: created.id },
    });

    return created;
  }

  async getAddresses(userId: string): Promise<Address[]> {
    return this.userRepository.findAddressesByUserId(userId);
  }

  async deleteAddress(userId: string, addressId: string): Promise<boolean> {
    return this.userRepository.deleteAddress(userId, addressId);
  }
}
