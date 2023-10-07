# Kafka Spring Boot Chaos Testing Demo Project

THIS IS WORK IN PROGRESS [04/07/2023]

Spring Boot application demonstrating chaos testing a Kafka application with Conduktor Gateway.

## Overview

The application provides a REST endpoint that accepts a request to trigger sending events.  The number of events to produce can be specified.

Conduktor Gateway sits as a proxy between the Spring Boot application and Kafka.  The Gateway can be configured to simulate broker failure scenarios, returning errors to the producer requests.  This is used to verify that the application behaves as expected under error scenarios.  See more on Conduktor Gateway at (https://www.conduktor.io/)[https://www.conduktor.io/]

Component tests are used to automate these error scenarios, using Lydtech's component-test-framework and Testcontainers.

## Run Spring Boot Application

### Build
```
mvn clean install
```

### Run Docker Containers

From the root dir run the `docker-compose` files to start dockerised Kafka, Zookeeper, and Conduktor Gateway:
```
docker-compose -f docker-compose.yml up -d
```

### Start Demo Spring Boot Application

To start the application use:
```
java -jar target/kafka-chaos-testing-1.0.0.jar
```

### Trigger Events

To trigger sending a configurable number of events send the following request to the application using curl:

```
curl -v -d '{"numberOfEvents":100}' -H "Content-Type: application/json" -X POST http://localhost:9001/v1/demo/trigger
```

The request should be accepted with a `202 ACCEPTED` response, as the event sending processing is asynchronous. 

### Consume Events

To view the outbound events emitted by the application, use the command line tool `kafka-console-consumer` to consume the events from the topic:
```
docker exec -it kafka  /bin/sh /usr/bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic demo-topic --from-beginning
```
Pressing `ctrl-c` stops the console consumer, and logs how many messages were consumed.

## Component Tests

### Overview

The tests call the dockerised demo application over REST to trigger sending and consuming a configurable number of events.  The impact of the configured compression type on messages stored on the Kafka broker can then be viewed using the command line tools or Conduktor. 

Conduktor Gateway is enabled for the component test run in the `pom.xml`:

```
<conduktor.gateway.enabled>true</conduktor.gateway.enabled>
```

For more on the component tests see: https://github.com/lydtechconsulting/component-test-framework

### Test

The component tests used to trigger sending events is `demo.kafka.component.BrokerFailureCT`.  Edit the request to determine the quantity of events to send.

Send a specified number of events: 
```
TriggerEventsRequest request = TriggerEventsRequest.builder()
    .numberOfEvents(NUMBER_OF_EVENTS)
    .build();
```

### Build

Build Spring Boot application jar:
```
mvn clean install
```

Build Docker container:
```
docker build -t ct/kafka-chaos-testing:latest .
```

### Test Execution

Run tests (by default the containers are torn down after the test run):
```
mvn test -Pcomponent
```

Run tests leaving the containers up at the end:
```
mvn test -Pcomponent -Dcontainers.stayup=true
```

## Docker Clean Up

Manual clean up (if left containers up):
```
docker rm -f $(docker ps -aq)
```

Further docker clean up (if network issues and to remove old networks/volumes):
```
docker network prune
docker system prune
docker volume prune
```
