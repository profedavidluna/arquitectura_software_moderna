namespace PaymentService.Domain.Interfaces;

using PaymentService.Domain.Models;

public interface IPaymentRepository
{
    Task<Payment?> GetByIdAsync(Guid id);
    Task<Payment?> GetByOrderIdAsync(Guid orderId);
    Task<IEnumerable<Payment>> GetByUserIdAsync(Guid userId);
    Task<Payment> CreateAsync(Payment payment);
    Task<Payment> UpdateAsync(Payment payment);
    Task<Refund> CreateRefundAsync(Refund refund);
    Task<IEnumerable<Refund>> GetRefundsByPaymentIdAsync(Guid paymentId);
}

public interface IPaymentService
{
    Task<Payment?> GetPaymentByIdAsync(Guid id);
    Task<Payment?> GetPaymentByOrderIdAsync(Guid orderId);
    Task<Payment> ProcessPaymentAsync(ProcessPaymentRequest request);
    Task<Refund> ProcessRefundAsync(RefundRequest request);
    Task<IEnumerable<Payment>> GetPaymentsByUserIdAsync(Guid userId);
}
