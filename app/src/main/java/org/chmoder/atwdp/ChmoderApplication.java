package org.chmoder.atwdp;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.annotations.RealmModule;

/**
 * Created by tcross on 6/27/2017.
 */

@RealmModule(library = true, allClasses = true)
public class ChmoderApplication extends Application {
    private final static String TAG = ChmoderApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
//        deleteRealm();
    }

    public static RealmConfiguration getRealmConfig() {
        RealmConfiguration config = new RealmConfiguration.Builder()
                .name(TAG)
                .modules(new ChmoderApplication())
//                .encryptionKey(getKey())
//                .schemaVersion(1)
//                .modules(new MySchemaModule())
//                .migration(new MyMigration())
                .deleteRealmIfMigrationNeeded()
                .build();
        return config;
    }

    public void deleteRealm() {
        RealmConfiguration config = getRealmConfig();
        Realm.deleteRealm(config);
    }

    public static Realm getRealmInstance() {
        RealmConfiguration config = getRealmConfig();
        return Realm.getInstance(config);
    }
}
