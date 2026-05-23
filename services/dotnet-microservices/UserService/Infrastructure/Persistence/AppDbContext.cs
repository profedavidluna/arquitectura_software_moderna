namespace UserService.Infrastructure.Persistence;

using Microsoft.EntityFrameworkCore;
using UserService.Infrastructure.Persistence.Entities;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<UserEntity> Users => Set<UserEntity>();
    public DbSet<AddressEntity> Addresses => Set<AddressEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<UserEntity>(entity =>
        {
            entity.ToTable("Users");
            entity.HasKey(e => e.Id);
            entity.HasIndex(e => e.Email).IsUnique();
            entity.HasMany(e => e.Addresses)
                  .WithOne(a => a.User)
                  .HasForeignKey(a => a.UserId)
                  .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<AddressEntity>(entity =>
        {
            entity.ToTable("Addresses");
            entity.HasKey(e => e.Id);
        });
    }
}
