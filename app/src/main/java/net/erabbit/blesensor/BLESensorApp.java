package net.erabbit.blesensor;

import android.app.Application;

/**
 * Created by Tom on 16/7/25.
 */
public class BLESensorApp extends Application {

    protected static BLESensorApp instance;

    public static BLESensorApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
