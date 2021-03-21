
# Kafka Streams Dashboards

* Showcases the monitoring of Kafka Streams Metrics

* This is the code and dashboards as the basis of a Kafka Summit Europe 2021 presentation titled,
[What is the State of my Kafka Streams Application? Unleashing Metrics.](https://www.kafka-summit.org/sessions/what-is-the-state-of-my-kafka-streams-application-unleashing-metrics).

## TL;TR

* Setup and Configuration all in the `./scripts/startup.sh` script; execute from root directory to get everything running.

* Shut it all down, use `./scripts/teardown.sh` script.

* Grafana Dashboard 

  * `https://localhost:3000`
  * Credentials:
    * username: `admin`
    * password: `grafana`

  * on MacOS your the grafana dashboard will auto open in your default browser.

* Leverages Docker and Docker Container extensively

## Examples

### Kafka Streams Threads Dashboard
![Kafka Streams Threads](./doc/streams_thread_dashboard.png)

## Docker 

* This project leverages docker and docker compose for easy of demonstration.

* to minimize having to start up all components, separate `docker-compose.yml` for each logical-unit and a common bridge network `ksd`.

* docker compose .env files used to keep container names short and consistent but hopefully not clash with any existing docker containers you are using.

## OpenSource libraries

* The primary software libraries used in addition to Apache Kafka Client and Streams Libraries.

  * FasterXML Jackson

  * Lombok

  * JCommander

  * Logback

  * Apache Commons
