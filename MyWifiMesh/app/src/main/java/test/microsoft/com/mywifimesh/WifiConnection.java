package test.microsoft.com.mywifimesh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.RouteInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Formatter;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.net.Inet4Address;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;

/**
 * Created by juksilve on 28.2.2015.
 */
public class WifiConnection {

    static final public String DSS_WIFICON_VALUES = "test.microsoft.com.mywifimesh.DSS_WIFICON_VALUES";
    static final public String DSS_WIFICON_MESSAGE = "test.microsoft.com.mywifimesh.DSS_WIFICON_MESSAGE";

    static final public String DSS_WIFICON_STATUSVAL = "test.microsoft.com.mywifimesh.DSS_WIFICON_STATUSVAL";
    static final public String DSS_WIFICON_CONSTATUS = "test.microsoft.com.mywifimesh.DSS_WIFICON_CONSTATUS";

    static final public String DSS_WIFICON_SERVERADDRESS = "test.microsoft.com.mywifimesh.DSS_WIFICON_SERVERADDRESS";
    static final public String DSS_WIFICON_INETADDRESS = "test.microsoft.com.mywifimesh.DSS_WIFICON_INETADDRESS";


    static final public int ConectionStateNONE = 0;
    static final public int ConectionStatePreConnecting = 1;
    static final public int ConectionStateConnecting = 2;
    static final public int ConectionStateConnected = 3;
    static final public int ConectionStateDisconnected = 4;

    private int  mConectionState = ConectionStateNONE;

    private boolean hadConnection = false;

    WifiConnection that = this;
    WifiManager wifiManager = null;
    WifiConfiguration wifiConfig = null;
    Context context = null;
    int netId = 0;

    LocalBroadcastManager broadcaster;
    WiFiConnectionReceiver receiver;
    private IntentFilter filter;

    String intetAddress = "";

    public WifiConnection(Context Context, String SSIS, String password) {
        this.context = Context;

        broadcaster = LocalBroadcastManager.getInstance(this.context);

        receiver = new WiFiConnectionReceiver();
        filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        this.context.registerReceiver(receiver, filter);

        this.wifiManager = (WifiManager)this.context.getSystemService(this.context.WIFI_SERVICE);

        this.wifiConfig = new WifiConfiguration();
        this.wifiConfig.SSID = String.format("\"%s\"", SSIS);
        this.wifiConfig.preSharedKey = String.format("\"%s\"", password);

        this.netId = this.wifiManager.addNetwork(this.wifiConfig);
     //   this.wifiManager.disconnect();
        this.wifiManager.enableNetwork(this.netId, false);
        this.wifiManager.reconnect();
    }

    public void Stop(){
        this.context.unregisterReceiver(receiver);
        this.wifiManager.disconnect();
    }

    public void SetInetAddress(String address){
        this.intetAddress = address;
    }

    public String GetInetAddress(){
        return this.intetAddress;
    }

    private class WiFiConnectionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if(info != null) {

                    if (info.isConnected()) {
                        hadConnection = true;
                        mConectionState = ConectionStateConnected;
                    }else if(info.isConnectedOrConnecting()) {
                        mConectionState = ConectionStateConnecting;
                    }else {
                        if(hadConnection){
                            mConectionState = ConectionStateDisconnected;
                        }else{
                            mConectionState = ConectionStatePreConnecting;
                        }
                    }

                    if(broadcaster != null) {
                        Intent sndeInt = new Intent(DSS_WIFICON_VALUES);
                        sndeInt.putExtra(DSS_WIFICON_MESSAGE,"DetailedState: " + info.getDetailedState() );
                        broadcaster.sendBroadcast(sndeInt);
                    }


                    if(broadcaster != null) {
                        Intent sndeInt = new Intent(DSS_WIFICON_STATUSVAL);
                        sndeInt.putExtra(DSS_WIFICON_CONSTATUS, mConectionState);
                        broadcaster.sendBroadcast(sndeInt);
                    }
                }

                WifiInfo wiffo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                if(wiffo != null){
                    if(broadcaster != null) {
                        Intent snInt = new Intent(DSS_WIFICON_SERVERADDRESS);
                        snInt.putExtra(DSS_WIFICON_INETADDRESS, wiffo.getIpAddress());
                        broadcaster.sendBroadcast(snInt);
                    }

                    // you could get otherparty IP via:
                    // http://stackoverflow.com/questions/10053385/how-to-get-each-devices-ip-address-in-wifi-direct-scenario
                    // as well if needed
                }
            }
        }
    }
}
