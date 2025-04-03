docker exec -it kafka kafka-topics --create --topic user-events --bootstrap-server kafka:9092 --partitions 3 --replication-factor 1
