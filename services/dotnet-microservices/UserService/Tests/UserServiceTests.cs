namespace UserService.Tests;

using Xunit;
using Moq;
using Microsoft.EntityFrameworkCore;
using UserService.Application;
using UserService.Domain.Models;
using UserService.Infrastructure.Messaging;
using UserService.Infrastructure.Persistence;
using UserService.Infrastructure.Persistence.Repositories;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;

public class UserServiceTests
{
    private readonly AppDbContext _context;
    private readonly UserServiceImpl _service;

    public UserServiceTests()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
            .Options;

        _context = new AppDbContext(options);
        var repository = new UserRepository(_context);
        var config = new ConfigurationBuilder().AddInMemoryCollection().Build();
        var kafkaProducer = new KafkaProducer(config, NullLogger<KafkaProducer>.Instance);
        _service = new UserServiceImpl(repository, kafkaProducer, NullLogger<UserServiceImpl>.Instance);
    }

    [Fact]
    public async Task CreateUser_ShouldReturnCreatedUser()
    {
        var request = new CreateUserRequest("test@example.com", "John", "Doe", "password123", "555-0100");

        var result = await _service.CreateUserAsync(request);

        Assert.NotEqual(Guid.Empty, result.Id);
        Assert.Equal("test@example.com", result.Email);
        Assert.Equal("John", result.FirstName);
        Assert.Equal("Doe", result.LastName);
        Assert.True(result.IsActive);
    }

    [Fact]
    public async Task CreateUser_DuplicateEmail_ShouldThrow()
    {
        var request = new CreateUserRequest("dup@example.com", "Jane", "Doe", "pass", null);
        await _service.CreateUserAsync(request);

        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateUserAsync(request));
    }

    [Fact]
    public async Task GetUserById_ShouldReturnUser()
    {
        var request = new CreateUserRequest("find@example.com", "Find", "Me", "pass", null);
        var created = await _service.CreateUserAsync(request);

        var found = await _service.GetUserByIdAsync(created.Id);

        Assert.NotNull(found);
        Assert.Equal(created.Id, found.Id);
        Assert.Equal("find@example.com", found.Email);
    }

    [Fact]
    public async Task UpdateUser_ShouldUpdateFields()
    {
        var request = new CreateUserRequest("update@example.com", "Old", "Name", "pass", null);
        var created = await _service.CreateUserAsync(request);

        var updateRequest = new UpdateUserRequest("New", "Name", "555-9999");
        var updated = await _service.UpdateUserAsync(created.Id, updateRequest);

        Assert.Equal("New", updated.FirstName);
        Assert.Equal("555-9999", updated.Phone);
    }

    [Fact]
    public async Task DeleteUser_ShouldRemoveUser()
    {
        var request = new CreateUserRequest("delete@example.com", "Del", "User", "pass", null);
        var created = await _service.CreateUserAsync(request);

        var deleted = await _service.DeleteUserAsync(created.Id);
        var found = await _service.GetUserByIdAsync(created.Id);

        Assert.True(deleted);
        Assert.Null(found);
    }
}
