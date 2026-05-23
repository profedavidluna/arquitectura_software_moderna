namespace NotificationService.Infrastructure.Persistence;

using Microsoft.EntityFrameworkCore;
using NotificationService.Infrastructure.Persistence.Entities;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<NotificationEntity> Notifications => Set<NotificationEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<NotificationEntity>(entity =>
        {
            entity.ToTable("Notifications");
            entity.HasKey(e => e.Id);
        });
    }
}
