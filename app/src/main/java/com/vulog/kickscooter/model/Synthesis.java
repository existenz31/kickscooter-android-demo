package com.vulog.kickscooter.model;

import lombok.Data;

@Data
public class Synthesis {
    String id;
    Boolean modulePowerOn;
    Boolean moduleSleep;
    Boolean vehiclePowerOn;
    Boolean vehicleStolen;
    Boolean moduleAttached;
    Integer auxiliaryLightColorOne;
    Integer auxiliaryLightColorTwo;
    Integer vehicleMaxSpeed;
    Boolean auxiliaryLightOn;
    Integer auxiliaryLightMode;
    Boolean bluetoothActive;
    Boolean cellularConnection;
    Boolean vehicleOnGround;
    Integer vehicleBatteryPercentage;
    Integer moduleBatteryPercentage;
    Integer speedKph;
    Integer odometerKm;
    Double gpsLatitude;
    Double gpsLongitude;
    Long lastUpdate;
    Boolean vehicleCharging;

}
