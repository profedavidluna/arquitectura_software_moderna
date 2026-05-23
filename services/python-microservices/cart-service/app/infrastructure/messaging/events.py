from enum import Enum


class CartEvent(str, Enum):
    ITEM_ADDED = "ITEM_ADDED"
    ITEM_REMOVED = "ITEM_REMOVED"
    CART_CHECKED_OUT = "CART_CHECKED_OUT"
    COUPON_APPLIED = "COUPON_APPLIED"
