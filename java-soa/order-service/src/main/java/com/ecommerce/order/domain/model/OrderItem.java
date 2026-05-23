package com.ecommerce.order.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Order Item Domain Model
 * 
 * <p>Represents a single line item within an order.
 * Contains product reference and quantity information.</p>
 * 
 * <p><b>SOA - Data Ownership</b>: The Order Service stores a snapshot of
 * product information (name, price) at the time of order. This ensures
 * the order remains consistent even if the product is later modified
 * in the Product Service.</p>
 */
public class OrderItem {

    private UUID id;
    private UUID productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public OrderItem() {
        this.id = UUID.randomUUID();
    }

    public OrderItem(UUID id, UUID productId, String productName,
                     int quantity, BigDecimal unitPrice) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Factory method to create an order item with calculated subtotal.
     */
    public static OrderItem create(UUID productId, String productName,
                                    int quantity, BigDecimal unitPrice) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setProductName(productName);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        return item;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
