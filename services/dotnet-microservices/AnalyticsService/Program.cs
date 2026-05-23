using Microsoft.EntityFrameworkCore;
using AnalyticsService.Domain.Interfaces;
using AnalyticsService.Application;
using AnalyticsService.Infrastructure.Persistence;
using AnalyticsService.Infrastructure.Persistence.Repositories;
using AnalyticsService.Infrastructure.Messaging;

var builder = WebApplication.CreateBuilder(args);

if (builder.Environment.IsEnvironment("Docker"))
{
    builder.Services.AddDbContext<AppDbContext>(options =>
        options.UseNpgsql(builder.Configuration.GetConnectionString("PostgreSQL")));
}
else
{
    builder.Services.AddDbContext<AppDbContext>(options =>
        options.UseInMemoryDatabase("AnalyticsDb"));
}

builder.Services.AddScoped<IAnalyticsRepository, AnalyticsRepository>();
builder.Services.AddScoped<IAnalyticsService, AnalyticsServiceImpl>();
builder.Services.AddHostedService<AnalyticsEventConsumer>();

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
