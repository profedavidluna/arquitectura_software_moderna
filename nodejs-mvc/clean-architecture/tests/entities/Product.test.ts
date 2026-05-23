import { Product } from '../../src/entities/Product';

describe('Product Entity (Clean Architecture)', () => {
  const validProps = {
    id: '123',
    name: 'Test Product',
    description: 'A test product',
    price: 29.99,
    category: 'Electronics',
    stockQuantity: 100,
    sku: 'TST-001',
    active: true,
    createdAt: new Date(),
    updatedAt: new Date(),
  };

  describe('create', () => {
    it('should create a valid product', () => {
      const product = Product.create(validProps);
      expect(product.id).toBe('123');
      expect(product.name).toBe('Test Product');
      expect(product.price).toBe(29.99);
    });

    it('should reject price <= 0', () => {
      expect(() => Product.create({ ...validProps, price: 0 })).toThrow('Price must be greater than 0');
      expect(() => Product.create({ ...validProps, price: -5 })).toThrow('Price must be greater than 0');
    });

    it('should reject negative stock', () => {
      expect(() => Product.create({ ...validProps, stockQuantity: -1 })).toThrow('Stock quantity cannot be negative');
    });

    it('should reject empty name', () => {
      expect(() => Product.create({ ...validProps, name: '' })).toThrow('Product name is required');
    });

    it('should reject empty SKU', () => {
      expect(() => Product.create({ ...validProps, sku: '' })).toThrow('SKU is required');
    });
  });

  describe('decreaseStock', () => {
    it('should decrease stock', () => {
      const product = Product.create(validProps);
      const updated = product.decreaseStock(10);
      expect(updated.stockQuantity).toBe(90);
    });

    it('should throw on insufficient stock', () => {
      const product = Product.create({ ...validProps, stockQuantity: 5 });
      expect(() => product.decreaseStock(10)).toThrow('Insufficient stock');
    });

    it('should throw on zero quantity', () => {
      const product = Product.create(validProps);
      expect(() => product.decreaseStock(0)).toThrow('Quantity must be greater than 0');
    });
  });

  describe('increaseStock', () => {
    it('should increase stock', () => {
      const product = Product.create(validProps);
      const updated = product.increaseStock(50);
      expect(updated.stockQuantity).toBe(150);
    });

    it('should throw on zero quantity', () => {
      const product = Product.create(validProps);
      expect(() => product.increaseStock(0)).toThrow('Quantity must be greater than 0');
    });
  });

  describe('deactivate', () => {
    it('should set active to false', () => {
      const product = Product.create(validProps);
      const deactivated = product.deactivate();
      expect(deactivated.active).toBe(false);
    });
  });

  describe('updateDetails', () => {
    it('should update specified fields', () => {
      const product = Product.create(validProps);
      const updated = product.updateDetails({ name: 'New Name', price: 49.99 });
      expect(updated.name).toBe('New Name');
      expect(updated.price).toBe(49.99);
      expect(updated.description).toBe('A test product'); // unchanged
    });

    it('should validate updated price', () => {
      const product = Product.create(validProps);
      expect(() => product.updateDetails({ price: -1 })).toThrow('Price must be greater than 0');
    });
  });
});
