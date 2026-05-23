using Microsoft.EntityFrameworkCore;

namespace CleanArchitecture.Framework.Persistence;

/// <summary>
/// EF Core DbContext for the Product Catalog.
/// This belongs to the FRAMEWORK layer (outermost) — it is a persistence infrastructure detail.
/// </summary>
public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<ProductJpaEntity> Products => Set<ProductJpaEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<ProductJpaEntity>(entity =>
        {
            entity.HasIndex(e => e.Sku).IsUnique();
            entity.HasIndex(e => e.Category);
            entity.HasIndex(e => e.Active);
        });
    }
}
