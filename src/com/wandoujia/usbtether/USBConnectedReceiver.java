package com.wandoujia.usbtether;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

public class USBConnectedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            int powerSource = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,
                    0);
            if (powerSource == BatteryManager.BATTERY_PLUGGED_USB) {

            }

        }
    }
}
