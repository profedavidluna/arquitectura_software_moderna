namespace PaymentService.Infrastructure.Persistence;

using Microsoft.EntityFrameworkCore;
using PaymentService.Infrastructure.Persistence.Entities;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<PaymentEntity> Payments => Set<PaymentEntity>();
    public DbSet<RefundEntity> Refunds => Set<RefundEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<PaymentEntity>(entity =>
        {
            entity.ToTable("Payments");
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Amount).HasColumnType("decimal(10,2)");
        });

        modelBuilder.Entity<RefundEntity>(entity =>
        {
            entity.ToTable("Refunds");
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Amount).HasColumnType("decimal(10,2)");
        });
    }
}
