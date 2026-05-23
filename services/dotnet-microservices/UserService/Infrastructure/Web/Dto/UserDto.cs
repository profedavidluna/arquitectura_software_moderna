namespace UserService.Infrastructure.Web.Dto;

public record UserDto(
    Guid Id,
    string Email,
    string FirstName,
    string LastName,
    string? Phone,
    bool IsActive,
    DateTime CreatedAt,
    List<AddressDto> Addresses
);

public record AddressDto(
    Guid Id,
    string Street,
    string City,
    string State,
    string ZipCode,
    string Country,
    bool IsDefault
);
