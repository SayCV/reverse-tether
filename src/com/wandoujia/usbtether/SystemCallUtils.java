package com.wandoujia.usbtether;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;

import android.net.ConnectivityManager;
import android.util.Log;

import com.stericson.RootTools.Command;
import com.stericson.RootTools.CommandCapture;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.Shell;

public class SystemCallUtils {
    private static AtomicInteger index = new AtomicInteger();

    public static int updateIp(Shell shell, String dev, String ip)
            throws IOException, InterruptedException {

        Command command = new CommandCapture(index.incrementAndGet(),
                "ifconfig " + dev + " " + ip + " netmask 255.255.255.0");
        shell.add(command).waitForFinish();
        return command.exitCode();

    }

    public static String tether(final ConnectivityManager cm,
            String[] tetherableIfaces) {
        for (String dev: tetherableIfaces) {
            // let's tether
            try {
                int ret = (Integer) ConnectivityManager.class.getMethod(
                        "tether", String.class).invoke(cm, dev);

                // 0 -> TETHER_ERROR_NO_ERROR
                if (ret != 0) {
                    Log.w(Const.TAG, "tether " + dev + " error: " + ret);
                    return null;
                }

                return dev;
            } catch (Exception e) {
                Log.w(Const.TAG, e);
            }
        }
        return null;
    }

    public static int setUsbTethering(final ConnectivityManager cm,
            boolean enable) {
        try {
            int ret = (Integer) ConnectivityManager.class.getMethod(
                    "setUsbTethering", boolean.class).invoke(cm, true);

            // 0 -> TETHER_ERROR_NO_ERROR
            if (ret != 0) {
                Log.w(Const.TAG, "tether usb error: " + ret);

            }

            return ret;
        } catch (Exception e) {
            Log.w(Const.TAG, e);
            return -1;
        }
    }

    public static int untether(final ConnectivityManager cm,
            String tetheredIface) {
        try {
            int ret = (Integer) ConnectivityManager.class.getMethod("untether",
                    String.class).invoke(cm, tetheredIface);

            // 0 -> TETHER_ERROR_NO_ERROR
            if (ret != 0) {
                Log.w(Const.TAG, "untether " + tetheredIface + " error: " + ret);

            }

            return ret;
        } catch (Exception e) {
            Log.w(Const.TAG, e);
            return -1;
        }

    }

    public static int deleteDefaultRoute(Shell shell)
            throws InterruptedException, IOException {
        Command command = new CommandCapture(index.incrementAndGet(),
                "ip route del default");

        shell.add(command).waitForFinish();
        return command.exitCode();
    }

    public static int addDefaultRoute(Shell shell, String dev, String gateway)
            throws IOException, InterruptedException {

        Command command = new CommandCapture(index.incrementAndGet(),
                "ip route add default via " + gateway + " dev " + dev);

        shell.add(command).waitForFinish();

        return command.exitCode();
    }

    public static int pingGateway(Shell shell, String gateway)
            throws InterruptedException, IOException {
        Command command = new CommandCapture(index.incrementAndGet(), "ping "
                + gateway);

        shell.add(command).waitForFinish(500);
        return command.exitCode();
    }

    public static Shell getRootShell() {
        try {
            return RootTools.getShell(true);
        } catch (Exception e) {
            Log.w(Const.TAG, e);

            return null;
        }
    }

    public static String[] getTetherableIfaces(final ConnectivityManager cm) {
        // try {
        // String[] tetherableIfaces = (String[]) ConnectivityManager.class
        // .getMethod("getTetherableIfaces").invoke(cm);
        //
        // Log.i(Const.TAG,
        // "tetherableIfaces "
        // + StringUtils.join(tetherableIfaces, ','));
        // logs.append("\n" + "tetherableIfaces "
        // + StringUtils.join(tetherableIfaces, ','));
        //
        // if (tetherableIfaces.length != 0) {
        // return tetherableIfaces;
        // }
        // return tetherableIfaces;
        // } catch (Exception e) {
        // Log.w(Const.TAG, "exception caught", e);
        // }
        return new String[] {
            "rndis0"
        };
    }

    public static String[] getUsbTetherableIfaces(final ConnectivityManager cm) {
        // try {
        // String[] tetherableIfaces = (String[]) ConnectivityManager.class
        // .getMethod("getTetherableIfaces").invoke(cm);
        //
        // Log.i(Const.TAG,
        // "tetherableIfaces "
        // + StringUtils.join(tetherableIfaces, ','));
        // logs.append("\n" + "tetherableIfaces "
        // + StringUtils.join(tetherableIfaces, ','));
        //
        // if (tetherableIfaces.length != 0) {
        // return tetherableIfaces;
        // }
        // return tetherableIfaces;
        // } catch (Exception e) {
        // Log.w(Const.TAG, "exception caught", e);
        // }
        return new String[] {
            "rndis0"
        };
    }

    public static String[] getTetheredIfaces(final ConnectivityManager cm) {
        try {
            String[] tetheredIfaces = (String[]) ConnectivityManager.class
                    .getMethod("getTetheredIfaces").invoke(cm);

            Log.i(Const.TAG,
                    "tetheredIfaces " + StringUtils.join(tetheredIfaces, ','));

            return tetheredIfaces;

        } catch (Exception e) {
            Log.w(Const.TAG, "exception caught", e);
            return new String[0];
        }
    }

    public static boolean isTetheringSupported(final ConnectivityManager cm) {
        try {
            boolean isTetheringSupported = (Boolean) ConnectivityManager.class
                    .getMethod("isTetheringSupported").invoke(cm);

            Log.i(Const.TAG, "isTetheringSupported " + isTetheringSupported);

            return true;
        } catch (Exception e) {
            Log.w(Const.TAG, "exception caught", e);
            return false;
        }
    }

    public static void setMobileDataEnabled(ConnectivityManager cm,
            boolean enable) {
        try {
            ConnectivityManager.class.getMethod("setMobileDataEnabled",
                    boolean.class).invoke(cm, enable);

        } catch (Exception e) {
            Log.w(Const.TAG, "exception caught", e);

        }

    }

}
