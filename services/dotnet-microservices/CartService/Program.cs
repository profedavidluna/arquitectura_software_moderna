using Microsoft.EntityFrameworkCore;
using CartService.Domain.Interfaces;
using CartService.Application;
using CartService.Infrastructure.Persistence;
using CartService.Infrastructure.Persistence.Repositories;
using CartService.Infrastructure.Messaging;
using CartService.Infrastructure.Cache;
using Polly;
using Polly.Extensions.Http;

var builder = WebApplication.CreateBuilder(args);

// Database configuration
if (builder.Environment.IsEnvironment("Docker"))
{
    builder.Services.AddDbContext<AppDbContext>(options =>
        options.UseNpgsql(builder.Configuration.GetConnectionString("PostgreSQL")));
}
else
{
    builder.Services.AddDbContext<AppDbContext>(options =>
        options.UseInMemoryDatabase("CartDb"));
}

// Redis Cache
builder.Services.AddSingleton<IRedisCacheService, RedisCacheService>();

// HTTP Client with Polly Circuit Breaker for ProductService
builder.Services.AddHttpClient("ProductService", client =>
{
    var baseUrl = builder.Configuration["Services:ProductService"] ?? "http://localhost:6083";
    client.BaseAddress = new Uri(baseUrl);
    client.Timeout = TimeSpan.FromSeconds(10);
});

// Services
builder.Services.AddScoped<ICartRepository, CartRepository>();
builder.Services.AddScoped<ICartService, CartServiceImpl>();
builder.Services.AddSingleton<KafkaProducer>();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();

var app = builder.Build();

using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    db.Database.EnsureCreated();
}

app.MapControllers();
app.Run();

public partial class Program { }
