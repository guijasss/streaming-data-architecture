docker exec -it kafka kafka-topics --create --topic user-events --bootstrap-server kafka:29092 
docker exec -it kafka kafka-topics --create --topic user-events-flatten --bootstrap-server kafka:29092