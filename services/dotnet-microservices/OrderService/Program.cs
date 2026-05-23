using Microsoft.EntityFrameworkCore;
using OrderService.Domain.Interfaces;
using OrderService.Application;
using OrderService.Infrastructure.Persistence;
using OrderService.Infrastructure.Persistence.Repositories;
using OrderService.Infrastructure.Messaging;

var builder = WebApplication.CreateBuilder(args);

if (builder.Environment.IsEnvironment("Docker"))
{
    builder.Services.AddDbContext<AppDbContext>(options =>
        options.UseNpgsql(builder.Configuration.GetConnectionString("PostgreSQL")));
}
else
{
    builder.Services.AddDbContext<AppDbContext>(options =>
        options.UseInMemoryDatabase("OrderDb"));
}

builder.Services.AddScoped<IOrderRepository, OrderRepository>();
builder.Services.AddScoped<IOrderService, OrderServiceImpl>();
builder.Services.AddSingleton<KafkaProducer>();
builder.Services.AddHostedService<OrderSagaConsumer>();

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
