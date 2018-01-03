package org.chmoder.atwdp.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tcross on 6/29/2017.
 */

public class ChmoderUser {
    public String id;
    public List<String> deviceIds;

    public ChmoderUser() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public ChmoderUser(String userId) {
        this.id = userId;
        this.deviceIds = new ArrayList<>();
    }

    public ChmoderUser(String userId, String deviceId) {
        this.id = userId;
        if(this.deviceIds == null) {
            this.deviceIds = new ArrayList<>();
        }
        if(!this.deviceIds.contains(deviceId)) {
            this.deviceIds.add(deviceId);
        }
    }
}
