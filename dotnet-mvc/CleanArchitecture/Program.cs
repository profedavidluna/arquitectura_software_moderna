using CleanArchitecture.Entities;
using CleanArchitecture.Framework.Config;
using CleanArchitecture.UseCases;
using Microsoft.AspNetCore.Diagnostics;
using Microsoft.AspNetCore.Mvc;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container
builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new() { Title = "Clean Architecture - Product Catalog", Version = "v1" });
});

// Register all Clean Architecture layers via extension method
builder.Services.AddCleanArchitectureServices();

// Configure port
builder.WebHost.UseUrls("http://localhost:5083");

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
            UseCaseException => StatusCodes.Status400BadRequest,
            EntityValidationException => StatusCodes.Status400BadRequest,
            _ => StatusCodes.Status500InternalServerError
        };

        var problemDetails = new ProblemDetails
        {
            Status = statusCode,
            Title = exception switch
            {
                ProductNotFoundException => "Product Not Found",
                UseCaseException => "Application Rule Violation",
                EntityValidationException => "Business Rule Violation",
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
