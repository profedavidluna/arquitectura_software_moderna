namespace PaymentService.Application;

using PaymentService.Domain.Interfaces;
using PaymentService.Domain.Models;
using PaymentService.Infrastructure.Messaging;

public class PaymentServiceImpl : IPaymentService
{
    private readonly IPaymentRepository _repository;
    private readonly KafkaProducer _kafkaProducer;
    private readonly ILogger<PaymentServiceImpl> _logger;

    public PaymentServiceImpl(IPaymentRepository repository, KafkaProducer kafkaProducer, ILogger<PaymentServiceImpl> logger)
    {
        _repository = repository;
        _kafkaProducer = kafkaProducer;
        _logger = logger;
    }

    public async Task<Payment?> GetPaymentByIdAsync(Guid id) => await _repository.GetByIdAsync(id);

    public async Task<Payment?> GetPaymentByOrderIdAsync(Guid orderId) => await _repository.GetByOrderIdAsync(orderId);

    public async Task<IEnumerable<Payment>> GetPaymentsByUserIdAsync(Guid userId) => await _repository.GetByUserIdAsync(userId);

    public async Task<Payment> ProcessPaymentAsync(ProcessPaymentRequest request)
    {
        _logger.LogInformation("Processing payment for order {OrderId}, amount: {Amount}", request.OrderId, request.Amount);

        var payment = new Payment(
            Id: Guid.NewGuid(),
            OrderId: request.OrderId,
            UserId: request.UserId,
            Amount: request.Amount,
            Currency: request.Currency,
            Status: PaymentStatuses.Processing,
            PaymentMethod: request.PaymentMethod ?? "credit_card",
            TransactionId: null,
            FailureReason: null,
            CreatedAt: DateTime.UtcNow,
            UpdatedAt: DateTime.UtcNow
        );

        var created = await _repository.CreateAsync(payment);

        // Simulate payment processing with retry logic
        var (success, transactionId, failureReason) = await SimulatePaymentProcessingAsync(request.Amount);

        Payment updated;
        if (success)
        {
            updated = created with
            {
                Status = PaymentStatuses.Completed,
                TransactionId = transactionId,
                UpdatedAt = DateTime.UtcNow
            };

            await _kafkaProducer.PublishAsync("payment-events", new
            {
                EventType = "PaymentProcessed",
                OrderId = request.OrderId,
                PaymentId = updated.Id,
                Amount = updated.Amount,
                Timestamp = DateTime.UtcNow
            });
        }
        else
        {
            updated = created with
            {
                Status = PaymentStatuses.Failed,
                FailureReason = failureReason,
                UpdatedAt = DateTime.UtcNow
            };

            await _kafkaProducer.PublishAsync("payment-events", new
            {
                EventType = "PaymentFailed",
                OrderId = request.OrderId,
                PaymentId = updated.Id,
                Reason = failureReason,
                Timestamp = DateTime.UtcNow
            });
        }

        return await _repository.UpdateAsync(updated);
    }

    public async Task<Refund> ProcessRefundAsync(RefundRequest request)
    {
        var payment = await _repository.GetByIdAsync(request.PaymentId)
            ?? throw new KeyNotFoundException($"Payment {request.PaymentId} not found");

        if (payment.Status != PaymentStatuses.Completed)
            throw new InvalidOperationException("Can only refund completed payments");

        var refund = new Refund(
            Id: Guid.NewGuid(),
            PaymentId: request.PaymentId,
            Amount: request.Amount,
            Reason: request.Reason,
            Status: "completed",
            CreatedAt: DateTime.UtcNow
        );

        var created = await _repository.CreateRefundAsync(refund);

        var updatedPayment = payment with { Status = PaymentStatuses.Refunded, UpdatedAt = DateTime.UtcNow };
        await _repository.UpdateAsync(updatedPayment);

        await _kafkaProducer.PublishAsync("payment-events", new
        {
            EventType = "PaymentRefunded",
            OrderId = payment.OrderId,
            PaymentId = payment.Id,
            RefundAmount = request.Amount,
            Timestamp = DateTime.UtcNow
        });

        _logger.LogInformation("Refund processed for payment {PaymentId}", request.PaymentId);
        return created;
    }

    private static Task<(bool Success, string? TransactionId, string? FailureReason)> SimulatePaymentProcessingAsync(decimal amount)
    {
        // Simulate: payments over 10000 fail, others succeed
        if (amount > 10000)
        {
            return Task.FromResult<(bool, string?, string?)>((false, null, "Amount exceeds limit"));
        }

        var transactionId = $"TXN-{Guid.NewGuid().ToString()[..8].ToUpper()}";
        return Task.FromResult<(bool, string?, string?)>((true, transactionId, null));
    }
}
