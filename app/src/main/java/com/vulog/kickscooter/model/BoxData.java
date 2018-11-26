package com.vulog.kickscooter.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoxData {
    String boxId;
    Boolean inCharge;
    Boolean doorLocked;
    Boolean doorClosed;
    Double longitude;
    Double latitude;
}
