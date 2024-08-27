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
     * Ensure that the REST request is successfully passed on to the service, returning once complete
     */
    @Test
    public void testTrigger_Sync_Success() throws Exception {
        TriggerEventsRequest request = TestData.buildTriggerEventsRequest(10);
        ResponseEntity response = controller.trigger(request, false);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        verify(serviceMock, times(1)).triggerSync(request);
    }

    /**
     * Ensure that the REST request is successfully passed on to the service, returning immediately.
     */
    @Test
    public void testTrigger_Async_Success() throws Exception {
        TriggerEventsRequest request = TestData.buildTriggerEventsRequest(10);
        ResponseEntity response = controller.trigger(request, true);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.ACCEPTED));
        verify(serviceMock, times(1)).triggerAsync(request);
    }

    /**
     * If an exception is thrown, an internal server error is returned.
     */
    @Test
    public void testTrigger_Sync_ServiceThrowsException() throws Exception{
        TriggerEventsRequest request = TestData.buildTriggerEventsRequest(10);
        doThrow(new Exception("Service failure")).when(serviceMock).triggerSync(request);
        ResponseEntity response = controller.trigger(request, false);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(response.getBody(), equalTo("Service failure"));
        verify(serviceMock, times(1)).triggerSync(request);
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL, 400",
                        "10, 202",
                        }, nullValues = "NULL")
    void testTrigger_Validation(Integer numberOfEvents, Integer expectedHttpStatusCode) {
        TriggerEventsRequest request = TriggerEventsRequest.builder()
                .numberOfEvents(numberOfEvents)
                .build();
        ResponseEntity response = controller.trigger(request, true);
        assertThat(response.getStatusCode().value(), equalTo(expectedHttpStatusCode));
    }
}
