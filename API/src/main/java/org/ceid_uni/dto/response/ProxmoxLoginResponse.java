package org.ceid_uni.dto.response;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public class ProxmoxLoginResponse {

    public ProxmoxLoginResponse() {
    }

    public ProxmoxLoginResponse(String csrfToken, String ticket, String username, String clustername, List<String> roles) {
        this.csrfToken = csrfToken;
        this.ticket = ticket;
        this.username = username;
        this.clustername = clustername;
        this.roles = roles;
    }

    @JsonProperty("CSRFPreventionToken")
    private String csrfToken;

    @JsonProperty("ticket")
    private String ticket;

    @JsonProperty("username")
    private String username;

    @JsonProperty("clustername")
    private String clustername;

    @JsonProperty("cap")
    @JsonIgnore
    private String cap;
    private List<String> roles;

    public String getCsrfToken() {
        return csrfToken;
    }

    public String getTicket() {
        return ticket;
    }

    public String getUsername() {
        return username;
    }

    public String getClustername() {
        return clustername;
    }

    public String getCap() {
        return cap;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

}
