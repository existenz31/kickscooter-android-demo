package com.vulog.kickscooter.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BoxMessage {
    String boxId;
    String type;
    String name;
    String token;
    BoxData data;

}
