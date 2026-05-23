using Microsoft.AspNetCore.Mvc;
using OrderService.Domain.Interfaces;
using OrderService.Domain.Models;
using OrderService.Infrastructure.Web.Dto;

namespace OrderService.Infrastructure.Web;

/// <summary>
/// REST API controller for Order Service.
/// 
/// SOA Principle: Service Reusability - exposes order management
/// through a standardized REST interface that can be consumed by
/// multiple clients (web, mobile, other services).
/// </summary>
[ApiController]
[Route("api/[controller]")]
public class OrderController : ControllerBase
{
    private readonly IOrderService _orderService;
    private readonly ILogger<OrderController> _logger;

    public OrderController(IOrderService orderService, ILogger<OrderController> logger)
    {
        _orderService = orderService;
        _logger = logger;
    }

    /// <summary>GET /api/order - Retrieve all orders</summary>
    [HttpGet]
    public async Task<ActionResult<IEnumerable<OrderResponse>>> GetAll()
    {
        var orders = await _orderService.GetAllOrdersAsync();
        var response = orders.Select(MapToResponse);
        return Ok(response);
    }

    /// <summary>GET /api/order/{id} - Retrieve an order by ID</summary>
    [HttpGet("{id:guid}")]
    public async Task<ActionResult<OrderResponse>> GetById(Guid id)
    {
        var order = await _orderService.GetOrderByIdAsync(id);
        if (order is null)
            return NotFound(new { message = $"Order with id {id} not found" });

        return Ok(MapToResponse(order));
    }

    /// <summary>POST /api/order - Create a new order (initiates Saga)</summary>
    [HttpPost]
    public async Task<ActionResult<OrderResponse>> Create([FromBody] CreateOrderRequest request)
    {
        if (!ModelState.IsValid)
            return BadRequest(ModelState);

        _logger.LogInformation("Creating order for customer: {CustomerName}", request.CustomerName);

        var items = request.Items.Select(i => new OrderItem
        {
            ProductId = i.ProductId,
            ProductName = i.ProductName,
            Quantity = i.Quantity,
            UnitPrice = i.UnitPrice
        }).ToList();

        var order = await _orderService.CreateOrderAsync(
            request.CustomerName, request.CustomerEmail, items);

        var response = MapToResponse(order);
        return CreatedAtAction(nameof(GetById), new { id = order.Id }, response);
    }

    private static OrderResponse MapToResponse(Order order) => new()
    {
        Id = order.Id,
        CustomerName = order.CustomerName,
        CustomerEmail = order.CustomerEmail,
        Status = order.Status.ToString(),
        TotalAmount = order.TotalAmount,
        Items = order.Items.Select(i => new OrderItemResponse
        {
            Id = i.Id,
            ProductId = i.ProductId,
            ProductName = i.ProductName,
            Quantity = i.Quantity,
            UnitPrice = i.UnitPrice
        }).ToList(),
        CreatedAt = order.CreatedAt,
        UpdatedAt = order.UpdatedAt
    };
}
