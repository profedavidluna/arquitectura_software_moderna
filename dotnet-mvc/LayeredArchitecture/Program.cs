using LayeredArchitecture.Business.Exceptions;
using LayeredArchitecture.Business.Services;
using LayeredArchitecture.Data;
using LayeredArchitecture.Data.Repositories;
using Microsoft.AspNetCore.Diagnostics;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container
builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new() { Title = "Layered Architecture - Product Catalog", Version = "v1" });
});

// EF Core InMemory Database (Data Layer)
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseInMemoryDatabase("LayeredProductCatalog"));

// Register services — simple layered DI
builder.Services.AddScoped<ProductRepository>();
builder.Services.AddScoped<ProductService>();

// Configure port
builder.WebHost.UseUrls("http://localhost:5082");

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
            BusinessRuleException => StatusCodes.Status400BadRequest,
            _ => StatusCodes.Status500InternalServerError
        };

        var problemDetails = new ProblemDetails
        {
            Status = statusCode,
            Title = exception switch
            {
                ProductNotFoundException => "Product Not Found",
                BusinessRuleException => "Business Rule Violation",
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
