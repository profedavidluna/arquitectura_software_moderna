/// <summary>
/// Product Service - SOA Architecture
/// 
/// This service manages the product catalog in the SOA ecosystem.
/// It publishes events to Kafka when products are created, enabling
/// other services to react to product changes asynchronously.
/// 
/// SOA Principle: Service Autonomy - This service owns its data
/// and exposes functionality through well-defined interfaces.
/// </summary>

using Microsoft.EntityFrameworkCore;
using ProductService.Application;
using ProductService.Domain.Interfaces;
using ProductService.Infrastructure.Messaging;
using ProductService.Infrastructure.Persistence;

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
        options.UseInMemoryDatabase("ProductDb"));
}

// Register application services following DIP (Dependency Inversion Principle)
builder.Services.AddScoped<IProductService, ProductServiceImpl>();
builder.Services.AddScoped<ProductRepository>();

// Register Kafka producer as singleton (thread-safe)
builder.Services.AddSingleton<KafkaProducer>(sp =>
    new KafkaProducer(builder.Configuration["Kafka:BootstrapServers"] ?? "localhost:9095"));

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();

var app = builder.Build();

// Ensure database is created for InMemory provider
using (var scope = app.Services.CreateScope())
{
    var context = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    context.Database.EnsureCreated();
}

app.MapControllers();

app.Run();
