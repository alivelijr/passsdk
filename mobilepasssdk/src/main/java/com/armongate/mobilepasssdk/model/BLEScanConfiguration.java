package com.armongate.mobilepasssdk.model;

import java.util.ArrayList;
import java.util.List;

public class BLEScanConfiguration {

    public List<String> uuidFilter;
    public String dataUserId;
    public String dataAccessPointId;
    public int dataDirection;

    public BLEScanConfiguration(List<String> uuidFilter, String userId, String accessPointId, int direction) {
        this.uuidFilter         = uuidFilter;
        this.dataUserId         = userId;
        this.dataAccessPointId  = accessPointId;
        this.dataDirection      = direction;
    }

}
