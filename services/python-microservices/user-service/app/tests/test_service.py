import pytest
from unittest.mock import AsyncMock, MagicMock
from app.application.service import UserService
from app.domain.models import User, Address


@pytest.fixture
def mock_repository():
    repo = AsyncMock()
    return repo


@pytest.fixture
def mock_producer():
    producer = AsyncMock()
    return producer


@pytest.fixture
def user_service(mock_repository, mock_producer):
    return UserService(mock_repository, mock_producer)


@pytest.mark.asyncio
async def test_create_user_success(user_service, mock_repository, mock_producer):
    mock_repository.find_by_email.return_value = None
    mock_repository.create.return_value = User(
        id="test-id", email="test@example.com", password_hash="hashed",
        first_name="John", last_name="Doe", status="ACTIVE"
    )

    result = await user_service.create_user(
        email="test@example.com", password="password123",
        first_name="John", last_name="Doe"
    )

    assert result.email == "test@example.com"
    assert result.first_name == "John"
    mock_repository.create.assert_called_once()
    mock_producer.publish.assert_called_once()


@pytest.mark.asyncio
async def test_create_user_duplicate_email(user_service, mock_repository):
    mock_repository.find_by_email.return_value = User(
        id="existing-id", email="test@example.com", first_name="Existing", last_name="User"
    )

    with pytest.raises(ValueError, match="already exists"):
        await user_service.create_user(
            email="test@example.com", password="password123",
            first_name="John", last_name="Doe"
        )


@pytest.mark.asyncio
async def test_get_user_found(user_service, mock_repository):
    mock_repository.find_by_id.return_value = User(
        id="test-id", email="test@example.com", first_name="John", last_name="Doe"
    )

    result = await user_service.get_user("test-id")

    assert result is not None
    assert result.id == "test-id"


@pytest.mark.asyncio
async def test_get_user_not_found(user_service, mock_repository):
    mock_repository.find_by_id.return_value = None

    result = await user_service.get_user("nonexistent-id")

    assert result is None


@pytest.mark.asyncio
async def test_delete_user_publishes_event(user_service, mock_repository, mock_producer):
    mock_repository.delete.return_value = True

    result = await user_service.delete_user("test-id")

    assert result is True
    mock_producer.publish.assert_called_once_with(
        topic="user-events",
        key="test-id",
        value={"event": "USER_DELETED", "userId": "test-id"},
    )
