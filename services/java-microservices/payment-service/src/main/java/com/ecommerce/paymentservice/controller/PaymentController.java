package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.dto.*;
import com.ecommerce.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment processing and management endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Process a payment", description = "Process a new payment transaction")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment processed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate payment"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Payment processing failed")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request) {
        log.info("POST /api/v1/payments - orderId={}", request.getOrderId());
        TransactionResponse response = paymentService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Payment processed successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID", description = "Retrieve a payment transaction by its ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> getPaymentById(
            @Parameter(description = "Transaction ID") @PathVariable UUID id) {
        log.info("GET /api/v1/payments/{}", id);
        TransactionResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payments for order", description = "Retrieve all payment transactions for a specific order")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getPaymentsByOrderId(
            @Parameter(description = "Order ID") @PathVariable UUID orderId) {
        log.info("GET /api/v1/payments/order/{}", orderId);
        List<TransactionResponse> responses = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Process refund", description = "Process a full or partial refund for a transaction")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refund processed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Refund processing failed")
    })
    public ResponseEntity<ApiResponse<RefundResponse>> processRefund(
            @Parameter(description = "Transaction ID") @PathVariable UUID id,
            @Valid @RequestBody RefundRequest request) {
        log.info("POST /api/v1/payments/{}/refund", id);
        RefundResponse response = paymentService.processRefund(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Refund processed successfully"));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user payment history", description = "Retrieve paginated payment history for a user")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getUserPaymentHistory(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("GET /api/v1/payments/user/{}", userId);
        Page<TransactionResponse> responses = paymentService.getUserPaymentHistory(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry failed payment", description = "Retry a failed payment transaction with exponential backoff")
    public ResponseEntity<ApiResponse<TransactionResponse>> retryPayment(
            @Parameter(description = "Transaction ID") @PathVariable UUID id) {
        log.info("POST /api/v1/payments/{}/retry", id);
        TransactionResponse response = paymentService.retryFailedPayment(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment retry processed"));
    }

    @PostMapping("/methods")
    @Operation(summary = "Save payment method", description = "Save a tokenized payment method for a user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment method saved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate payment method")
    })
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> savePaymentMethod(
            @Valid @RequestBody SavePaymentMethodRequest request) {
        log.info("POST /api/v1/payments/methods - userId={}", request.getUserId());
        PaymentMethodResponse response = paymentService.savePaymentMethod(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Payment method saved successfully"));
    }

    @GetMapping("/methods/user/{userId}")
    @Operation(summary = "Get user payment methods", description = "Retrieve all active payment methods for a user")
    public ResponseEntity<ApiResponse<List<PaymentMethodResponse>>> getUserPaymentMethods(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        log.info("GET /api/v1/payments/methods/user/{}", userId);
        List<PaymentMethodResponse> responses = paymentService.getUserPaymentMethods(userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
