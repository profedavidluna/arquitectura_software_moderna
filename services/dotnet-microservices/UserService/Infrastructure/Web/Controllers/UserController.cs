namespace UserService.Infrastructure.Web.Controllers;

using Microsoft.AspNetCore.Mvc;
using UserService.Domain.Interfaces;
using UserService.Domain.Models;
using UserService.Infrastructure.Web.Dto;

[ApiController]
[Route("api/[controller]")]
public class UserController : ControllerBase
{
    private readonly IUserService _userService;

    public UserController(IUserService userService)
    {
        _userService = userService;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<UserDto>>> GetAll()
    {
        var users = await _userService.GetAllUsersAsync();
        return Ok(users.Select(MapToDto));
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<UserDto>> GetById(Guid id)
    {
        var user = await _userService.GetUserByIdAsync(id);
        if (user == null) return NotFound();
        return Ok(MapToDto(user));
    }

    [HttpGet("email/{email}")]
    public async Task<ActionResult<UserDto>> GetByEmail(string email)
    {
        var user = await _userService.GetUserByEmailAsync(email);
        if (user == null) return NotFound();
        return Ok(MapToDto(user));
    }

    [HttpPost]
    public async Task<ActionResult<UserDto>> Create([FromBody] CreateUserRequest request)
    {
        try
        {
            var user = await _userService.CreateUserAsync(request);
            return CreatedAtAction(nameof(GetById), new { id = user.Id }, MapToDto(user));
        }
        catch (InvalidOperationException ex)
        {
            return Conflict(new { message = ex.Message });
        }
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<UserDto>> Update(Guid id, [FromBody] UpdateUserRequest request)
    {
        try
        {
            var user = await _userService.UpdateUserAsync(id, request);
            return Ok(MapToDto(user));
        }
        catch (KeyNotFoundException)
        {
            return NotFound();
        }
    }

    [HttpDelete("{id:guid}")]
    public async Task<ActionResult> Delete(Guid id)
    {
        var deleted = await _userService.DeleteUserAsync(id);
        if (!deleted) return NotFound();
        return NoContent();
    }

    [HttpPost("{userId:guid}/addresses")]
    public async Task<ActionResult<AddressDto>> AddAddress(Guid userId, [FromBody] CreateAddressRequest request)
    {
        try
        {
            var address = await _userService.AddAddressAsync(userId, request);
            return Created($"/api/user/{userId}/addresses/{address.Id}", MapAddressToDto(address));
        }
        catch (KeyNotFoundException)
        {
            return NotFound();
        }
    }

    [HttpGet("{userId:guid}/addresses")]
    public async Task<ActionResult<IEnumerable<AddressDto>>> GetAddresses(Guid userId)
    {
        var addresses = await _userService.GetUserAddressesAsync(userId);
        return Ok(addresses.Select(MapAddressToDto));
    }

    private static UserDto MapToDto(User user) => new(
        user.Id, user.Email, user.FirstName, user.LastName,
        user.Phone, user.IsActive, user.CreatedAt,
        user.Addresses.Select(MapAddressToDto).ToList()
    );

    private static AddressDto MapAddressToDto(Address a) => new(
        a.Id, a.Street, a.City, a.State, a.ZipCode, a.Country, a.IsDefault
    );
}
