namespace ProductService.Infrastructure.Persistence.Repositories;

using Microsoft.EntityFrameworkCore;
using ProductService.Domain.Interfaces;
using ProductService.Domain.Models;
using ProductService.Infrastructure.Persistence.Entities;

public class ProductRepository : IProductRepository
{
    private readonly AppDbContext _context;

    public ProductRepository(AppDbContext context) => _context = context;

    public async Task<Product?> GetByIdAsync(Guid id)
    {
        var entity = await _context.Products.FindAsync(id);
        return entity == null ? null : MapToDomain(entity);
    }

    public async Task<IEnumerable<Product>> GetAllAsync()
    {
        var entities = await _context.Products.Where(p => p.IsActive).ToListAsync();
        return entities.Select(MapToDomain);
    }

    public async Task<IEnumerable<Product>> SearchAsync(string query)
    {
        var entities = await _context.Products
            .Where(p => p.IsActive && (p.Name.Contains(query) || (p.Description != null && p.Description.Contains(query))))
            .ToListAsync();
        return entities.Select(MapToDomain);
    }

    public async Task<IEnumerable<Product>> GetByCategoryAsync(Guid categoryId)
    {
        var entities = await _context.Products.Where(p => p.CategoryId == categoryId && p.IsActive).ToListAsync();
        return entities.Select(MapToDomain);
    }

    public async Task<Product> CreateAsync(Product product)
    {
        var entity = MapToEntity(product);
        _context.Products.Add(entity);
        await _context.SaveChangesAsync();
        return MapToDomain(entity);
    }

    public async Task<Product> UpdateAsync(Product product)
    {
        var entity = await _context.Products.FindAsync(product.Id)
            ?? throw new KeyNotFoundException($"Product {product.Id} not found");

        entity.Name = product.Name;
        entity.Description = product.Description;
        entity.Price = product.Price;
        entity.CategoryId = product.CategoryId;
        entity.ImageUrl = product.ImageUrl;
        entity.IsActive = product.IsActive;
        entity.UpdatedAt = product.UpdatedAt;

        await _context.SaveChangesAsync();
        return MapToDomain(entity);
    }

    public async Task<bool> DeleteAsync(Guid id)
    {
        var entity = await _context.Products.FindAsync(id);
        if (entity == null) return false;
        _context.Products.Remove(entity);
        await _context.SaveChangesAsync();
        return true;
    }

    public async Task<Category> CreateCategoryAsync(Category category)
    {
        var entity = new CategoryEntity
        {
            Id = category.Id, Name = category.Name,
            Description = category.Description, ParentId = category.ParentId,
            CreatedAt = category.CreatedAt
        };
        _context.Categories.Add(entity);
        await _context.SaveChangesAsync();
        return new Category(entity.Id, entity.Name, entity.Description, entity.ParentId, entity.CreatedAt);
    }

    public async Task<IEnumerable<Category>> GetAllCategoriesAsync()
    {
        var entities = await _context.Categories.ToListAsync();
        return entities.Select(e => new Category(e.Id, e.Name, e.Description, e.ParentId, e.CreatedAt));
    }

    public async Task<Review> AddReviewAsync(Review review)
    {
        var entity = new ReviewEntity
        {
            Id = review.Id, ProductId = review.ProductId, UserId = review.UserId,
            Rating = review.Rating, Comment = review.Comment, CreatedAt = review.CreatedAt
        };
        _context.Reviews.Add(entity);
        await _context.SaveChangesAsync();
        return new Review(entity.Id, entity.ProductId, entity.UserId, entity.Rating, entity.Comment, entity.CreatedAt);
    }

    public async Task<IEnumerable<Review>> GetReviewsByProductAsync(Guid productId)
    {
        var entities = await _context.Reviews.Where(r => r.ProductId == productId).ToListAsync();
        return entities.Select(e => new Review(e.Id, e.ProductId, e.UserId, e.Rating, e.Comment, e.CreatedAt));
    }

    private static Product MapToDomain(ProductEntity e) => new(e.Id, e.Name, e.Description, e.Price, e.CategoryId, e.ImageUrl, e.IsActive, e.CreatedAt, e.UpdatedAt);
    private static ProductEntity MapToEntity(Product p) => new()
    {
        Id = p.Id, Name = p.Name, Description = p.Description, Price = p.Price,
        CategoryId = p.CategoryId, ImageUrl = p.ImageUrl, IsActive = p.IsActive,
        CreatedAt = p.CreatedAt, UpdatedAt = p.UpdatedAt
    };
}
