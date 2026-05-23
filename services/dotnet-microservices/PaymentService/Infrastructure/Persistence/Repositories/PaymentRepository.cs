namespace PaymentService.Infrastructure.Persistence.Repositories;

using Microsoft.EntityFrameworkCore;
using PaymentService.Domain.Interfaces;
using PaymentService.Domain.Models;
using PaymentService.Infrastructure.Persistence.Entities;

public class PaymentRepository : IPaymentRepository
{
    private readonly AppDbContext _context;

    public PaymentRepository(AppDbContext context) => _context = context;

    public async Task<Payment?> GetByIdAsync(Guid id)
    {
        var entity = await _context.Payments.FindAsync(id);
        return entity == null ? null : MapToDomain(entity);
    }

    public async Task<Payment?> GetByOrderIdAsync(Guid orderId)
    {
        var entity = await _context.Payments.FirstOrDefaultAsync(p => p.OrderId == orderId);
        return entity == null ? null : MapToDomain(entity);
    }

    public async Task<IEnumerable<Payment>> GetByUserIdAsync(Guid userId)
    {
        var entities = await _context.Payments.Where(p => p.UserId == userId).ToListAsync();
        return entities.Select(MapToDomain);
    }

    public async Task<Payment> CreateAsync(Payment payment)
    {
        var entity = MapToEntity(payment);
        _context.Payments.Add(entity);
        await _context.SaveChangesAsync();
        return MapToDomain(entity);
    }

    public async Task<Payment> UpdateAsync(Payment payment)
    {
        var entity = await _context.Payments.FindAsync(payment.Id)
            ?? throw new KeyNotFoundException($"Payment {payment.Id} not found");
        entity.Status = payment.Status;
        entity.TransactionId = payment.TransactionId;
        entity.FailureReason = payment.FailureReason;
        entity.UpdatedAt = payment.UpdatedAt;
        await _context.SaveChangesAsync();
        return MapToDomain(entity);
    }

    public async Task<Refund> CreateRefundAsync(Refund refund)
    {
        var entity = new RefundEntity
        {
            Id = refund.Id, PaymentId = refund.PaymentId, Amount = refund.Amount,
            Reason = refund.Reason, Status = refund.Status, CreatedAt = refund.CreatedAt
        };
        _context.Refunds.Add(entity);
        await _context.SaveChangesAsync();
        return new Refund(entity.Id, entity.PaymentId, entity.Amount, entity.Reason, entity.Status, entity.CreatedAt);
    }

    public async Task<IEnumerable<Refund>> GetRefundsByPaymentIdAsync(Guid paymentId)
    {
        var entities = await _context.Refunds.Where(r => r.PaymentId == paymentId).ToListAsync();
        return entities.Select(e => new Refund(e.Id, e.PaymentId, e.Amount, e.Reason, e.Status, e.CreatedAt));
    }

    private static Payment MapToDomain(PaymentEntity e) => new(
        e.Id, e.OrderId, e.UserId, e.Amount, e.Currency, e.Status,
        e.PaymentMethod, e.TransactionId, e.FailureReason, e.CreatedAt, e.UpdatedAt
    );

    private static PaymentEntity MapToEntity(Payment p) => new()
    {
        Id = p.Id, OrderId = p.OrderId, UserId = p.UserId, Amount = p.Amount,
        Currency = p.Currency, Status = p.Status, PaymentMethod = p.PaymentMethod,
        TransactionId = p.TransactionId, FailureReason = p.FailureReason,
        CreatedAt = p.CreatedAt, UpdatedAt = p.UpdatedAt
    };
}
