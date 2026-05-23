namespace PaymentService.Infrastructure.Web.Controllers;

using Microsoft.AspNetCore.Mvc;
using PaymentService.Domain.Interfaces;
using PaymentService.Domain.Models;
using PaymentService.Infrastructure.Web.Dto;

[ApiController]
[Route("api/[controller]")]
public class PaymentController : ControllerBase
{
    private readonly IPaymentService _paymentService;

    public PaymentController(IPaymentService paymentService) => _paymentService = paymentService;

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<PaymentDto>> GetById(Guid id)
    {
        var payment = await _paymentService.GetPaymentByIdAsync(id);
        if (payment == null) return NotFound();
        return Ok(MapToDto(payment));
    }

    [HttpGet("order/{orderId:guid}")]
    public async Task<ActionResult<PaymentDto>> GetByOrderId(Guid orderId)
    {
        var payment = await _paymentService.GetPaymentByOrderIdAsync(orderId);
        if (payment == null) return NotFound();
        return Ok(MapToDto(payment));
    }

    [HttpGet("user/{userId:guid}")]
    public async Task<ActionResult<IEnumerable<PaymentDto>>> GetByUserId(Guid userId)
    {
        var payments = await _paymentService.GetPaymentsByUserIdAsync(userId);
        return Ok(payments.Select(MapToDto));
    }

    [HttpPost]
    public async Task<ActionResult<PaymentDto>> ProcessPayment([FromBody] ProcessPaymentRequest request)
    {
        var payment = await _paymentService.ProcessPaymentAsync(request);
        return CreatedAtAction(nameof(GetById), new { id = payment.Id }, MapToDto(payment));
    }

    [HttpPost("refund")]
    public async Task<ActionResult<RefundDto>> ProcessRefund([FromBody] RefundRequest request)
    {
        try
        {
            var refund = await _paymentService.ProcessRefundAsync(request);
            return Created($"/api/payment/refund/{refund.Id}", new RefundDto(refund.Id, refund.PaymentId, refund.Amount, refund.Reason, refund.Status, refund.CreatedAt));
        }
        catch (KeyNotFoundException) { return NotFound(); }
        catch (InvalidOperationException ex) { return BadRequest(new { message = ex.Message }); }
    }

    private static PaymentDto MapToDto(Payment p) => new(
        p.Id, p.OrderId, p.UserId, p.Amount, p.Currency, p.Status,
        p.PaymentMethod, p.TransactionId, p.FailureReason, p.CreatedAt
    );
}
