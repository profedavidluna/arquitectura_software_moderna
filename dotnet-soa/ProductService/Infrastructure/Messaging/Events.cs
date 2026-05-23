namespace ProductService.Infrastructure.Messaging;

/// <summary>
/// Domain events published by the Product Service to the ESB (Kafka).
/// 
/// SOA Principle: Standardized Service Contract - events define the
/// communication protocol between services in the SOA ecosystem.
/// </summary>
public record ProductCreatedEvent
{
    public Guid ProductId { get; init; }
    public string Name { get; init; } = string.Empty;
    public string? Description { get; init; }
    public decimal Price { get; init; }
    public string? Category { get; init; }
    public DateTime Timestamp { get; init; } = DateTime.UtcNow;
}
