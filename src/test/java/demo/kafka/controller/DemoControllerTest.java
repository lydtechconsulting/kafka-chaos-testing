package demo.kafka.controller;

import demo.kafka.rest.api.TriggerEventsRequest;
import demo.kafka.service.DemoService;
import demo.kafka.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DemoControllerTest {

    private DemoService serviceMock;
    private DemoController controller;

    @BeforeEach
    public void setUp() {
        serviceMock = mock(DemoService.class);
        controller = new DemoController(serviceMock);
    }

    /**
     * Ensure that the REST request is successfully passed on to the service.
     */
    @Test
    public void testTrigger_Success() {
        TriggerEventsRequest request = TestData.buildTriggerEventsRequest(10);
        ResponseEntity response = controller.trigger(request);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.ACCEPTED));
        verify(serviceMock, times(1)).process(request);
    }

    /**
     * If an exception is thrown, an internal server error is returned.
     */
    @Test
    public void testTrigger_ServiceThrowsException() {
        TriggerEventsRequest request = TestData.buildTriggerEventsRequest(10);
        doThrow(new RuntimeException("Service failure")).when(serviceMock).process(request);
        ResponseEntity response = controller.trigger(request);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
        verify(serviceMock, times(1)).process(request);
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL, 400",
                        "10, 202",
                        }, nullValues = "NULL")
    void testTrigger_Validation(Integer numberOfEvents, Integer expectedHttpStatusCode) {
        TriggerEventsRequest request = TriggerEventsRequest.builder()
                .numberOfEvents(numberOfEvents)
                .build();
        ResponseEntity response = controller.trigger(request);
        assertThat(response.getStatusCode().value(), equalTo(expectedHttpStatusCode));
    }
}
