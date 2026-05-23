from typing import Optional
from decimal import Decimal
from app.domain.models import Payment, Refund
from app.infrastructure.persistence.database import Database


class PaymentRepository:
    def __init__(self, db: Database):
        self.db = db

    async def create(self, payment: Payment) -> Payment:
        query = """
            INSERT INTO payments (id, order_id, user_id, amount, currency, method, status)
            VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING *
        """
        row = await self.db.fetch_one(
            query, payment.id, payment.order_id, payment.user_id,
            payment.amount, payment.currency, payment.method, payment.status
        )
        return self._row_to_payment(row)

    async def find_by_id(self, payment_id: str) -> Optional[Payment]:
        row = await self.db.fetch_one("SELECT * FROM payments WHERE id = $1", payment_id)
        return self._row_to_payment(row) if row else None

    async def find_by_order(self, order_id: str) -> Optional[Payment]:
        row = await self.db.fetch_one("SELECT * FROM payments WHERE order_id = $1", order_id)
        return self._row_to_payment(row) if row else None

    async def update_status(self, payment_id: str, status: str,
                            transaction_id: Optional[str] = None) -> None:
        if transaction_id:
            await self.db.execute(
                "UPDATE payments SET status=$2, transaction_id=$3, updated_at=NOW() WHERE id=$1",
                payment_id, status, transaction_id,
            )
        else:
            await self.db.execute(
                "UPDATE payments SET status=$2, updated_at=NOW() WHERE id=$1",
                payment_id, status,
            )

    async def update_retry(self, payment_id: str, retry_count: int, failure_reason: str) -> None:
        await self.db.execute(
            "UPDATE payments SET retry_count=$2, failure_reason=$3, updated_at=NOW() WHERE id=$1",
            payment_id, retry_count, failure_reason,
        )

    async def create_refund(self, refund: Refund) -> Refund:
        query = """
            INSERT INTO refunds (id, payment_id, amount, reason, status)
            VALUES ($1, $2, $3, $4, $5) RETURNING *
        """
        row = await self.db.fetch_one(
            query, refund.id, refund.payment_id, refund.amount,
            refund.reason, refund.status
        )
        return Refund(
            id=str(row["id"]), payment_id=str(row["payment_id"]),
            amount=Decimal(str(row["amount"])), reason=row["reason"],
            status=row["status"], created_at=row["created_at"],
        )

    async def find_refunds_by_payment(self, payment_id: str) -> list[Refund]:
        rows = await self.db.fetch_all(
            "SELECT * FROM refunds WHERE payment_id = $1", payment_id
        )
        return [
            Refund(
                id=str(r["id"]), payment_id=str(r["payment_id"]),
                amount=Decimal(str(r["amount"])), reason=r["reason"],
                status=r["status"], created_at=r["created_at"],
            )
            for r in rows
        ]

    @staticmethod
    def _row_to_payment(row) -> Payment:
        return Payment(
            id=str(row["id"]), order_id=str(row["order_id"]),
            user_id=str(row["user_id"]), amount=Decimal(str(row["amount"])),
            currency=row["currency"], method=row["method"],
            status=row["status"], transaction_id=row["transaction_id"],
            failure_reason=row["failure_reason"], retry_count=row["retry_count"],
            created_at=row["created_at"], updated_at=row["updated_at"],
        )
