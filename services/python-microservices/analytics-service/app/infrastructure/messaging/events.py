from enum import Enum


class AnalyticsMetric(str, Enum):
    TOTAL_ORDERS = "total_orders"
    TOTAL_REVENUE = "total_revenue"
    TOTAL_USERS = "total_users"
    CONVERSION_RATE = "conversion_rate"
