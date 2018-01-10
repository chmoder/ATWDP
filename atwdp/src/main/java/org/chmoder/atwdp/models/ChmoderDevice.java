package org.chmoder.atwdp.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tcross on 6/26/2017.
 */

public class ChmoderDevice {
    public String id;
    public String customName;
    public Map<String, Object> schemas = new HashMap<>();


    public ChmoderDevice() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public ChmoderDevice(String deviceId) {
        this.id = deviceId;
    }

    public ChmoderDevice(String customName, String deviceId) {
        this.customName = customName;
        this.id = deviceId;
    }
}
