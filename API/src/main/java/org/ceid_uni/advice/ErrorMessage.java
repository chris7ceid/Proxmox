package org.ceid_uni.advice;

import java.util.Date;

public class ErrorMessage {
  private final int statusCode;
  private final String timestamp;
  private final String message;
  private final String description;

  public ErrorMessage(int statusCode, String timestamp, String message, String description) {
    this.statusCode = statusCode;
    this.timestamp = timestamp;
    this.message = message;
    this.description = description;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public String getMessage() {
    return message;
  }

  public String getDescription() {
    return description;
  }
}