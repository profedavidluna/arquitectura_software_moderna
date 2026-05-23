namespace UserService.Application;

using System.Security.Cryptography;
using System.Text;
using UserService.Domain.Interfaces;
using UserService.Domain.Models;
using UserService.Infrastructure.Messaging;

public class UserServiceImpl : IUserService
{
    private readonly IUserRepository _repository;
    private readonly KafkaProducer _kafkaProducer;
    private readonly ILogger<UserServiceImpl> _logger;

    public UserServiceImpl(IUserRepository repository, KafkaProducer kafkaProducer, ILogger<UserServiceImpl> logger)
    {
        _repository = repository;
        _kafkaProducer = kafkaProducer;
        _logger = logger;
    }

    public async Task<User?> GetUserByIdAsync(Guid id) => await _repository.GetByIdAsync(id);

    public async Task<User?> GetUserByEmailAsync(string email) => await _repository.GetByEmailAsync(email);

    public async Task<IEnumerable<User>> GetAllUsersAsync() => await _repository.GetAllAsync();

    public async Task<User> CreateUserAsync(CreateUserRequest request)
    {
        var existing = await _repository.GetByEmailAsync(request.Email);
        if (existing != null)
            throw new InvalidOperationException($"User with email {request.Email} already exists");

        var user = new User(
            Id: Guid.NewGuid(),
            Email: request.Email,
            FirstName: request.FirstName,
            LastName: request.LastName,
            PasswordHash: HashPassword(request.Password),
            Phone: request.Phone,
            IsActive: true,
            CreatedAt: DateTime.UtcNow,
            UpdatedAt: DateTime.UtcNow,
            Addresses: new List<Address>()
        );

        var created = await _repository.CreateAsync(user);

        await _kafkaProducer.PublishAsync("user-events", new
        {
            EventType = "UserCreated",
            UserId = created.Id,
            Email = created.Email,
            Timestamp = DateTime.UtcNow
        });

        _logger.LogInformation("User created: {UserId}", created.Id);
        return created;
    }

    public async Task<User> UpdateUserAsync(Guid id, UpdateUserRequest request)
    {
        var user = await _repository.GetByIdAsync(id)
            ?? throw new KeyNotFoundException($"User {id} not found");

        var updated = user with
        {
            FirstName = request.FirstName ?? user.FirstName,
            LastName = request.LastName ?? user.LastName,
            Phone = request.Phone ?? user.Phone,
            UpdatedAt = DateTime.UtcNow
        };

        var result = await _repository.UpdateAsync(updated);

        await _kafkaProducer.PublishAsync("user-events", new
        {
            EventType = "UserUpdated",
            UserId = result.Id,
            Timestamp = DateTime.UtcNow
        });

        return result;
    }

    public async Task<bool> DeleteUserAsync(Guid id)
    {
        var deleted = await _repository.DeleteAsync(id);
        if (deleted)
        {
            await _kafkaProducer.PublishAsync("user-events", new
            {
                EventType = "UserDeleted",
                UserId = id,
                Timestamp = DateTime.UtcNow
            });
        }
        return deleted;
    }

    public async Task<Address> AddAddressAsync(Guid userId, CreateAddressRequest request)
    {
        var user = await _repository.GetByIdAsync(userId)
            ?? throw new KeyNotFoundException($"User {userId} not found");

        var address = new Address(
            Id: Guid.NewGuid(),
            UserId: userId,
            Street: request.Street,
            City: request.City,
            State: request.State,
            ZipCode: request.ZipCode,
            Country: request.Country,
            IsDefault: request.IsDefault,
            CreatedAt: DateTime.UtcNow
        );

        return await _repository.AddAddressAsync(address);
    }

    public async Task<IEnumerable<Address>> GetUserAddressesAsync(Guid userId)
    {
        return await _repository.GetAddressesByUserIdAsync(userId);
    }

    private static string HashPassword(string password)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(password));
        return Convert.ToBase64String(bytes);
    }
}
