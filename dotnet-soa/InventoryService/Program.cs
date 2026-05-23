/// <summary>
/// Inventory Service - SOA Architecture
/// 
/// This service manages product inventory/stock levels and participates
/// in the Order Saga as a participant service. It consumes order events
/// and publishes stock reservation results.
/// 
/// SOA Principle: Service Statelessness - while the service manages state
/// (inventory levels), each request is processed independently without
/// relying on previous interactions.
/// 
/// Design Pattern: Saga Participant - responds to order events by
/// attempting stock reservation and publishing the result.
/// </summary>

using Microsoft.EntityFrameworkCore;
using InventoryService.Application;
using InventoryService.Domain.Interfaces;
using InventoryService.Infrastructure.Messaging;
using InventoryService.Infrastructure.Persistence;

var builder = WebApplication.CreateBuilder(args);

// Configure Entity Framework based on environment
if (builder.Environment.IsEnvironment("Docker"))
{
    builder.Services.AddDbContext<AppDbContext>(options =>
        options.UseNpgsql(builder.Configuration.GetConnectionString("DefaultConnection")));
}
else
{
    builder.Services.AddDbContext<AppDbContext>(options =>
        options.UseInMemoryDatabase("InventoryDb"));
}

// Register application services following DIP
builder.Services.AddScoped<IInventoryService, InventoryServiceImpl>();
builder.Services.AddScoped<InventoryRepository>();

// Register Kafka producer as singleton
builder.Services.AddSingleton<KafkaProducer>(sp =>
    new KafkaProducer(builder.Configuration["Kafka:BootstrapServers"] ?? "localhost:9095"));

// Register Kafka consumer as hosted background service
builder.Services.AddHostedService<OrderEventConsumer>();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();

var app = builder.Build();

// Ensure database is created
using (var scope = app.Services.CreateScope())
{
    var context = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    context.Database.EnsureCreated();
}

app.MapControllers();

app.Run();
