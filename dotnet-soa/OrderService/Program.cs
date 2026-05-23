/// <summary>
/// Order Service - SOA Architecture
/// 
/// This service manages orders and orchestrates the Saga pattern for
/// distributed transactions across services. It publishes order events
/// and consumes inventory events to coordinate the order lifecycle.
/// 
/// SOA Principle: Service Composability - this service composes
/// functionality from multiple services (Product, Inventory) through
/// asynchronous event-driven communication via the ESB (Kafka).
/// 
/// Design Pattern: Saga Orchestrator - manages the distributed transaction
/// for order creation by coordinating stock reservation and confirmation.
/// </summary>

using Microsoft.EntityFrameworkCore;
using OrderService.Application;
using OrderService.Domain.Interfaces;
using OrderService.Infrastructure.Messaging;
using OrderService.Infrastructure.Persistence;

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
        options.UseInMemoryDatabase("OrderDb"));
}

// Register application services following DIP
builder.Services.AddScoped<IOrderService, OrderServiceImpl>();
builder.Services.AddScoped<OrderRepository>();

// Register Kafka producer as singleton
builder.Services.AddSingleton<KafkaProducer>(sp =>
    new KafkaProducer(builder.Configuration["Kafka:BootstrapServers"] ?? "localhost:9095"));

// Register Kafka consumer as hosted background service
builder.Services.AddHostedService<InventoryEventConsumer>();

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
