using CleanArchitecture.Framework.Persistence;
using CleanArchitecture.InterfaceAdapters.Gateways;
using CleanArchitecture.UseCases;
using CleanArchitecture.UseCases.Ports;
using Microsoft.EntityFrameworkCore;

namespace CleanArchitecture.Framework.Config;

/// <summary>
/// Dependency Injection configuration.
/// This belongs to the FRAMEWORK layer (outermost) — it wires all layers together.
/// This is the composition root where the Dependency Rule is enforced:
/// outer layers depend on inner layers, never the reverse.
/// </summary>
public static class DependencyInjection
{
    public static IServiceCollection AddCleanArchitectureServices(this IServiceCollection services)
    {
        // Framework layer: EF Core InMemory Database
        services.AddDbContext<AppDbContext>(options =>
            options.UseInMemoryDatabase("CleanProductCatalog"));

        // Interface Adapters layer: Gateway implementations
        services.AddScoped<IProductGateway, ProductDatabaseGateway>();

        // Use Cases layer: One use case per class
        services.AddScoped<CreateProductUseCase>();
        services.AddScoped<GetProductUseCase>();
        services.AddScoped<ListProductsUseCase>();
        services.AddScoped<SearchProductsUseCase>();
        services.AddScoped<UpdateProductUseCase>();
        services.AddScoped<DeleteProductUseCase>();
        services.AddScoped<ManageStockUseCase>();

        return services;
    }
}
