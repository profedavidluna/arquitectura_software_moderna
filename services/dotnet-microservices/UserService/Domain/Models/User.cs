namespace UserService.Domain.Models;

public record User(
    Guid Id,
    string Email,
    string FirstName,
    string LastName,
    string PasswordHash,
    string? Phone,
    bool IsActive,
    DateTime CreatedAt,
    DateTime UpdatedAt,
    List<Address> Addresses
);

public record Address(
    Guid Id,
    Guid UserId,
    string Street,
    string City,
    string State,
    string ZipCode,
    string Country,
    bool IsDefault,
    DateTime CreatedAt
);

public record CreateUserRequest(
    string Email,
    string FirstName,
    string LastName,
    string Password,
    string? Phone
);

public record UpdateUserRequest(
    string? FirstName,
    string? LastName,
    string? Phone
);

public record CreateAddressRequest(
    string Street,
    string City,
    string State,
    string ZipCode,
    string Country,
    bool IsDefault
);
