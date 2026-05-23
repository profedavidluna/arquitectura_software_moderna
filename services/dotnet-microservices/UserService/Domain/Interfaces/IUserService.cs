namespace UserService.Domain.Interfaces;

using UserService.Domain.Models;

public interface IUserService
{
    Task<User?> GetUserByIdAsync(Guid id);
    Task<User?> GetUserByEmailAsync(string email);
    Task<IEnumerable<User>> GetAllUsersAsync();
    Task<User> CreateUserAsync(CreateUserRequest request);
    Task<User> UpdateUserAsync(Guid id, UpdateUserRequest request);
    Task<bool> DeleteUserAsync(Guid id);
    Task<Address> AddAddressAsync(Guid userId, CreateAddressRequest request);
    Task<IEnumerable<Address>> GetUserAddressesAsync(Guid userId);
}
