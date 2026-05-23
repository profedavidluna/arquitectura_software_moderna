namespace UserService.Infrastructure.Persistence.Repositories;

using Microsoft.EntityFrameworkCore;
using UserService.Domain.Interfaces;
using UserService.Domain.Models;
using UserService.Infrastructure.Persistence.Entities;

public class UserRepository : IUserRepository
{
    private readonly AppDbContext _context;

    public UserRepository(AppDbContext context)
    {
        _context = context;
    }

    public async Task<User?> GetByIdAsync(Guid id)
    {
        var entity = await _context.Users
            .Include(u => u.Addresses)
            .FirstOrDefaultAsync(u => u.Id == id);
        return entity == null ? null : MapToDomain(entity);
    }

    public async Task<User?> GetByEmailAsync(string email)
    {
        var entity = await _context.Users
            .Include(u => u.Addresses)
            .FirstOrDefaultAsync(u => u.Email == email);
        return entity == null ? null : MapToDomain(entity);
    }

    public async Task<IEnumerable<User>> GetAllAsync()
    {
        var entities = await _context.Users
            .Include(u => u.Addresses)
            .ToListAsync();
        return entities.Select(MapToDomain);
    }

    public async Task<User> CreateAsync(User user)
    {
        var entity = MapToEntity(user);
        _context.Users.Add(entity);
        await _context.SaveChangesAsync();
        return MapToDomain(entity);
    }

    public async Task<User> UpdateAsync(User user)
    {
        var entity = await _context.Users.FindAsync(user.Id)
            ?? throw new KeyNotFoundException($"User {user.Id} not found");

        entity.FirstName = user.FirstName;
        entity.LastName = user.LastName;
        entity.Phone = user.Phone;
        entity.IsActive = user.IsActive;
        entity.UpdatedAt = user.UpdatedAt;

        await _context.SaveChangesAsync();
        return MapToDomain(entity);
    }

    public async Task<bool> DeleteAsync(Guid id)
    {
        var entity = await _context.Users.FindAsync(id);
        if (entity == null) return false;
        _context.Users.Remove(entity);
        await _context.SaveChangesAsync();
        return true;
    }

    public async Task<Address> AddAddressAsync(Address address)
    {
        var entity = new AddressEntity
        {
            Id = address.Id,
            UserId = address.UserId,
            Street = address.Street,
            City = address.City,
            State = address.State,
            ZipCode = address.ZipCode,
            Country = address.Country,
            IsDefault = address.IsDefault,
            CreatedAt = address.CreatedAt
        };
        _context.Addresses.Add(entity);
        await _context.SaveChangesAsync();
        return MapAddressToDomain(entity);
    }

    public async Task<IEnumerable<Address>> GetAddressesByUserIdAsync(Guid userId)
    {
        var entities = await _context.Addresses
            .Where(a => a.UserId == userId)
            .ToListAsync();
        return entities.Select(MapAddressToDomain);
    }

    public async Task<bool> DeleteAddressAsync(Guid addressId)
    {
        var entity = await _context.Addresses.FindAsync(addressId);
        if (entity == null) return false;
        _context.Addresses.Remove(entity);
        await _context.SaveChangesAsync();
        return true;
    }

    private static User MapToDomain(UserEntity entity) => new(
        Id: entity.Id,
        Email: entity.Email,
        FirstName: entity.FirstName,
        LastName: entity.LastName,
        PasswordHash: entity.PasswordHash,
        Phone: entity.Phone,
        IsActive: entity.IsActive,
        CreatedAt: entity.CreatedAt,
        UpdatedAt: entity.UpdatedAt,
        Addresses: entity.Addresses.Select(MapAddressToDomain).ToList()
    );

    private static Address MapAddressToDomain(AddressEntity entity) => new(
        Id: entity.Id,
        UserId: entity.UserId,
        Street: entity.Street,
        City: entity.City,
        State: entity.State,
        ZipCode: entity.ZipCode,
        Country: entity.Country,
        IsDefault: entity.IsDefault,
        CreatedAt: entity.CreatedAt
    );

    private static UserEntity MapToEntity(User user) => new()
    {
        Id = user.Id,
        Email = user.Email,
        FirstName = user.FirstName,
        LastName = user.LastName,
        PasswordHash = user.PasswordHash,
        Phone = user.Phone,
        IsActive = user.IsActive,
        CreatedAt = user.CreatedAt,
        UpdatedAt = user.UpdatedAt
    };
}
