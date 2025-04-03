from datetime import datetime
from typing import Optional
from uuid import uuid4
from random import choice, random

from kafka_handler import send_to_kafka


CAMPAIGN_ID = "summer_sale_2025"
PRODUCTS = [{"product_id": "123", "price": 49.99, "currency": "USD"}]
AD_PLATFORMS = ["facebook", "google_ads", "instagram"]
DEVICES = ["mobile", "desktop"]
PAYMENT_METHODS = ["credit_card", "paypal", "apple_pay"]
DROP_OFF_PROBABILITY = [0.75, 0.3, 0.4, 0.5, 0.6]  # Probability of dropping at each step


def generate_event(event_type: str, user_id: int, extra_data: Optional[dict] = None):
    event = {
        "event_id": str(uuid4()),
        "timestamp": datetime.now().isoformat() + "Z",
        "user_id": user_id,
        "event_type": event_type,
    }
    if extra_data:
        event.update(extra_data)
    return event


def generate_user_journey(user_id: int):
    """Simulates a user going through the ad funnel with random drop-offs."""
    events = []
    tracking_id = str(uuid4())

    # Step 1: Ad Impression
    ad_platform = choice(AD_PLATFORMS)
    events.append(generate_event("ad_impression", user_id, {
        "tracking_id": tracking_id,
        "campaign_id": CAMPAIGN_ID,
        "ad_platform": ad_platform,
        "device": choice(DEVICES),
    }))
    if random() < DROP_OFF_PROBABILITY[0]: return events  # User drops off

    # Step 2: Ad Click
    events.append(generate_event("ad_click", user_id, {
        "tracking_id": tracking_id,
        "campaign_id": CAMPAIGN_ID,
        "ad_platform": ad_platform,
        "landing_page": "/welcome",
    }))
    if random() < DROP_OFF_PROBABILITY[1]: return events

    # Step 3: Landing Page View
    events.append(generate_event("landing_page_view", user_id, {
        "tracking_id": tracking_id,
        "campaign_id": CAMPAIGN_ID,
        "landing_page": "/welcome",
        "referrer": ad_platform,
    }))
    if random() < DROP_OFF_PROBABILITY[2]: return events

    # Step 4: Add to Cart
    product = choice(PRODUCTS)
    product.update({"tracking_id": tracking_id})

    events.append(generate_event("add_to_cart", user_id, product))
    if random() < DROP_OFF_PROBABILITY[3]: return events

    # Step 5: Checkout Start
    events.append(generate_event("checkout_start", user_id, {
        "tracking_id": tracking_id,
        "cart_items": [product],
        "total_amount": product["price"],
        "currency": product["currency"],
        "payment_options": PAYMENT_METHODS,
    }))
    if random() < DROP_OFF_PROBABILITY[4]: return events

    # Step 6: Purchase
    events.append(generate_event("purchase", user_id, {
        "tracking_id": tracking_id,
        "order_id": str(uuid4()),
        "total_amount": product["price"],
        "currency": product["currency"],
        "payment_method": choice(PAYMENT_METHODS),
    }))

    return events


import json

journey = generate_user_journey(1)
if len(journey) > 1:
    for event in journey:
        send_to_kafka(event)
        print(json.dumps(event, indent=2))
