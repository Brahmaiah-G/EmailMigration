
package com.testing.mail.connectors.google.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"kind",
	"etag",
	"users",
	"nextPageToken"
})
public class MembersList {

	@JsonProperty("kind")
	private String kind;
	@JsonProperty("etag")
	private String etag;
	@JsonProperty("users")
	private List<User> users = null;
	@JsonProperty("users")
	private List<Member> members = null;
	@JsonProperty("nextPageToken")
	private String nextPageToken;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<>();

}
