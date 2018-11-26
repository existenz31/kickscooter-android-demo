package com.vulog.kickscooter.model;

import java.util.Date;

import lombok.Data;

@Data
public class IoT {
    String id;
    Double gpsLatitude;
    Double gpsLongitude;
    Boolean vehiclePowerOn;
    Integer vehicleBatteryPercentage;
    Integer moduleBatteryPercentage;
    Long lastUpdate;

    Date getLastUpdateDate() {
        return new Date(lastUpdate*1000);
    }

}
