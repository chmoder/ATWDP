package org.chmoder.atwdp.models;


import org.chmoder.atwdp.ChmoderApplication;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by tcross on 6/27/2017.
 */

public class ChmoderProperties extends RealmObject {
    @PrimaryKey
    private String deviceId;
    private String uid;
    private String provisionedStatus;

    @Ignore
    public static final String DEVICE_UNPROVISIONED_STATUS = "unprovisioned";
    public static final String DEVICE_PROVISIONING_STATUS = "provisioning";
    public static final String DEVICE_PROVISIONED_STATUS = "provisioned";

    @Ignore
    public static final List<String> PROVISIONED_STATUSES = Arrays.asList(
            DEVICE_UNPROVISIONED_STATUS,
            DEVICE_PROVISIONING_STATUS,
            DEVICE_PROVISIONED_STATUS
    );

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getProvisionedStatus() {
        return provisionedStatus;
    }

    public void setProvisionedStatus(String provisionedStatus) {
        if(!PROVISIONED_STATUSES.contains(provisionedStatus)) {
            throw new InvalidParameterException("must be a valid PROVISIONED_STATUS");
        }
        this.provisionedStatus = provisionedStatus;
    }

    public static ChmoderProperties getProperties() {
        Realm realm = ChmoderApplication.getRealmInstance();
        ChmoderProperties first = realm.where(ChmoderProperties.class).findFirst();

        if(first != null) {
            first = realm.copyFromRealm(first);
        }
        realm.close();
        return first;
    }

    public static void setProperties(final String deviceId, final String uid, final String provisionedStatus) {
        Realm realm = ChmoderApplication.getRealmInstance();
        ChmoderProperties properties = getProperties();

        realm.beginTransaction();

        if(properties == null) {
            properties = new ChmoderProperties();
            properties.deviceId = UUID.randomUUID().toString();
        }

        if(deviceId != null && deviceId.length() > 0) {
            properties.setDeviceId(deviceId);
        }

        if(uid != null && uid.length() > 0) {
            properties.setUid(uid);
        }

        if(provisionedStatus != null) {
            properties.setProvisionedStatus(provisionedStatus);
        } else {
            properties.setProvisionedStatus(PROVISIONED_STATUSES.get(0));
        }

        realm.copyToRealmOrUpdate(properties);
        realm.commitTransaction();
        realm.close();
    }
}
