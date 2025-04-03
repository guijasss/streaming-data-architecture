from typing import TypedDict, List
from datetime import datetime

class AdImpressionEvent(TypedDict):
    event_id: str
    timestamp: str
    user_id: int
    event_type: str = "ad_impression"
    campaign_id: str
    ad_platform: str
    device: str

class AdClickEvent(TypedDict):
    event_id: str
    timestamp: str
    user_id: int
    event_type: str = "ad_click"
    campaign_id: str
    ad_platform: str
    landing_page: str  # URL path (e.g., "/product/123")

class LandingPageViewEvent(TypedDict):
    event_id: str
    timestamp: str
    user_id: int
    event_type: str = "landing_page_view"
    campaign_id: str
    landing_page: str
    referrer: str  # Where they came from (e.g., "facebook_ads")

class AddToCartEvent(TypedDict):
    event_id: str
    timestamp: str
    user_id: int
    event_type: str = "add_to_cart"
    product_id: str
    price: float
    currency: str

class CheckoutStartEvent(TypedDict):
    event_id: str
    timestamp: str
    user_id: int
    event_type: str = "checkout_start"
    cart_items: List[dict]
    total_amount: float
    currency: str
    payment_options: List[str]

class PurchaseEvent(TypedDict):
    event_id: str
    timestamp: str
    user_id: int
    event_type: str = "purchase"
    order_id: str
    total_amount: float
    currency: str
    payment_method: str  # E.g., "credit_card", "paypal"
