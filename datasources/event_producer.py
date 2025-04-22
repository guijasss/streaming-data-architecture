from datetime import datetime
from typing import Optional
from json import dumps
from uuid import uuid4
from random import choice, random
from argparse import ArgumentParser

from kafka_handler import send_to_kafka


if __name__ == '__main__':
    parser = ArgumentParser(prog="user-events-producer", description="Sends user events to Kafka.")
    parser.add_argument("--log", action='store_true', default=None, help="Print events, instead of sending to Kafka")
    parser.add_argument("--pattern", default="RANDOM", help="Generate events with predefined drop-off", choices=["ABANDON_CHECKOUT", "FINISH_FLOW", "RANDOM"])
    parser.add_argument("--batch-size", default=1, help="Specify how much events to generate", type=int)

    args = parser.parse_args()

    print_log: bool = args.log is not None
    pattern: str = args.pattern
    batch_size: int = args.batch_size

    CAMPAIGN_ID = "summer_sale_2025"
    PRODUCTS = ["8f3bd0fe-c32b-44ad-b29c-100342eab215"]
    AD_PLATFORMS = ["facebook", "google_ads", "instagram"]
    DEVICES = ["mobile", "desktop"]
    PAYMENT_METHODS = ["credit_card", "paypal", "apple_pay"]

    drop_off_patterns = {
        "ABANDON_CHECKOUT": [0, 0, 0, 0, 1],
        "FINISH_FLOW": [0, 0, 0, 0, 0],
        "RANDOM": [0.3, 0.4, 0.3, 0.4, 0.6]
    }

    DROP_OFF_PROBABILITY = drop_off_patterns.get(pattern)  # Probability of dropping at each step

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
        if random() < DROP_OFF_PROBABILITY[0]: return events

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
        
        events.append(generate_event("add_to_cart", user_id, {
            "tracking_id": tracking_id,
            "product_id": PRODUCTS[0]
        }))

        if random() < DROP_OFF_PROBABILITY[3]: return events

        # Step 5: Checkout Start
        events.append(generate_event("checkout_start", user_id, {
            "tracking_id": tracking_id,
            "cart_items": PRODUCTS,
            "email": "fake@email.com"
        }))
        if random() < DROP_OFF_PROBABILITY[4]: return events

        # Step 6: Purchase
        events.append(generate_event("purchase", user_id, {
            "tracking_id": tracking_id,
            "order_id": str(uuid4()),
            "payment_method": choice(PAYMENT_METHODS),
        }))

        return events
    
    def send_event() -> int:
        """
        returns: length of the simulated journey (number of events)
        """
        user_id = 1
        journey = generate_user_journey(user_id)
        if len(journey) > 1:
            for event in journey:
                if print_log:
                    print(dumps(event, indent=2))
                else:
                    send_to_kafka(event)
        return len(journey)
    
    num_events = 0
    for i in range(0, batch_size):
        num_events += send_event()
        
    print(f"{num_events} events were sent to Kafka!")
