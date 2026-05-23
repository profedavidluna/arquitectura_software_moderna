using Microsoft.EntityFrameworkCore;

namespace HexagonalArchitecture.Adapters.Output.Persistence;

/// <summary>
/// EF Core DbContext for the Product Catalog.
/// This belongs to the OUTPUT ADAPTER layer — it is a framework/infrastructure concern.
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
