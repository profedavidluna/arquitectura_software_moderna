using Microsoft.EntityFrameworkCore;
using ProductService.Domain.Interfaces;
using ProductService.Application;
using ProductService.Infrastructure.Persistence;
using ProductService.Infrastructure.Persistence.Repositories;
using ProductService.Infrastructure.Messaging;
using ProductService.Infrastructure.Cache;

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
        options.UseInMemoryDatabase("ProductDb"));
}

// Redis Cache
builder.Services.AddSingleton<IRedisCacheService, RedisCacheService>();

// Services
builder.Services.AddScoped<IProductRepository, ProductRepository>();
builder.Services.AddScoped<IProductService, ProductServiceImpl>();
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
