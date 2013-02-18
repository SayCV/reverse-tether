package com.wandoujia.usbtether;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.Shell;

public class MainActivity extends Activity {
    private Handler handler = null;

    private final static int TETHER = 0;

    private Shell shell = null;

    private Button actionButton;

    private String gateway;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actionButton = (Button) findViewById(R.id.actionButton);

        if (!RootTools.isRootAvailable()) {
            new AlertDialog.Builder(this)
                    .setMessage(
                            "root is required, root first? z4root is excellent, try itz?")
                    .setCancelable(true).create().show();

            return;
        }

        if (!RootTools.checkUtil("ifconfig")) {
            new AlertDialog.Builder(this)
                    .setMessage("ifconfig is required, why you don't have it?")
                    .setCancelable(true).create().show();

            return;
        }

        if (!RootTools.checkUtil("ip")) {
            new AlertDialog.Builder(this)
                    .setMessage("ip is required, why you don't have it?")
                    .setCancelable(true).create().show();

            return;
        }

        final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        handler = new Handler(getMainLooper()) {

            private String tetheredDev;

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case TETHER:
                        // check environment

                        if (!SystemCallUtils.isTetheringSupported(cm)) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage("tether is not supported!")
                                    .setCancelable(true).create().show();

                            return;
                        }

                        for (String tetheredIface: SystemCallUtils
                                .getTetheredIfaces(cm)) {
                            int ret = SystemCallUtils.untether(cm,
                                    tetheredIface);
                            if (ret != 0) {
                                Log.w(Const.TAG, "fail to untether "
                                        + tetheredIface);
                            }
                        }

                        shell = SystemCallUtils.getRootShell();
                        if (shell == null) {
                            Toast.makeText(MainActivity.this,
                                    "fail to get root", Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }

                        // check if usb is plugged
                        boolean usbPlugged = isUsbPlugged();
                        if (!usbPlugged) {
                            Toast.makeText(MainActivity.this,
                                    "usb is not pluged, retry",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        tetheredDev = SystemCallUtils
                                .getUsbTetherableIfaces(cm)[0];

                        String s = SystemCallUtils.tether(cm, new String[] {
                            tetheredDev
                        });
                        if (s == null) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage(
                                            "tether " + tetheredDev + " fail!")
                                    .setCancelable(true).create().show();
                            return;
                        }

                        gateway = "192.168.137.1";

                        try {
                            int ret = SystemCallUtils.pingGateway(shell,
                                    gateway);
                            if (ret == 0) {
                                if (reroute(cm, tetheredDev, gateway) != 0) {
                                    SystemCallUtils.setMobileDataEnabled(cm,
                                            false);
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setMessage(
                                                    "reverse tether fail, sorry")
                                            .setCancelable(true).create()
                                            .show();
                                }
                            } else {
                                Log.w(Const.TAG, "ping " + gateway + " return "
                                        + ret);
                                shell = SystemCallUtils.getRootShell();

                                final EditText ipView = new EditText(
                                        MainActivity.this);
                                ipView.setText(gateway);

                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle(
                                                "please confirm your new connnection ip")
                                        .setView(ipView)
                                        .setPositiveButton(
                                                "DONE",
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(
                                                            DialogInterface dialog,
                                                            int which) {
                                                        gateway = ipView
                                                                .getText()
                                                                .toString();
                                                        if (!checkGateway(gateway)) {
                                                            new AlertDialog.Builder(
                                                                    MainActivity.this)
                                                                    .setMessage(
                                                                            "wrong gateway!")
                                                                    .setCancelable(
                                                                            false)
                                                                    .create()
                                                                    .show();
                                                            return;
                                                        }

                                                        if (reroute(cm,
                                                                tetheredDev,
                                                                gateway) != 0) {
                                                            SystemCallUtils
                                                                    .setMobileDataEnabled(
                                                                            cm,
                                                                            false);
                                                            new AlertDialog.Builder(
                                                                    MainActivity.this)
                                                                    .setMessage(
                                                                            "reverse tether fail, sorry")
                                                                    .setCancelable(
                                                                            false)
                                                                    .create()
                                                                    .show();
                                                        }
                                                    }

                                                }

                                        ).show();

                            }
                        } catch (Exception e) {
                            Log.w(Const.TAG, e);
                        }

                }
            }

        };

        actionButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                handler.sendEmptyMessage(TETHER);
            }
        });

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    private boolean isUsbPlugged() {
        Intent intent = MainActivity.this.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkGateway(String gateway) {
        String[] tokens = gateway.split("\\.");

        if (tokens.length != 4 || !tokens[0].equalsIgnoreCase("192")
                || !tokens[1].equalsIgnoreCase("168")) {

            return false;
        }

        int third = -1;
        int fourth = -1;
        try {
            third = Integer.valueOf(tokens[2]);
            fourth = Integer.valueOf(tokens[3]);
        } catch (NumberFormatException e) {}
        if (third < 0 || third > 255) {

            return false;
        }

        if (fourth < 0 || fourth > 255) {

            return false;
        }
        return true;
    }

    private String generateLocalIp(String gateway) {

        String[] tokens = gateway.split("\\.");

        int third = Integer.valueOf(tokens[2]);
        int fourth = Integer.valueOf(tokens[3]);

        String localIp = "192.168." + third + "."
                + ((fourth + 1) > 255 ? 1 : (fourth + 1));
        return localIp;
    }

    private int reroute(ConnectivityManager cm, String tetheredDev,
            String gateway) {
        try {
            SystemCallUtils.setMobileDataEnabled(cm, true);

            String localIp = generateLocalIp(gateway);

            int exitCode = 0;

            exitCode = SystemCallUtils.updateIp(shell, tetheredDev, localIp);
            Log.i(Const.TAG, "updateIp " + tetheredDev + " " + localIp
                    + " return " + exitCode);

            if (exitCode != 0)
                return exitCode;

            exitCode = SystemCallUtils.deleteDefaultRoute(shell);

            Log.i(Const.TAG, "deleteDefaultRoute return " + exitCode);

            if (exitCode != 0) {
                Log.i(Const.TAG, "deleteDefaultRoute " + tetheredDev + " "
                        + localIp + " return " + exitCode);
            }

            exitCode = SystemCallUtils.addDefaultRoute(shell, tetheredDev,
                    gateway);

            Log.i(Const.TAG, "addDefaultRoute " + gateway + " return "
                    + exitCode);

            return exitCode;

        } catch (Exception e) {
            Log.w(Const.TAG, e);
            return -1;
        }
    }

}
