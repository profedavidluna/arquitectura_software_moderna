namespace UserService.Domain.Interfaces;

using UserService.Domain.Models;

public interface IUserRepository
{
    Task<User?> GetByIdAsync(Guid id);
    Task<User?> GetByEmailAsync(string email);
    Task<IEnumerable<User>> GetAllAsync();
    Task<User> CreateAsync(User user);
    Task<User> UpdateAsync(User user);
    Task<bool> DeleteAsync(Guid id);
    Task<Address> AddAddressAsync(Address address);
    Task<IEnumerable<Address>> GetAddressesByUserIdAsync(Guid userId);
    Task<bool> DeleteAddressAsync(Guid addressId);
}
