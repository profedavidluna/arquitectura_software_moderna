using HexagonalArchitecture.Adapters.Output.Persistence;
using HexagonalArchitecture.Domain.Models;
using HexagonalArchitecture.Domain.Ports.Input;
using HexagonalArchitecture.Domain.Ports.Output;
using HexagonalArchitecture.Domain.Services;
using Microsoft.AspNetCore.Diagnostics;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container
builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new() { Title = "Hexagonal Architecture - Product Catalog", Version = "v1" });
});

// EF Core InMemory Database (Output Adapter infrastructure)
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseInMemoryDatabase("HexagonalProductCatalog"));

// Dependency Injection: Wire ports to adapters
// Output port → Output adapter
builder.Services.AddScoped<IProductRepository, ProductRepositoryAdapter>();
// Input port → Domain service
builder.Services.AddScoped<IProductService, ProductService>();

// Configure port
builder.WebHost.UseUrls("http://localhost:5081");

var app = builder.Build();

// Configure the HTTP request pipeline
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

// Global exception handler using ProblemDetails
app.UseExceptionHandler(errorApp =>
{
    errorApp.Run(async context =>
    {
        var exception = context.Features.Get<IExceptionHandlerFeature>()?.Error;
        var statusCode = exception switch
        {
            ProductNotFoundException => StatusCodes.Status404NotFound,
            DomainException => StatusCodes.Status400BadRequest,
            _ => StatusCodes.Status500InternalServerError
        };

        var problemDetails = new ProblemDetails
        {
            Status = statusCode,
            Title = exception switch
            {
                ProductNotFoundException => "Product Not Found",
                DomainException => "Business Rule Violation",
                _ => "Internal Server Error"
            },
            Detail = exception?.Message,
            Instance = context.Request.Path
        };

        context.Response.StatusCode = statusCode;
        context.Response.ContentType = "application/problem+json";
        await context.Response.WriteAsJsonAsync(problemDetails);
    });
});

app.MapControllers();

app.Run();
