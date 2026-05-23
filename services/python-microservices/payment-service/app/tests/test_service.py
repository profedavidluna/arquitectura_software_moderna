import pytest
from unittest.mock import AsyncMock, patch
from decimal import Decimal
from app.application.service import PaymentService, PaymentProcessingError
from app.domain.models import Payment, Refund


@pytest.fixture
def mock_repository():
    return AsyncMock()


@pytest.fixture
def mock_producer():
    return AsyncMock()


@pytest.fixture
def payment_service(mock_repository, mock_producer):
    return PaymentService(
        repository=mock_repository, producer=mock_producer,
        max_retries=3, retry_base_delay=0.01,  # Fast retries for tests
    )


@pytest.mark.asyncio
async def test_process_payment_success(payment_service, mock_repository, mock_producer):
    mock_repository.create.return_value = Payment(
        id="pay-1", order_id="order-1", user_id="user-1",
        amount=Decimal("50.00"), status="PENDING"
    )

    result = await payment_service.process_payment(
        order_id="order-1", user_id="user-1", amount=Decimal("50.00")
    )

    assert result.status == "COMPLETED"
    mock_repository.update_status.assert_called_once()
    mock_producer.publish.assert_called()


@pytest.mark.asyncio
async def test_process_payment_low_amount_fails(payment_service, mock_repository, mock_producer):
    mock_repository.create.return_value = Payment(
        id="pay-2", order_id="order-2", user_id="user-1",
        amount=Decimal("0.50"), status="PENDING"
    )

    result = await payment_service.process_payment(
        order_id="order-2", user_id="user-1", amount=Decimal("0.50")
    )

    assert result.status == "FAILED"


@pytest.mark.asyncio
async def test_refund_payment_success(payment_service, mock_repository, mock_producer):
    mock_repository.find_by_id.return_value = Payment(
        id="pay-1", order_id="order-1", user_id="user-1",
        amount=Decimal("100.00"), status="COMPLETED"
    )
    mock_repository.create_refund.return_value = Refund(
        id="ref-1", payment_id="pay-1", amount=Decimal("100.00"), status="COMPLETED"
    )

    result = await payment_service.refund_payment("pay-1")

    assert result.status == "COMPLETED"
    assert result.amount == Decimal("100.00")
    mock_repository.update_status.assert_called_with("pay-1", "REFUNDED")


@pytest.mark.asyncio
async def test_refund_non_completed_payment_fails(payment_service, mock_repository):
    mock_repository.find_by_id.return_value = Payment(
        id="pay-1", order_id="order-1", user_id="user-1",
        amount=Decimal("100.00"), status="PENDING"
    )

    with pytest.raises(ValueError, match="Cannot refund"):
        await payment_service.refund_payment("pay-1")


@pytest.mark.asyncio
async def test_refund_exceeds_amount_fails(payment_service, mock_repository):
    mock_repository.find_by_id.return_value = Payment(
        id="pay-1", order_id="order-1", user_id="user-1",
        amount=Decimal("50.00"), status="COMPLETED"
    )

    with pytest.raises(ValueError, match="exceeds"):
        await payment_service.refund_payment("pay-1", amount=Decimal("100.00"))
