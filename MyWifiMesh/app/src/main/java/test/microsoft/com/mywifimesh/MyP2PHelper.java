package test.microsoft.com.mywifimesh;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;

import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

/**
 * Created by juksilve on 16.2.2015.
 */
public class MyP2PHelper {

    static public byte[] ipIntToBytes(int ip) {
        byte[] b = new byte[4];
        b[0] = (byte) (ip & 0xFF);
        b[1] = (byte) ((ip >> 8) & 0xFF);
        b[2] = (byte) ((ip >> 16) & 0xFF);
        b[3] = (byte) ((ip >> 24) & 0xFF);
        return b;
    }

    static public String ipAddressToString(InetAddress ip) {
        return ip.getHostAddress().replaceFirst("%.*", "");
    }

    static public String deviceToString(WifiP2pDevice device) {
        return device.deviceName + " " + device.deviceAddress;
    }

    static public boolean isValidIpAddress(String ip) {
        boolean v4 = InetAddressUtils.isIPv4Address(ip);
        boolean v6 = InetAddressUtils.isIPv6Address(ip);
        if(!v4 && !v6) return false;
        try {
            InetAddress inet = InetAddress.getByName(ip);
            return inet.isLinkLocalAddress() || inet.isSiteLocalAddress();
        } catch(UnknownHostException e) {
            //Log.e(TAG, e.toString());
            return false;
        }
    }

    static public void printLocalIpAddresses(Context context) {
        List<NetworkInterface> ifaces;
        try {
            ifaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch(SocketException e) {
            showDialogBox("Got error: " + e.toString(), context);
            return;
        }
        String stuff = "Local IP addresses: \n";
        for(NetworkInterface iface : ifaces) {
            for(InetAddress addr : Collections.list(iface.getInetAddresses())) {
                String desc = MyP2PHelper.ipAddressToString(addr);
                if(addr.isLoopbackAddress()) desc += " (loopback)\n";
                if(addr.isLinkLocalAddress()) desc += " (link-local)\n";
                if(addr.isSiteLocalAddress()) desc += " (site-local)\n";
                if(addr.isMulticastAddress()) desc += " (multicast)\n";

                stuff += "\t" + iface.getName() + ": " + desc;
            }
        }

        showDialogBox(stuff, context);
    }
    // Dialog box

    static public void showDialogBox(String message,Context context) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setMessage(message);

        alertDialogBuilder.setNegativeButton("cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}
