package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.*;
import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartItem;
import com.ecommerce.cartservice.entity.CartStatus;
import com.ecommerce.cartservice.exception.CartExpiredException;
import com.ecommerce.cartservice.exception.CartItemNotFoundException;
import com.ecommerce.cartservice.exception.CartNotFoundException;
import com.ecommerce.cartservice.mapper.CartMapper;
import com.ecommerce.cartservice.repository.CartItemRepository;
import com.ecommerce.cartservice.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartService cartService;

    private UUID cartId;
    private UUID userId;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        cartId = UUID.randomUUID();
        userId = UUID.randomUUID();

        ReflectionTestUtils.setField(cartService, "taxRate", new BigDecimal("0.08"));
        ReflectionTestUtils.setField(cartService, "freeShippingThreshold", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(cartService, "defaultShippingAmount", new BigDecimal("5.99"));
        ReflectionTestUtils.setField(cartService, "expirationDays", 30);

        testCart = Cart.builder()
                .id(cartId)
                .userId(userId)
                .status(CartStatus.ACTIVE)
                .itemCount(0)
                .subtotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .currency("USD")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .items(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("Create Cart Tests")
    class CreateCartTests {

        @Test
        @DisplayName("Should create a new cart successfully")
        void shouldCreateCart() {
            CreateCartRequest request = CreateCartRequest.builder()
                    .userId(userId)
                    .sessionId("session-123")
                    .currency("USD")
                    .build();

            CartResponse expectedResponse = CartResponse.builder()
                    .id(cartId)
                    .userId(userId)
                    .status(CartStatus.ACTIVE)
                    .build();

            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
            when(cartMapper.toCartResponse(any(Cart.class))).thenReturn(expectedResponse);

            CartResponse result = cartService.createCart(request);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getStatus()).isEqualTo(CartStatus.ACTIVE);
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should use default currency when not specified")
        void shouldUseDefaultCurrency() {
            CreateCartRequest request = CreateCartRequest.builder()
                    .userId(userId)
                    .build();

            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
            when(cartMapper.toCartResponse(any(Cart.class))).thenReturn(CartResponse.builder().build());

            cartService.createCart(request);

            verify(cartRepository).save(argThat(cart -> "USD".equals(cart.getCurrency())));
        }
    }

    @Nested
    @DisplayName("Get Cart Tests")
    class GetCartTests {

        @Test
        @DisplayName("Should get cart by ID")
        void shouldGetCartById() {
            CartResponse expectedResponse = CartResponse.builder().id(cartId).build();

            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
            when(cartMapper.toCartResponse(testCart)).thenReturn(expectedResponse);

            CartResponse result = cartService.getCart(cartId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(cartId);
        }

        @Test
        @DisplayName("Should throw exception when cart not found")
        void shouldThrowWhenCartNotFound() {
            when(cartRepository.findById(cartId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.getCart(cartId))
                    .isInstanceOf(CartNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when cart is expired")
        void shouldThrowWhenCartExpired() {
            testCart.setExpiresAt(LocalDateTime.now().minusDays(1));
            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));

            assertThatThrownBy(() -> cartService.getCart(cartId))
                    .isInstanceOf(CartExpiredException.class);
        }

        @Test
        @DisplayName("Should get active cart for user")
        void shouldGetActiveCartForUser() {
            CartResponse expectedResponse = CartResponse.builder().id(cartId).userId(userId).build();

            when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE))
                    .thenReturn(Optional.of(testCart));
            when(cartMapper.toCartResponse(testCart)).thenReturn(expectedResponse);

            CartResponse result = cartService.getActiveCartForUser(userId);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("Add Item Tests")
    class AddItemTests {

        @Test
        @DisplayName("Should add new item to cart")
        void shouldAddNewItem() {
            AddItemRequest request = AddItemRequest.builder()
                    .productId(UUID.randomUUID())
                    .productName("Test Product")
                    .productSku("SKU-001")
                    .quantity(2)
                    .unitPrice(new BigDecimal("25.00"))
                    .build();

            CartResponse expectedResponse = CartResponse.builder()
                    .id(cartId)
                    .itemCount(2)
                    .subtotal(new BigDecimal("50.00"))
                    .build();

            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
            when(cartItemRepository.findByCartIdAndProductId(cartId, request.getProductId()))
                    .thenReturn(Optional.empty());
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
            when(cartMapper.toCartResponse(any(Cart.class))).thenReturn(expectedResponse);

            CartResponse result = cartService.addItem(cartId, request);

            assertThat(result).isNotNull();
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should update quantity when product already in cart")
        void shouldUpdateQuantityWhenProductExists() {
            UUID productId = UUID.randomUUID();
            CartItem existingItem = CartItem.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .quantity(1)
                    .unitPrice(new BigDecimal("25.00"))
                    .subtotal(new BigDecimal("25.00"))
                    .build();
            existingItem.setCart(testCart);

            AddItemRequest request = AddItemRequest.builder()
                    .productId(productId)
                    .productName("Test Product")
                    .productSku("SKU-001")
                    .quantity(2)
                    .unitPrice(new BigDecimal("25.00"))
                    .build();

            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
            when(cartItemRepository.findByCartIdAndProductId(cartId, productId))
                    .thenReturn(Optional.of(existingItem));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
            when(cartMapper.toCartResponse(any(Cart.class))).thenReturn(CartResponse.builder().build());

            cartService.addItem(cartId, request);

            assertThat(existingItem.getQuantity()).isEqualTo(3);
            verify(cartItemRepository).save(existingItem);
        }

        @Test
        @DisplayName("Should throw when adding to non-active cart")
        void shouldThrowWhenCartNotActive() {
            testCart.setStatus(CartStatus.CONVERTED);
            AddItemRequest request = AddItemRequest.builder()
                    .productId(UUID.randomUUID())
                    .productName("Test")
                    .productSku("SKU")
                    .quantity(1)
                    .unitPrice(BigDecimal.TEN)
                    .build();

            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));

            assertThatThrownBy(() -> cartService.addItem(cartId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not active");
        }
    }

    @Nested
    @DisplayName("Update Item Quantity Tests")
    class UpdateItemTests {

        @Test
        @DisplayName("Should update item quantity")
        void shouldUpdateItemQuantity() {
            UUID itemId = UUID.randomUUID();
            CartItem item = CartItem.builder()
                    .id(itemId)
                    .quantity(1)
                    .unitPrice(new BigDecimal("10.00"))
                    .subtotal(new BigDecimal("10.00"))
                    .build();
            item.setCart(testCart);
            testCart.getItems().add(item);

            UpdateItemRequest request = UpdateItemRequest.builder().quantity(5).build();

            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
            when(cartItemRepository.findById(itemId)).thenReturn(Optional.of(item));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
            when(cartMapper.toCartResponse(any(Cart.class))).thenReturn(CartResponse.builder().build());

            cartService.updateItemQuantity(cartId, itemId, request);

            assertThat(item.getQuantity()).isEqualTo(5);
            verify(cartItemRepository).save(item);
        }

        @Test
        @DisplayName("Should throw when item not found")
        void shouldThrowWhenItemNotFound() {
            UUID itemId = UUID.randomUUID();
            UpdateItemRequest request = UpdateItemRequest.builder().quantity(5).build();

            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
            when(cartItemRepository.findById(itemId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateItemQuantity(cartId, itemId, request))
                    .isInstanceOf(CartItemNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw when item does not belong to cart")
        void shouldThrowWhenItemNotInCart() {
            UUID itemId = UUID.randomUUID();
            Cart otherCart = Cart.builder().id(UUID.randomUUID()).build();
            CartItem item = CartItem.builder().id(itemId).build();
            item.setCart(otherCart);

            UpdateItemRequest request = UpdateItemRequest.builder().quantity(5).build();

            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
            when(cartItemRepository.findById(itemId)).thenReturn(Optional.of(item));

            assertThatThrownBy(() -> cartService.updateItemQuantity(cartId, itemId, request))
                    .isInstanceOf(CartItemNotFoundException.class)
                    .hasMessageContaining("does not belong");
        }
    }

    @Nested
    @DisplayName("Remove Item Tests")
    class RemoveItemTests {

        @Test
        @DisplayName("Should remove item from cart")
        void shouldRemoveItem() {
            UUID itemId = UUID.randomUUID();
            CartItem item = CartItem.builder()
                    .id(itemId)
                    .quantity(1)
                    .unitPrice(new BigDecimal("10.00"))
                    .subtotal(new BigDecimal("10.00"))
                    .build();
            item.setCart(testCart);
            testCart.getItems().add(item);

            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
            when(cartItemRepository.findById(itemId)).thenReturn(Optional.of(item));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
            when(cartMapper.toCartResponse(any(Cart.class))).thenReturn(CartResponse.builder().build());

            cartService.removeItem(cartId, itemId);

            verify(cartItemRepository).delete(item);
            verify(cartRepository).save(any(Cart.class));
        }
    }

    @Nested
    @DisplayName("Clear Cart Tests")
    class ClearCartTests {

        @Test
        @DisplayName("Should clear all items from cart")
        void shouldClearCart() {
            CartItem item = CartItem.builder()
                    .id(UUID.randomUUID())
                    .quantity(1)
                    .unitPrice(BigDecimal.TEN)
                    .subtotal(BigDecimal.TEN)
                    .build();
            item.setCart(testCart);
            testCart.getItems().add(item);
            testCart.setItemCount(1);
            testCart.setSubtotal(BigDecimal.TEN);

            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));

            cartService.clearCart(cartId);

            assertThat(testCart.getItems()).isEmpty();
            assertThat(testCart.getItemCount()).isZero();
            assertThat(testCart.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(testCart.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(testCart.getCouponCode()).isNull();
            verify(cartRepository).save(testCart);
        }
    }

    @Nested
    @DisplayName("Coupon Tests")
    class CouponTests {

        @Test
        @DisplayName("Should apply percentage coupon")
        void shouldApplyPercentageCoupon() {
            CartItem item = CartItem.builder()
                    .id(UUID.randomUUID())
                    .quantity(2)
                    .unitPrice(new BigDecimal("50.00"))
                    .subtotal(new BigDecimal("100.00"))
                    .build();
            item.setCart(testCart);
            testCart.getItems().add(item);
            testCart.setSubtotal(new BigDecimal("100.00"));

            ApplyCouponRequest request = ApplyCouponRequest.builder()
                    .couponCode("SAVE20PCT")
                    .build();

            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
            when(cartMapper.toCartResponse(any(Cart.class))).thenReturn(CartResponse.builder().build());

            cartService.applyCoupon(cartId, request);

            assertThat(testCart.getCouponCode()).isEqualTo("SAVE20PCT");
            assertThat(testCart.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("Should apply fixed amount coupon")
        void shouldApplyFixedCoupon() {
            CartItem item = CartItem.builder()
                    .id(UUID.randomUUID())
                    .quantity(2)
                    .unitPrice(new BigDecimal("50.00"))
                    .subtotal(new BigDecimal("100.00"))
                    .build();
            item.setCart(testCart);
            testCart.getItems().add(item);
            testCart.setSubtotal(new BigDecimal("100.00"));

            ApplyCouponRequest request = ApplyCouponRequest.builder()
                    .couponCode("SAVE15OFF")
                    .build();

            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
            when(cartMapper.toCartResponse(any(Cart.class))).thenReturn(CartResponse.builder().build());

            cartService.applyCoupon(cartId, request);

            assertThat(testCart.getCouponCode()).isEqualTo("SAVE15OFF");
            assertThat(testCart.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("15.00"));
        }

        @Test
        @DisplayName("Should throw when applying coupon to empty cart")
        void shouldThrowWhenCartEmpty() {
            ApplyCouponRequest request = ApplyCouponRequest.builder()
                    .couponCode("SAVE10PCT")
                    .build();

            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));

            assertThatThrownBy(() -> cartService.applyCoupon(cartId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("empty cart");
        }

        @Test
        @DisplayName("Should remove coupon from cart")
        void shouldRemoveCoupon() {
            testCart.setCouponCode("SAVE10PCT");
            testCart.setDiscountAmount(new BigDecimal("10.00"));

            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
            when(cartMapper.toCartResponse(any(Cart.class))).thenReturn(CartResponse.builder().build());

            cartService.removeCoupon(cartId);

            assertThat(testCart.getCouponCode()).isNull();
            assertThat(testCart.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Coupon Discount Calculation Tests")
    class CouponDiscountCalculationTests {

        @Test
        @DisplayName("Should calculate percentage discount correctly")
        void shouldCalculatePercentageDiscount() {
            BigDecimal subtotal = new BigDecimal("100.00");
            BigDecimal discount = cartService.calculateCouponDiscount("SAVE10PCT", subtotal);
            assertThat(discount).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("Should calculate fixed discount correctly")
        void shouldCalculateFixedDiscount() {
            BigDecimal subtotal = new BigDecimal("100.00");
            BigDecimal discount = cartService.calculateCouponDiscount("SAVE25OFF", subtotal);
            assertThat(discount).isEqualByComparingTo(new BigDecimal("25.00"));
        }

        @Test
        @DisplayName("Should cap fixed discount at subtotal")
        void shouldCapFixedDiscountAtSubtotal() {
            BigDecimal subtotal = new BigDecimal("10.00");
            BigDecimal discount = cartService.calculateCouponDiscount("SAVE25OFF", subtotal);
            assertThat(discount).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("Should apply default 10% for generic codes")
        void shouldApplyDefaultDiscount() {
            BigDecimal subtotal = new BigDecimal("100.00");
            BigDecimal discount = cartService.calculateCouponDiscount("WELCOME", subtotal);
            assertThat(discount).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("Should cap percentage at 100%")
        void shouldCapPercentageAt100() {
            BigDecimal subtotal = new BigDecimal("100.00");
            BigDecimal discount = cartService.calculateCouponDiscount("SAVE150PCT", subtotal);
            assertThat(discount).isEqualByComparingTo(new BigDecimal("100.00"));
        }
    }
}
