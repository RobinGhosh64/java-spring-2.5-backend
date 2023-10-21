package es.macero.dev.restexample;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;

@RestController
@RequestMapping("/spanish-greetings")
public class SpanishGreetingController {

    private List<SpanishGreeting> spanishGreetings;

    public SpanishGreetingController() {
        spanishGreetings = new ArrayList<>();
        spanishGreetings.add(new SpanishGreeting("Hola!"));
        spanishGreetings.add(new SpanishGreeting("Qué tal?!"));
        spanishGreetings.add(new SpanishGreeting("Buenas!"));
    }

    @GetMapping("/{id}")
    public SpanishGreeting getSpanishGreetingById(@PathVariable("id") final int id) {
        return spanishGreetings.get(id - 1); // list index starts with 0 but we prefer to start on 1
    }

    @GetMapping("/random")
    public SpanishGreeting getRandom() {
        return spanishGreetings.get(new Random().nextInt(spanishGreetings.size()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void createSpanishGreeting(@RequestBody SpanishGreeting spanishGreeting) {
        spanishGreetings.add(spanishGreeting);
    }

    @PostMapping("/token")
    @ResponseStatus(HttpStatus.OK)
    public String processEvent(@RequestBody EventGridEvent eventgridEvent) {
        return eventgridEvent.getSubject();
    }

    @PostMapping("/event")
    @ResponseStatus(HttpStatus.OK)
    public String processEvent(@RequestBody EventGridEvent eventgridEvent) {
        return eventgridEvent.getSubject();
    }

    
    @PostMapping("/send")
    @ResponseStatus(HttpStatus.OK)
    public String sendEvent(@RequestBody PartnerToken token) {
   
        EventGridPublisherClient<EventGridEvent> publisherClient = new EventGridPublisherClientBuilder()
            .endpoint(System.getenv("AZURE_EVENTGRID_EVENT_ENDPOINT"))  // make sure it accepts EventGridEvent
            .credential(new AzureKeyCredential(System.getenv("AZURE_EVENTGRID_EVENT_KEY")))
            .buildEventGridEventPublisherClient();

        String str = \"{"PartnerId":"robin","Token":"robin" }\";
        // Create a EventGridEvent with String data
        EventGridEvent eventJson = new EventGridEvent("com/example/MyApp", "DevStudio.Test", BinaryData.fromObject(str), "0.1");
        // Create a CloudEvent with Object data
        //EventGridEvent eventModelClass = new EventGridEvent("com/example/MyApp", "DevStudio.Test", BinaryData.fromObject(token), "0.1");
        // Send them to the event grid topic altogether.

        List<EventGridEvent> events = new ArrayList<>();
        events.add(eventJson);
        publisherClient.sendEvent(eventJson);
        return "Ok";
    }

    @PostMapping("/token/validate")
    @ResponseStatus(HttpStatus.OK) 
    public String VerifyToken(@RequestBody PartnerToken ptoken) {
        String verifytoken =ptoken.verifyJWTToken(ptoken.getToken(),ptoken.getPartnerId());
        return verifytoken;
    }


}
