import pytest
from unittest.mock import AsyncMock, patch
from decimal import Decimal
from app.application.service import CartService
from app.domain.models import Cart, CartItem
from app.infrastructure.web.circuit_breaker import CircuitBreaker, CircuitBreakerOpenError


@pytest.fixture
def mock_repository():
    return AsyncMock()


@pytest.fixture
def mock_producer():
    return AsyncMock()


@pytest.fixture
def mock_cache():
    return AsyncMock()


@pytest.fixture
def mock_http_client():
    return AsyncMock()


@pytest.fixture
def circuit_breaker():
    return CircuitBreaker(name="test", threshold=3, timeout=10.0)


@pytest.fixture
def cart_service(mock_repository, mock_producer, mock_cache, mock_http_client, circuit_breaker):
    return CartService(
        repository=mock_repository, producer=mock_producer,
        cache=mock_cache, http_client=mock_http_client,
        circuit_breaker=circuit_breaker, product_service_url="http://test:7083",
    )


@pytest.mark.asyncio
async def test_get_or_create_cart_existing(cart_service, mock_repository):
    existing_cart = Cart(id="cart-1", user_id="user-1", status="ACTIVE")
    mock_repository.find_by_user.return_value = existing_cart

    result = await cart_service.get_or_create_cart("user-1")

    assert result.id == "cart-1"
    mock_repository.create.assert_not_called()


@pytest.mark.asyncio
async def test_get_or_create_cart_new(cart_service, mock_repository):
    mock_repository.find_by_user.return_value = None
    mock_repository.create.return_value = Cart(id="new-cart", user_id="user-1")

    result = await cart_service.get_or_create_cart("user-1")

    assert result.id == "new-cart"
    mock_repository.create.assert_called_once()


@pytest.mark.asyncio
async def test_apply_valid_coupon(cart_service, mock_repository):
    mock_repository.find_by_id.return_value = Cart(
        id="cart-1", user_id="user-1", discount_percent=Decimal("10")
    )

    result = await cart_service.apply_coupon("cart-1", "SAVE10")

    mock_repository.apply_coupon.assert_called_once_with("cart-1", "SAVE10", 10.0)


@pytest.mark.asyncio
async def test_apply_invalid_coupon(cart_service):
    with pytest.raises(ValueError, match="Invalid coupon"):
        await cart_service.apply_coupon("cart-1", "INVALID")


def test_circuit_breaker_opens_after_threshold():
    cb = CircuitBreaker(name="test", threshold=3, timeout=10.0)

    for _ in range(3):
        cb._on_failure()

    assert cb.state == "OPEN"
    assert cb.is_open is True
