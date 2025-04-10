from typing import TypedDict, List, Optional

class AdImpressionEvent(TypedDict):
    event_id: str
    timestamp: str
    event_type: str = "ad_impression"
    campaign_id: str
    ad_platform: str
    device: str

class AdClickEvent(TypedDict):
    event_id: str
    timestamp: str
    event_type: str = "ad_click"
    campaign_id: str
    ad_platform: str
    landing_page: str

class LandingPageViewEvent(TypedDict):
    event_id: str
    timestamp: str
    event_type: str = "landing_page_view"
    campaign_id: str
    landing_page: str
    referrer: str

class AddToCartEvent(TypedDict):
    event_id: str
    timestamp: str
    event_type: str = "add_to_cart"
    product_id: str

class CheckoutStartEvent(TypedDict):
    event_id: str
    timestamp: str
    event_type: str = "checkout_start"
    cart_items: List[str]
    email: Optional[str]

class PurchaseEvent(TypedDict):
    event_id: str
    timestamp: str
    event_type: str = "purchase"
    order_id: str
    payment_method: str
