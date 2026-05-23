using Microsoft.EntityFrameworkCore;

namespace InventoryService.Infrastructure.Persistence;

/// <summary>
/// Entity Framework Core DbContext for the Inventory Service.
/// 
/// SOA Principle: Database-per-Service - the Inventory Service has its own
/// dedicated database for stock management, independent from other services.
/// </summary>
public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<InventoryEntity> Inventory => Set<InventoryEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<InventoryEntity>(entity =>
        {
            entity.ToTable("inventory");
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Id).HasColumnName("id");
            entity.Property(e => e.ProductId).HasColumnName("product_id");
            entity.Property(e => e.ProductName).HasColumnName("product_name").IsRequired().HasMaxLength(255);
            entity.Property(e => e.Quantity).HasColumnName("quantity");
            entity.Property(e => e.ReservedQuantity).HasColumnName("reserved_quantity");
            entity.Property(e => e.CreatedAt).HasColumnName("created_at");
            entity.Property(e => e.UpdatedAt).HasColumnName("updated_at");

            entity.HasIndex(e => e.ProductId).IsUnique();
        });
    }
}
