using Xunit;
using Moq;

namespace PaymentService.Tests;

using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using PaymentService.Application;
using PaymentService.Domain.Models;
using PaymentService.Infrastructure.Messaging;
using PaymentService.Infrastructure.Persistence;
using PaymentService.Infrastructure.Persistence.Repositories;

public class PaymentServiceTests
{
    private readonly PaymentServiceImpl _service;

    public PaymentServiceTests()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        var context = new AppDbContext(options);
        var repository = new PaymentRepository(context);
        var config = new ConfigurationBuilder().AddInMemoryCollection().Build();
        var kafka = new KafkaProducer(config, NullLogger<KafkaProducer>.Instance);
        _service = new PaymentServiceImpl(repository, kafka, NullLogger<PaymentServiceImpl>.Instance);
    }

    [Fact]
    public async Task ProcessPayment_ValidAmount_ShouldSucceed()
    {
        var request = new ProcessPaymentRequest(Guid.NewGuid(), Guid.NewGuid(), 99.99m, "credit_card");

        var result = await _service.ProcessPaymentAsync(request);

        Assert.Equal(PaymentStatuses.Completed, result.Status);
        Assert.NotNull(result.TransactionId);
        Assert.Null(result.FailureReason);
    }

    [Fact]
    public async Task ProcessPayment_ExceedsLimit_ShouldFail()
    {
        var request = new ProcessPaymentRequest(Guid.NewGuid(), Guid.NewGuid(), 15000m, "credit_card");

        var result = await _service.ProcessPaymentAsync(request);

        Assert.Equal(PaymentStatuses.Failed, result.Status);
        Assert.NotNull(result.FailureReason);
    }

    [Fact]
    public async Task ProcessRefund_CompletedPayment_ShouldSucceed()
    {
        var paymentRequest = new ProcessPaymentRequest(Guid.NewGuid(), Guid.NewGuid(), 50m, "credit_card");
        var payment = await _service.ProcessPaymentAsync(paymentRequest);

        var refund = await _service.ProcessRefundAsync(new RefundRequest(payment.Id, 50m, "Customer request"));

        Assert.Equal("completed", refund.Status);
        Assert.Equal(50m, refund.Amount);
    }

    [Fact]
    public async Task ProcessRefund_FailedPayment_ShouldThrow()
    {
        var paymentRequest = new ProcessPaymentRequest(Guid.NewGuid(), Guid.NewGuid(), 15000m, "credit_card");
        var payment = await _service.ProcessPaymentAsync(paymentRequest);

        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.ProcessRefundAsync(new RefundRequest(payment.Id, 15000m, "Refund")));
    }

    [Fact]
    public async Task GetPaymentByOrderId_ShouldReturnPayment()
    {
        var orderId = Guid.NewGuid();
        await _service.ProcessPaymentAsync(new ProcessPaymentRequest(orderId, Guid.NewGuid(), 25m, "debit_card"));

        var found = await _service.GetPaymentByOrderIdAsync(orderId);

        Assert.NotNull(found);
        Assert.Equal(orderId, found.OrderId);
    }
}

