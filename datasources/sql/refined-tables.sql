CREATE MATERIALIZED VIEW streaming.ad_clicks AS
    SELECT
        event_id,
        tracking_id,
        `timestamp`,
        event_data.campaign_id AS campaign_id,
        event_data.ad_platform AS ad_platform,
        event_data.landing_page AS landing_page
        FROM
        streaming.user_events
    WHERE
        event_type = 'ad_click'