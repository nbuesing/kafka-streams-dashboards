
URL=http://localhost:18081
#URL=http://druid-coordinator:8081


curl -X POST $URL/druid/indexer/v1/supervisor/_metrics-kafka-streams/shutdown
