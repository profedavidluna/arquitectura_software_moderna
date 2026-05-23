namespace PaymentService.Infrastructure.Messaging;

using System.Text.Json;
using Confluent.Kafka;
using PaymentService.Domain.Interfaces;
using PaymentService.Domain.Models;

public class PaymentCommandConsumer : BackgroundService
{
    private readonly ILogger<PaymentCommandConsumer> _logger;
    private readonly IConfiguration _configuration;
    private readonly IServiceProvider _serviceProvider;

    public PaymentCommandConsumer(ILogger<PaymentCommandConsumer> logger, IConfiguration configuration, IServiceProvider serviceProvider)
    {
        _logger = logger;
        _configuration = configuration;
        _serviceProvider = serviceProvider;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        var bootstrapServers = _configuration["Kafka:BootstrapServers"];
        if (string.IsNullOrEmpty(bootstrapServers))
        {
            _logger.LogWarning("Kafka not configured. PaymentCommandConsumer disabled.");
            return;
        }

        await Task.Delay(5000, stoppingToken);

        var config = new ConsumerConfig
        {
            BootstrapServers = bootstrapServers,
            GroupId = _configuration["Kafka:GroupId"] ?? "payment-service-group",
            AutoOffsetReset = AutoOffsetReset.Earliest,
            EnableAutoCommit = true
        };

        try
        {
            using var consumer = new ConsumerBuilder<string, string>(config).Build();
            consumer.Subscribe("payment-commands");

            _logger.LogInformation("PaymentCommandConsumer started, listening on 'payment-commands'");

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    var result = consumer.Consume(TimeSpan.FromSeconds(1));
                    if (result != null)
                    {
                        await ProcessCommandAsync(result.Message.Value);
                    }
                }
                catch (ConsumeException ex) { _logger.LogError(ex, "Error consuming payment command"); }
            }
        }
        catch (Exception ex) { _logger.LogError(ex, "PaymentCommandConsumer failed"); }
    }

    private async Task ProcessCommandAsync(string messageJson)
    {
        try
        {
            var message = JsonSerializer.Deserialize<JsonElement>(messageJson);
            var eventType = message.GetProperty("EventType").GetString();

            using var scope = _serviceProvider.CreateScope();
            var paymentService = scope.ServiceProvider.GetRequiredService<IPaymentService>();

            switch (eventType)
            {
                case "ProcessPayment":
                    var orderId = Guid.Parse(message.GetProperty("OrderId").GetString()!);
                    var userId = Guid.Parse(message.GetProperty("UserId").GetString()!);
                    var amount = message.GetProperty("Amount").GetDecimal();

                    await paymentService.ProcessPaymentAsync(new ProcessPaymentRequest(orderId, userId, amount, "credit_card"));
                    break;

                case "RefundPayment":
                    var refundOrderId = Guid.Parse(message.GetProperty("OrderId").GetString()!);
                    var payment = await paymentService.GetPaymentByOrderIdAsync(refundOrderId);
                    if (payment != null)
                    {
                        await paymentService.ProcessRefundAsync(new RefundRequest(payment.Id, payment.Amount, "Order cancelled"));
                    }
                    break;
            }
        }
        catch (Exception ex) { _logger.LogError(ex, "Error processing payment command"); }
    }
}
