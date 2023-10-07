package demo.kafka.controller;

import demo.kafka.rest.api.TriggerEventsRequest;
import demo.kafka.service.DemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/demo")
public class DemoController {

    @Autowired
    private final DemoService demoService;

    /**
     * If request param 'async' is present and true, then returns a 202 ACCEPTED immediately, as the send happens asynchronously.
     *
     * If request param 'async' is not present or false, then returns a 200 SUCCESS once send is complete.
     */
    @PostMapping("/trigger")
    public ResponseEntity<String> trigger(
            @RequestBody TriggerEventsRequest request,
            @RequestParam(value = "async", required = false, defaultValue = "false") Boolean async) {
        if(request.getNumberOfEvents() == null || request.getNumberOfEvents()<1) {
            log.error("Invalid number of events");
            return ResponseEntity.badRequest().build();
        }
        try {
            if(async) {
                demoService.triggerAsync(request);
                return ResponseEntity.accepted().build();
            } else {
                demoService.triggerSync(request);
                return ResponseEntity.ok().build();
            }
        } catch(Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
