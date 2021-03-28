
URL=http://localhost:18081
#URL=http://druid-coordinator:8081

CONTENT_TYPE_HDR='Content-Type:application/json'

DATA=config.json

curl -X POST -H $CONTENT_TYPE_HDR -d @$DATA $URL/druid/indexer/v1/supervisor
curl -X POST -H 'Content-Type:application/json' -d @$DATA http://localhost:18081/druid/indexer/v1/supervisor
#curl -X POST http://localhost:18081/druid/indexer/v1/supervisor/_metrics-kafka-streams/shutdown


#TASK=task
#curl -X POST -H $CONTENT_TYPE_HDR -d @$DATA $URL/druid/indexer/v1/$TASK

