namespace InventoryService.Infrastructure.Persistence;

using Microsoft.EntityFrameworkCore;
using InventoryService.Infrastructure.Persistence.Entities;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<InventoryItemEntity> InventoryItems => Set<InventoryItemEntity>();
    public DbSet<ReservationEntity> Reservations => Set<ReservationEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<InventoryItemEntity>(entity =>
        {
            entity.ToTable("InventoryItems");
            entity.HasKey(e => e.Id);
            entity.HasIndex(e => e.ProductId).IsUnique();
        });

        modelBuilder.Entity<ReservationEntity>(entity =>
        {
            entity.ToTable("Reservations");
            entity.HasKey(e => e.Id);
        });
    }
}
