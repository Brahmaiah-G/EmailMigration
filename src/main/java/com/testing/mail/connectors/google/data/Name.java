package com.testing.mail.connectors.google.data;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Name {

    @JsonProperty("givenName")
    private String givenName;
    @JsonProperty("familyName")
    private String familyName;
    @JsonProperty("fullName")
    private String fullName;
    @JsonProperty("displayName")
    private String displayName;
    @JsonProperty("displayNameLastFirst")
    private String displayNameLastFirst;
    @JsonProperty("unstructuredName")
    private String unstructuredName;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

   
}
