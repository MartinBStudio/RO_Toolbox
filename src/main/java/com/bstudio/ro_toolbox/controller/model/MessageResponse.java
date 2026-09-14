package com.bstudio.ro_toolbox.controller.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageResponse {
  private String message;
}
