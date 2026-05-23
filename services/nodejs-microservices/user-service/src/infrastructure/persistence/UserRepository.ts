/**
 * User Repository
 * 
 * Implements the Repository Pattern - abstracts database access
 * behind a clean interface. This allows swapping the database
 * implementation without changing business logic.
 */
import { pool } from './database';
import { User, Address, UserStatus } from '../../domain/models/User';

export class UserRepository {
  async create(user: Omit<User, 'createdAt' | 'updatedAt'>): Promise<User> {
    const query = `
      INSERT INTO users (id, email, password_hash, first_name, last_name, phone, status)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      RETURNING *
    `;
    const values = [user.id, user.email, user.passwordHash, user.firstName, user.lastName, user.phone, user.status];
    const result = await pool.query(query, values);
    return this.mapToUser(result.rows[0]);
  }

  async findById(id: string): Promise<User | null> {
    const query = 'SELECT * FROM users WHERE id = $1';
    const result = await pool.query(query, [id]);
    return result.rows.length > 0 ? this.mapToUser(result.rows[0]) : null;
  }

  async findByEmail(email: string): Promise<User | null> {
    const query = 'SELECT * FROM users WHERE email = $1';
    const result = await pool.query(query, [email]);
    return result.rows.length > 0 ? this.mapToUser(result.rows[0]) : null;
  }

  async update(id: string, fields: Partial<User>): Promise<User | null> {
    const setClauses: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    if (fields.firstName) { setClauses.push(`first_name = $${paramIndex++}`); values.push(fields.firstName); }
    if (fields.lastName) { setClauses.push(`last_name = $${paramIndex++}`); values.push(fields.lastName); }
    if (fields.phone !== undefined) { setClauses.push(`phone = $${paramIndex++}`); values.push(fields.phone); }
    if (fields.status) { setClauses.push(`status = $${paramIndex++}`); values.push(fields.status); }

    setClauses.push(`updated_at = CURRENT_TIMESTAMP`);
    values.push(id);

    const query = `UPDATE users SET ${setClauses.join(', ')} WHERE id = $${paramIndex} RETURNING *`;
    const result = await pool.query(query, values);
    return result.rows.length > 0 ? this.mapToUser(result.rows[0]) : null;
  }

  async delete(id: string): Promise<boolean> {
    const query = 'DELETE FROM users WHERE id = $1';
    const result = await pool.query(query, [id]);
    return (result.rowCount ?? 0) > 0;
  }

  async findAll(offset: number, limit: number): Promise<User[]> {
    const query = 'SELECT * FROM users ORDER BY created_at DESC LIMIT $1 OFFSET $2';
    const result = await pool.query(query, [limit, offset]);
    return result.rows.map(this.mapToUser);
  }

  // Address operations
  async createAddress(address: Omit<Address, 'createdAt'>): Promise<Address> {
    // If this is the default address, unset other defaults
    if (address.isDefault) {
      await pool.query('UPDATE addresses SET is_default = FALSE WHERE user_id = $1', [address.userId]);
    }

    const query = `
      INSERT INTO addresses (id, user_id, street, city, state, zip_code, country, is_default)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
      RETURNING *
    `;
    const values = [address.id, address.userId, address.street, address.city, address.state, address.zipCode, address.country, address.isDefault];
    const result = await pool.query(query, values);
    return this.mapToAddress(result.rows[0]);
  }

  async findAddressesByUserId(userId: string): Promise<Address[]> {
    const query = 'SELECT * FROM addresses WHERE user_id = $1 ORDER BY is_default DESC, created_at DESC';
    const result = await pool.query(query, [userId]);
    return result.rows.map(this.mapToAddress);
  }

  async deleteAddress(userId: string, addressId: string): Promise<boolean> {
    const query = 'DELETE FROM addresses WHERE id = $1 AND user_id = $2';
    const result = await pool.query(query, [addressId, userId]);
    return (result.rowCount ?? 0) > 0;
  }

  private mapToUser(row: any): User {
    return {
      id: row.id,
      email: row.email,
      passwordHash: row.password_hash,
      firstName: row.first_name,
      lastName: row.last_name,
      phone: row.phone,
      status: row.status as UserStatus,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    };
  }

  private mapToAddress(row: any): Address {
    return {
      id: row.id,
      userId: row.user_id,
      street: row.street,
      city: row.city,
      state: row.state,
      zipCode: row.zip_code,
      country: row.country,
      isDefault: row.is_default,
      createdAt: row.created_at,
    };
  }
}
