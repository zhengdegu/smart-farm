package com.smartfarm.telemetry.domain;

import lombok.*;
import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataId implements Serializable {
    private OffsetDateTime time;
    private String deviceId;
}
