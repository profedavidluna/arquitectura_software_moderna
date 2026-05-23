namespace ProductService.Domain.Interfaces;

using ProductService.Domain.Models;

public interface IProductRepository
{
    Task<Product?> GetByIdAsync(Guid id);
    Task<IEnumerable<Product>> GetAllAsync();
    Task<IEnumerable<Product>> SearchAsync(string query);
    Task<IEnumerable<Product>> GetByCategoryAsync(Guid categoryId);
    Task<Product> CreateAsync(Product product);
    Task<Product> UpdateAsync(Product product);
    Task<bool> DeleteAsync(Guid id);
    Task<Category> CreateCategoryAsync(Category category);
    Task<IEnumerable<Category>> GetAllCategoriesAsync();
    Task<Review> AddReviewAsync(Review review);
    Task<IEnumerable<Review>> GetReviewsByProductAsync(Guid productId);
}

public interface IProductService
{
    Task<Product?> GetProductByIdAsync(Guid id);
    Task<IEnumerable<Product>> GetAllProductsAsync();
    Task<IEnumerable<Product>> SearchProductsAsync(string query);
    Task<Product> CreateProductAsync(CreateProductRequest request);
    Task<Product> UpdateProductAsync(Guid id, UpdateProductRequest request);
    Task<bool> DeleteProductAsync(Guid id);
    Task<Category> CreateCategoryAsync(CreateCategoryRequest request);
    Task<IEnumerable<Category>> GetAllCategoriesAsync();
    Task<Review> AddReviewAsync(Guid productId, CreateReviewRequest request);
    Task<IEnumerable<Review>> GetProductReviewsAsync(Guid productId);
}
