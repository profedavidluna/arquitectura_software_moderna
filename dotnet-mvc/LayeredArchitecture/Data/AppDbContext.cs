using LayeredArchitecture.Data.Entities;
using Microsoft.EntityFrameworkCore;

namespace LayeredArchitecture.Data;

/// <summary>
/// EF Core DbContext for the Product Catalog.
/// This belongs to the DATA LAYER — provides database access infrastructure.
/// </summary>
public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<ProductEntity> Products => Set<ProductEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<ProductEntity>(entity =>
        {
            entity.HasIndex(e => e.Sku).IsUnique();
            entity.HasIndex(e => e.Category);
            entity.HasIndex(e => e.Active);
        });
    }
}
