package es.macero.dev.restexample;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;


@RestController
@Slf4j
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
    public String processToken(@RequestBody EventGridEvent eventgridEvent) {
        return eventgridEvent.getSubject();
    }


    @PostMapping("/event")
    @ResponseStatus(HttpStatus.OK)
    public String processEvent(@RequestBody List<LinkedHashMap> events) {
        //for right now let's pick up only the first item in the list
        try {
            Event event = new Event();
            LinkedHashMap obj = events.get(0);

            event.setId((String)obj.get("id"));
            event.setTopic((String)obj.get("topic"));
            event.setSubject((String)obj.get("subject"));
            event.setEventType((String)obj.get("eventType"));
            event.setEventTime((String) obj.get("eventTime"));
            if(event.getEventType().equals("Microsoft.EventGrid.SubscriptionValidationEvent")) {
                ValidationEvent ve = new ValidationEvent();
                LinkedHashMap<String,String> da = (LinkedHashMap) obj.get("data");
                ve.setValidationCode(da.get("validationCode"));
                ve.setValidationUrl(da.get("validationUrl"));
                event.setData(ve);
            } // robin.ghosh
            else{
                event.setData1((LinkedHashMap)obj.get("data"));
            }

          return parseAndProcessEvent(event);
        }catch (Exception e){
            System.out.println(e.toString());
        }
        log.info("Processing Azure Events..");
        return "";
    }


    private String parseAndProcessEvent(Event event) {

        log.info("Parse and Process Event..");
        Object data = event.getData()==null?event.getData1():event.getData();

        /* 
          Check if event type is SubscriptionValidation from Azure and also from our test bed
        */
        if (event.getEventType().equals("Microsoft.EventGrid.SubscriptionValidationEvent")) {

            log.info("Processing SubscriptionValidation..");
            /*
             * Check if this call is from Azure to validate our Webhook. In which case we need to return  {"validationResponse":"xxxxxx"}
             */
            if (data instanceof ValidationEvent) {
                ValidationEvent validationData = (ValidationEvent) data;
                log.info("ValidationData:" + validationData.getValidationCode());
                String jsonStr = validationData.getValidationCode();
                return "{\"validationResponse\" : \"" + jsonStr + "\"}";
            }

            /*
             * If we have came here, we are using our test bed and sending a custom EventGridEvent msg forcibly
             */

        }
        else {
            /* 
            *   This is where we will really process our payload for async processing
            */
            log.info("Our custom eventType=" + event.getEventType());
            log.info("Our custom payload:" + event.getData1().toString());
            /*
            * Process the payload please
            */
            if (data instanceof LinkedHashMap) {
                log.info("Forcing with a real Event Grid message..");
                log.info("Printing the payload.");
                return data.toString();
            }

        }
        return "OK. Will process your Event Asynchronously...";
    }


    @PostMapping("/test")
    @ResponseStatus(HttpStatus.OK)
    public String testEvent(@RequestBody Event event) {
        log.info("/test end point");
        if (event.getEventType().equals("Microsoft.EventGrid.SubscriptionValidationEvent")) {
            String jsonStr = event.getData().getValidationCode();
            return "{\"validationResponse\" : \"" + jsonStr + "\"}";
        }
        else return event.getData().toString();
    }


    
    @PostMapping("/forceeventgridevent")
    @ResponseStatus(HttpStatus.OK)
    public String sendEvent(@RequestBody List<Event> events) {
        Event event=events.get(0);
        log.info("Processing forceeventgridevent..");
        EventGridEvent eventMsg = new EventGridEvent("com/example/MyApp", event.getEventType(), BinaryData.fromObject(event.getData()), event.getDataVersion());
        // Send them to the event grid topic altogether.
        String str = "";//parseAndProcessEvent(eventMsg);
        log.info("Response : " + str);
        return str;
    }
    


    @PostMapping("/insert2eventgrid")
    @ResponseStatus(HttpStatus.OK)
    public String insert2EventGrid(@RequestBody List<Event> events) {
        Event event=events.get(0);
        try {

            String endpoint=System.getenv("AZURE_EVENTGRID_ENDPOINT");
            String sascredential=System.getenv("AZURE_EVENTGRID_KEY");

            log.debug("endpoint=" , endpoint);
            log.debug("sascredential=" , sascredential);
            
            // Get handle to Azure Event Grid publisher
            EventGridPublisherClient<EventGridEvent> publisherClient = new EventGridPublisherClientBuilder()
                .endpoint(endpoint)  // make sure it accepts EventGridEvent
                .credential(new AzureKeyCredential(sascredential))
                .buildEventGridEventPublisherClient();
            
            // Create custom message
            EventGridEvent eventMsg = new EventGridEvent("com/example/MyApp", event.getEventType(), BinaryData.fromObject(event.getData()), event.getDataVersion());
        
            // Insert into Azure Event Grid
            publisherClient.sendEvent(eventMsg);
        }
        catch (Exception e) {
            log.error("Azure Event Grid Exception :" + e.getMessage());
        }
        return "Ok";
    }


    @PostMapping("/token/validate")
    @ResponseStatus(HttpStatus.OK) 
    public String VerifyToken(@RequestBody PartnerToken ptoken) {
        String verifytoken =ptoken.verifyJWTToken(ptoken.getToken(),ptoken.getPartnerId());
        return verifytoken;
    }

}
