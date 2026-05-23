namespace AnalyticsService.Infrastructure.Persistence;

using Microsoft.EntityFrameworkCore;
using AnalyticsService.Infrastructure.Persistence.Entities;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<EventEntity> Events => Set<EventEntity>();
    public DbSet<MetricEntity> Metrics => Set<MetricEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<EventEntity>(entity =>
        {
            entity.ToTable("Events");
            entity.HasKey(e => e.Id);
        });

        modelBuilder.Entity<MetricEntity>(entity =>
        {
            entity.ToTable("Metrics");
            entity.HasKey(e => e.Id);
            entity.Property(e => e.MetricValue).HasColumnType("decimal(15,4)");
        });
    }
}
