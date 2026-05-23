package com.ecommerce.paymentservice.mapper;

import com.ecommerce.paymentservice.dto.*;
import com.ecommerce.paymentservice.entity.PaymentMethod;
import com.ecommerce.paymentservice.entity.Refund;
import com.ecommerce.paymentservice.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    TransactionResponse toTransactionResponse(Transaction transaction);

    @Mapping(target = "transactionId", source = "transaction.id")
    RefundResponse toRefundResponse(Refund refund);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "provider", constant = "STRIPE")
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaymentMethod toPaymentMethodEntity(SavePaymentMethodRequest request);

    PaymentMethodResponse toPaymentMethodResponse(PaymentMethod paymentMethod);
}
