package es.macero.dev.restexample;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.JsonElement;

import lombok.Data;
import org.springframework.boot.actuate.endpoint.web.Link;


@Data
public class Event {

    @JsonProperty("id")
    private String id;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("topic")
    private String topic;


    @JsonProperty("data")
    private ValidationEvent data;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("eventTime")
    private String eventTime;

    @JsonProperty("metadataVersion")
    private String metadataVersion;

    @JsonProperty("dataVersion")
    private String dataVersion;

    private LinkedHashMap<String,String> data1;

}
