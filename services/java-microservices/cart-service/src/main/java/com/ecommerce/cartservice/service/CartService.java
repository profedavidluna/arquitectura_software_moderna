package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.*;
import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartItem;
import com.ecommerce.cartservice.entity.CartStatus;
import com.ecommerce.cartservice.exception.*;
import com.ecommerce.cartservice.mapper.CartMapper;
import com.ecommerce.cartservice.repository.CartItemRepository;
import com.ecommerce.cartservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    @Value("${cart.tax-rate:0.08}")
    private BigDecimal taxRate;

    @Value("${cart.free-shipping-threshold:50.00}")
    private BigDecimal freeShippingThreshold;

    @Value("${cart.default-shipping-amount:5.99}")
    private BigDecimal defaultShippingAmount;

    @Value("${cart.expiration-days:30}")
    private int expirationDays;

    public CartResponse createCart(CreateCartRequest request) {
        log.info("Creating cart for userId: {}", request.getUserId());

        Cart cart = Cart.builder()
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .status(CartStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusDays(expirationDays))
                .build();

        Cart savedCart = cartRepository.save(cart);
        log.info("Cart created with id: {}", savedCart.getId());
        return cartMapper.toCartResponse(savedCart);
    }

    @Cacheable(value = "carts", key = "#cartId")
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID cartId) {
        log.debug("Fetching cart: {}", cartId);
        Cart cart = findCartOrThrow(cartId);
        validateCartNotExpired(cart);
        return cartMapper.toCartResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse getActiveCartForUser(UUID userId) {
        log.debug("Fetching active cart for userId: {}", userId);
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException("No active cart found for user: " + userId));
        validateCartNotExpired(cart);
        return cartMapper.toCartResponse(cart);
    }

    @CacheEvict(value = "carts", key = "#cartId")
    public CartResponse addItem(UUID cartId, AddItemRequest request) {
        log.info("Adding item to cart: {}, productId: {}", cartId, request.getProductId());

        Cart cart = findCartOrThrow(cartId);
        validateCartNotExpired(cart);
        validateCartActive(cart);

        // Check if product already exists in cart
        var existingItem = cartItemRepository.findByCartIdAndProductId(cartId, request.getProductId());
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.calculateSubtotal();
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .productId(request.getProductId())
                    .productName(request.getProductName())
                    .productSku(request.getProductSku())
                    .productImageUrl(request.getProductImageUrl())
                    .quantity(request.getQuantity())
                    .unitPrice(request.getUnitPrice())
                    .subtotal(request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
                    .build();
            cart.addItem(newItem);
        }

        recalculateTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        log.info("Item added to cart: {}", cartId);
        return cartMapper.toCartResponse(savedCart);
    }

    @CacheEvict(value = "carts", key = "#cartId")
    public CartResponse updateItemQuantity(UUID cartId, UUID itemId, UpdateItemRequest request) {
        log.info("Updating item quantity in cart: {}, itemId: {}", cartId, itemId);

        Cart cart = findCartOrThrow(cartId);
        validateCartNotExpired(cart);
        validateCartActive(cart);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));

        if (!item.getCart().getId().equals(cartId)) {
            throw new CartItemNotFoundException("Item does not belong to this cart");
        }

        item.setQuantity(request.getQuantity());
        item.calculateSubtotal();
        cartItemRepository.save(item);

        recalculateTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        log.info("Item quantity updated in cart: {}", cartId);
        return cartMapper.toCartResponse(savedCart);
    }

    @CacheEvict(value = "carts", key = "#cartId")
    public CartResponse removeItem(UUID cartId, UUID itemId) {
        log.info("Removing item from cart: {}, itemId: {}", cartId, itemId);

        Cart cart = findCartOrThrow(cartId);
        validateCartActive(cart);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));

        if (!item.getCart().getId().equals(cartId)) {
            throw new CartItemNotFoundException("Item does not belong to this cart");
        }

        cart.removeItem(item);
        cartItemRepository.delete(item);

        recalculateTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        log.info("Item removed from cart: {}", cartId);
        return cartMapper.toCartResponse(savedCart);
    }

    @CacheEvict(value = "carts", key = "#cartId")
    public void clearCart(UUID cartId) {
        log.info("Clearing cart: {}", cartId);

        Cart cart = findCartOrThrow(cartId);
        cart.getItems().clear();
        cart.setItemCount(0);
        cart.setSubtotal(BigDecimal.ZERO);
        cart.setTaxAmount(BigDecimal.ZERO);
        cart.setShippingAmount(BigDecimal.ZERO);
        cart.setDiscountAmount(BigDecimal.ZERO);
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setCouponCode(null);

        cartRepository.save(cart);
        log.info("Cart cleared: {}", cartId);
    }

    @CacheEvict(value = "carts", key = "#cartId")
    public CartResponse applyCoupon(UUID cartId, ApplyCouponRequest request) {
        log.info("Applying coupon to cart: {}, code: {}", cartId, request.getCouponCode());

        Cart cart = findCartOrThrow(cartId);
        validateCartNotExpired(cart);
        validateCartActive(cart);

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot apply coupon to an empty cart");
        }

        BigDecimal discount = calculateCouponDiscount(request.getCouponCode(), cart.getSubtotal());
        cart.setCouponCode(request.getCouponCode());
        cart.setDiscountAmount(discount);

        recalculateTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        log.info("Coupon applied to cart: {}", cartId);
        return cartMapper.toCartResponse(savedCart);
    }

    @CacheEvict(value = "carts", key = "#cartId")
    public CartResponse removeCoupon(UUID cartId) {
        log.info("Removing coupon from cart: {}", cartId);

        Cart cart = findCartOrThrow(cartId);
        validateCartActive(cart);

        cart.setCouponCode(null);
        cart.setDiscountAmount(BigDecimal.ZERO);

        recalculateTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        log.info("Coupon removed from cart: {}", cartId);
        return cartMapper.toCartResponse(savedCart);
    }

    // --- Private helper methods ---

    private Cart findCartOrThrow(UUID cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));
    }

    private void validateCartNotExpired(Cart cart) {
        if (cart.getExpiresAt() != null && cart.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CartExpiredException(cart.getId());
        }
    }

    private void validateCartActive(Cart cart) {
        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new IllegalStateException("Cart is not active. Current status: " + cart.getStatus());
        }
    }

    private void recalculateTotals(Cart cart) {
        // Calculate subtotal from items
        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int itemCount = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        // Calculate tax
        BigDecimal taxableAmount = subtotal.subtract(cart.getDiscountAmount());
        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
            taxableAmount = BigDecimal.ZERO;
        }
        BigDecimal tax = taxableAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);

        // Calculate shipping
        BigDecimal shipping = calculateShipping(subtotal);

        // Calculate total
        BigDecimal total = subtotal.add(tax).add(shipping).subtract(cart.getDiscountAmount());
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        cart.setItemCount(itemCount);
        cart.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        cart.setTaxAmount(tax);
        cart.setShippingAmount(shipping);
        cart.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal calculateShipping(BigDecimal subtotal) {
        if (subtotal.compareTo(freeShippingThreshold) >= 0) {
            return BigDecimal.ZERO;
        }
        return defaultShippingAmount;
    }

    /**
     * Calculate coupon discount based on coupon code pattern:
     * - Codes ending with "PCT" or containing "PERCENT": percentage discount (e.g., "SAVE10PCT" = 10% off)
     * - Codes ending with "OFF": fixed amount discount (e.g., "SAVE10OFF" = $10 off)
     * - Default: 10% discount for valid codes
     */
    BigDecimal calculateCouponDiscount(String couponCode, BigDecimal subtotal) {
        if (couponCode == null || couponCode.isBlank()) {
            throw new InvalidCouponException(couponCode);
        }

        String code = couponCode.toUpperCase().trim();

        // Extract numeric value from coupon code
        String numericPart = code.replaceAll("[^0-9]", "");
        BigDecimal value = numericPart.isEmpty() ? BigDecimal.TEN : new BigDecimal(numericPart);

        if (code.endsWith("PCT") || code.contains("PERCENT")) {
            // Percentage discount
            if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
                value = BigDecimal.valueOf(100);
            }
            return subtotal.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (code.endsWith("OFF")) {
            // Fixed amount discount
            return value.min(subtotal).setScale(2, RoundingMode.HALF_UP);
        } else {
            // Default: 10% discount for any valid-looking code
            return subtotal.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
