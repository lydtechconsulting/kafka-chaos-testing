# Kafka Spring Boot Chaos Testing Demo Project

Spring Boot application demonstrating chaos testing a Spring Boot application that integrates with Kafka as the messaging broker to send and receive events, using Conduktor Gateway to simulate failure scenarios with Kafka.

## Overview

Conduktor Gateway sits as a proxy between the Spring Boot application and Kafka.  The Gateway can be configured to simulate broker failure scenarios, returning errors to the producer requests.  This is used to verify that the application behaves as expected under error scenarios.  See https://www.conduktor.io/gateway/ for more on Conduktor Gateway.

The Spring Boot demo application provides a REST endpoint that accepts a request to trigger sending events.  The number of events to produce can be specified, and the behaviour of the application can then be observed different failure scenarios. 

Component tests are used to automate these failure scenarios and to verify that application is resilient in these conditions, using Lydtech's [component-test-framework](https://github.com/lydtechconsulting/component-test-framework) and [Testcontainers](https://testcontainers.com/).

## Run Spring Boot Application

### Build
```
mvn clean install
```

### Run Docker Containers

From the root dir run the `docker-compose` files to start dockerised Kafka, Zookeeper, and Conduktor Gateway:
```
docker-compose up -d
```

### Start Demo Spring Boot Application

To start the application use:
```
java -jar target/kafka-chaos-testing-1.0.0.jar
```

### Register Interceptor with Conduktor Gateway

Register an interceptor that simulates a broken broker:

```
curl \
--user "admin:conduktor" \
--request POST "localhost:8888/admin/interceptors/v1/vcluster/passthrough/interceptor/broken-broker" \
--header "Content-Type: application/json" \
--data-raw '{
    "pluginClass": "io.conduktor.gateway.interceptor.chaos.SimulateBrokenBrokersPlugin",
    "priority": 100,
    "config": {
        "rateInPercent": 20,
        "errorMap": {
            "PRODUCE": "NOT_ENOUGH_REPLICAS"
        }
    }
}'
```

List registered interceptors:

```
curl --user "admin:conduktor" localhost:8888/admin/interceptors/v1/vcluster/passthrough/interceptors
```

Remove the interceptor:

```
curl --user "admin:conduktor" --request DELETE "localhost:8888/admin/interceptors/v1/vcluster/passthrough/interceptor/broken-broker"
```

See the docs at https://www.conduktor.io/gateway/ for the full set of available chaos testing interceptors.

### Trigger Events

To trigger sending a configurable number of events send the following request to the application using curl, with the call returning immediately and the events being sent asynchronously, send with the `async` query param set to `true`:

```
curl -v -d '{"numberOfEvents":200}' -H "Content-Type: application/json" -X POST http://localhost:9001/v1/demo/trigger?async=true
```

The request should be accepted with a `202 ACCEPTED` response. 

To trigger sending the events synchronously so that the call only returns once the sending is complete, send with the `async` query param set to `false`.  The request should return successfully with a `200` response.

The producer retry behaviour can be configured in the `src/main/resources/application.yml`.  By setting producer `retries` to 0 for example will mean that the producer no longer retries when it receives a retryable exception from its request to Kafka.

### Consume Events

To view the outbound events emitted by the application, use the command line tool `kafka-console-consumer` to consume the events from the topic:
```
docker exec -it kafka  /bin/sh /usr/bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic demo-outbound-topic --from-beginning
```
Pressing `ctrl-c` stops the console consumer, and logs how many messages were consumed.

## Component Tests

### Overview

The tests call the dockerised demo application over REST to trigger sending and consuming a configurable number of events.  With Conduktor Gateway acting as a proxy between the application and Kafka, the requests to the broker can be intercepted and errors returned. 

Conduktor Gateway is enabled for the component test run in the `pom.xml`:

```
<conduktor.gateway.enabled>true</conduktor.gateway.enabled>
```

For more on the component tests see: https://github.com/lydtechconsulting/component-test-framework

### Tests

The component tests for the application are implemented in `demo.kafka.component.ResilienceCT`.

The tests register different interceptors with Conduktor Gateway in order to simulate different failure scenarios.

The application exposes a REST endpoint that the component tests call to trigger sending events.

The tests then assert that the application behaves as expected in these error scenarios.  That may be for the producer to retry until success, or fail and return the error to the caller.

The producer retry behaviour (as described above) can be overridden just for the component tests in `src/test/resources/application-component-test.yml`.  This means the application does not need to be rebuilt between test runs, although the docker container for the application does need to be rebuilt and restarted.

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
