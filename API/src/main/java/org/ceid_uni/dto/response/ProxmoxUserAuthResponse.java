package org.ceid_uni.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProxmoxUserAuthResponse {

    @JsonProperty("version")
    private String version;

    @JsonProperty("repoid")
    private String repoid;

    @JsonProperty("release")
    private String release;

    public ProxmoxUserAuthResponse() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRepoid() {
        return repoid;
    }

    public void setRepoid(String repoid) {
        this.repoid = repoid;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

}
