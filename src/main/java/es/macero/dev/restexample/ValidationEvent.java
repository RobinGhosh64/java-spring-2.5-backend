package es.macero.dev.restexample;


import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;


@Data
public class ValidationEvent {

    @JsonProperty("validationCode")
    private String validationCode;

    @JsonProperty("validationUrl")
    private String validationUrl;
    

}
