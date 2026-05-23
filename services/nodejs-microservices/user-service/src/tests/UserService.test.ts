/**
 * User Service Unit Tests
 * 
 * Tests business logic in isolation by mocking infrastructure dependencies.
 * This demonstrates the testability benefit of the Service Layer pattern.
 */
import { UserServiceImpl } from '../application/UserServiceImpl';
import { UserRepository } from '../infrastructure/persistence/UserRepository';
import { KafkaProducer } from '../infrastructure/messaging/KafkaProducer';
import { UserStatus } from '../domain/models/User';

// Mock dependencies
jest.mock('../infrastructure/persistence/UserRepository');
jest.mock('../infrastructure/messaging/KafkaProducer');

describe('UserServiceImpl', () => {
  let userService: UserServiceImpl;
  let mockRepository: jest.Mocked<UserRepository>;
  let mockKafkaProducer: jest.Mocked<KafkaProducer>;

  beforeEach(() => {
    mockRepository = new UserRepository() as jest.Mocked<UserRepository>;
    mockKafkaProducer = new KafkaProducer() as jest.Mocked<KafkaProducer>;
    userService = new UserServiceImpl(mockRepository, mockKafkaProducer);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('createUser', () => {
    it('should create a user and publish event', async () => {
      const dto = {
        email: 'test@example.com',
        password: 'password123',
        firstName: 'John',
        lastName: 'Doe',
      };

      mockRepository.findByEmail.mockResolvedValue(null);
      mockRepository.create.mockResolvedValue({
        id: 'user-1',
        email: dto.email,
        passwordHash: 'hashed',
        firstName: dto.firstName,
        lastName: dto.lastName,
        status: UserStatus.ACTIVE,
        createdAt: new Date(),
        updatedAt: new Date(),
      });
      mockKafkaProducer.publish.mockResolvedValue();

      const result = await userService.createUser(dto);

      expect(result.email).toBe(dto.email);
      expect(result.firstName).toBe(dto.firstName);
      expect(result.status).toBe(UserStatus.ACTIVE);
      expect(mockRepository.create).toHaveBeenCalledTimes(1);
      expect(mockKafkaProducer.publish).toHaveBeenCalledWith(
        'user-events',
        expect.objectContaining({ type: 'USER_CREATED' })
      );
    });

    it('should throw error if email already exists', async () => {
      mockRepository.findByEmail.mockResolvedValue({
        id: 'existing-user',
        email: 'test@example.com',
        passwordHash: 'hash',
        firstName: 'Existing',
        lastName: 'User',
        status: UserStatus.ACTIVE,
        createdAt: new Date(),
        updatedAt: new Date(),
      });

      await expect(
        userService.createUser({
          email: 'test@example.com',
          password: 'pass',
          firstName: 'John',
          lastName: 'Doe',
        })
      ).rejects.toThrow('User with this email already exists');
    });
  });

  describe('getUserById', () => {
    it('should return user when found', async () => {
      const mockUser = {
        id: 'user-1',
        email: 'test@example.com',
        passwordHash: 'hash',
        firstName: 'John',
        lastName: 'Doe',
        status: UserStatus.ACTIVE,
        createdAt: new Date(),
        updatedAt: new Date(),
      };
      mockRepository.findById.mockResolvedValue(mockUser);

      const result = await userService.getUserById('user-1');

      expect(result).toEqual(mockUser);
      expect(mockRepository.findById).toHaveBeenCalledWith('user-1');
    });

    it('should return null when user not found', async () => {
      mockRepository.findById.mockResolvedValue(null);

      const result = await userService.getUserById('nonexistent');

      expect(result).toBeNull();
    });
  });

  describe('deleteUser', () => {
    it('should delete user and publish event', async () => {
      mockRepository.delete.mockResolvedValue(true);
      mockKafkaProducer.publish.mockResolvedValue();

      const result = await userService.deleteUser('user-1');

      expect(result).toBe(true);
      expect(mockKafkaProducer.publish).toHaveBeenCalledWith(
        'user-events',
        expect.objectContaining({ type: 'USER_DELETED' })
      );
    });

    it('should return false when user does not exist', async () => {
      mockRepository.delete.mockResolvedValue(false);

      const result = await userService.deleteUser('nonexistent');

      expect(result).toBe(false);
      expect(mockKafkaProducer.publish).not.toHaveBeenCalled();
    });
  });

  describe('addAddress', () => {
    it('should add address for existing user', async () => {
      mockRepository.findById.mockResolvedValue({
        id: 'user-1',
        email: 'test@example.com',
        passwordHash: 'hash',
        firstName: 'John',
        lastName: 'Doe',
        status: UserStatus.ACTIVE,
        createdAt: new Date(),
        updatedAt: new Date(),
      });
      mockRepository.createAddress.mockResolvedValue({
        id: 'addr-1',
        userId: 'user-1',
        street: '123 Main St',
        city: 'Springfield',
        state: 'IL',
        zipCode: '62701',
        country: 'US',
        isDefault: true,
        createdAt: new Date(),
      });
      mockKafkaProducer.publish.mockResolvedValue();

      const result = await userService.addAddress('user-1', {
        street: '123 Main St',
        city: 'Springfield',
        state: 'IL',
        zipCode: '62701',
        country: 'US',
        isDefault: true,
      });

      expect(result.street).toBe('123 Main St');
      expect(result.isDefault).toBe(true);
    });

    it('should throw error if user not found', async () => {
      mockRepository.findById.mockResolvedValue(null);

      await expect(
        userService.addAddress('nonexistent', {
          street: '123 Main St',
          city: 'Springfield',
          state: 'IL',
          zipCode: '62701',
          country: 'US',
        })
      ).rejects.toThrow('User not found');
    });
  });
});
