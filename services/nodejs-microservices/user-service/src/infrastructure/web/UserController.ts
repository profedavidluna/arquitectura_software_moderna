/**
 * User Controller
 * 
 * Handles HTTP requests and delegates to the service layer.
 * Controllers are thin - they only handle request/response mapping
 * and input validation. Business logic stays in the service layer.
 */
import { Router, Request, Response } from 'express';
import { UserServiceImpl } from '../../application/UserServiceImpl';
import { CreateUserRequest, UpdateUserRequest, CreateAddressRequest, toUserResponse, toAddressResponse } from './dto';

export class UserController {
  public readonly router: Router;

  constructor(private readonly userService: UserServiceImpl) {
    this.router = Router();
    this.setupRoutes();
  }

  private setupRoutes(): void {
    // User CRUD
    this.router.post('/users', this.createUser.bind(this));
    this.router.get('/users', this.listUsers.bind(this));
    this.router.get('/users/:id', this.getUserById.bind(this));
    this.router.put('/users/:id', this.updateUser.bind(this));
    this.router.delete('/users/:id', this.deleteUser.bind(this));

    // Address management
    this.router.post('/users/:id/addresses', this.addAddress.bind(this));
    this.router.get('/users/:id/addresses', this.getAddresses.bind(this));
    this.router.delete('/users/:userId/addresses/:addressId', this.deleteAddress.bind(this));
  }

  private async createUser(req: Request, res: Response): Promise<void> {
    try {
      const dto: CreateUserRequest = req.body;

      // Basic validation
      if (!dto.email || !dto.password || !dto.firstName || !dto.lastName) {
        res.status(400).json({ error: 'Missing required fields: email, password, firstName, lastName' });
        return;
      }

      const user = await this.userService.createUser(dto);
      res.status(201).json(toUserResponse(user));
    } catch (error: any) {
      if (error.message.includes('already exists')) {
        res.status(409).json({ error: error.message });
      } else {
        console.error('[UserController] Create user error:', error);
        res.status(500).json({ error: 'Internal server error' });
      }
    }
  }

  private async getUserById(req: Request, res: Response): Promise<void> {
    try {
      const user = await this.userService.getUserById(req.params.id);
      if (!user) {
        res.status(404).json({ error: 'User not found' });
        return;
      }
      res.json(toUserResponse(user));
    } catch (error) {
      console.error('[UserController] Get user error:', error);
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async listUsers(req: Request, res: Response): Promise<void> {
    try {
      const page = parseInt(req.query.page as string) || 1;
      const limit = parseInt(req.query.limit as string) || 20;
      const users = await this.userService.listUsers(page, limit);
      res.json(users.map(toUserResponse));
    } catch (error) {
      console.error('[UserController] List users error:', error);
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async updateUser(req: Request, res: Response): Promise<void> {
    try {
      const dto: UpdateUserRequest = req.body;
      const user = await this.userService.updateUser(req.params.id, dto);
      if (!user) {
        res.status(404).json({ error: 'User not found' });
        return;
      }
      res.json(toUserResponse(user));
    } catch (error) {
      console.error('[UserController] Update user error:', error);
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async deleteUser(req: Request, res: Response): Promise<void> {
    try {
      const deleted = await this.userService.deleteUser(req.params.id);
      if (!deleted) {
        res.status(404).json({ error: 'User not found' });
        return;
      }
      res.status(204).send();
    } catch (error) {
      console.error('[UserController] Delete user error:', error);
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async addAddress(req: Request, res: Response): Promise<void> {
    try {
      const dto: CreateAddressRequest = req.body;
      if (!dto.street || !dto.city || !dto.state || !dto.zipCode || !dto.country) {
        res.status(400).json({ error: 'Missing required fields: street, city, state, zipCode, country' });
        return;
      }

      const address = await this.userService.addAddress(req.params.id, dto);
      res.status(201).json(toAddressResponse(address));
    } catch (error: any) {
      if (error.message === 'User not found') {
        res.status(404).json({ error: error.message });
      } else {
        console.error('[UserController] Add address error:', error);
        res.status(500).json({ error: 'Internal server error' });
      }
    }
  }

  private async getAddresses(req: Request, res: Response): Promise<void> {
    try {
      const addresses = await this.userService.getAddresses(req.params.id);
      res.json(addresses.map(toAddressResponse));
    } catch (error) {
      console.error('[UserController] Get addresses error:', error);
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async deleteAddress(req: Request, res: Response): Promise<void> {
    try {
      const deleted = await this.userService.deleteAddress(req.params.userId, req.params.addressId);
      if (!deleted) {
        res.status(404).json({ error: 'Address not found' });
        return;
      }
      res.status(204).send();
    } catch (error) {
      console.error('[UserController] Delete address error:', error);
      res.status(500).json({ error: 'Internal server error' });
    }
  }
}
