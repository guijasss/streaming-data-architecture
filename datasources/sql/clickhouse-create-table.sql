SET enable_json_type = 1;

CREATE DATABASE kafka;
CREATE DATABASE streaming;

CREATE TABLE IF NOT EXISTS kafka.stream_user_events
(
    user_id UInt64,
    event_id String,
    timestamp String,
    event_type LowCardinality(String),
    event_data JSON
)
ENGINE = Kafka
SETTINGS
    kafka_broker_list = 'kafka:29092',
    kafka_topic_list = 'user-events-flatten',
    kafka_group_name = 'test_consumer',
    kafka_format = 'JSONEachRow';

CREATE TABLE IF NOT EXISTS streaming.user_events (
    user_id UInt64,
    event_id String,
    timestamp String,
    event_type LowCardinality(String),
    event_data JSON
) ENGINE = MergeTree ORDER BY (`timestamp`, event_type);


CREATE MATERIALIZED VIEW streaming.user_events_mv TO streaming.user_events AS
SELECT *
FROM kafka.stream_user_events;

