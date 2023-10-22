package es.macero.dev.restexample;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class Event {

    @JsonProperty("id")
    private String id;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("data") 
    private String data;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("eventTime")
    private String eventTime;

    @JsonProperty("target")
    private String target;


}
