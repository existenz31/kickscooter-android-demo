package com.vulog.kickscooter.model;

import lombok.Data;

@Data
public class Vehicle {

    String id;
    String model;
    String name;
    String vulogId;
    String wsToken;
    Boolean booked;
    String bookedByName;
    String bookedByEmail;
    String bookedByUid;
    Boolean enabled;
}
