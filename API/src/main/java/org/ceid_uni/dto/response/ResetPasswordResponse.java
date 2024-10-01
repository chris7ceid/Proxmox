package org.ceid_uni.dto.response;

public class ResetPasswordResponse {
  private String token;

  public ResetPasswordResponse(String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}
