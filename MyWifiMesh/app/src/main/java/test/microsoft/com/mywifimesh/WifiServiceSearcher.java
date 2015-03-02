package test.microsoft.com.mywifimesh;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION;

/**
 * Created by juksilve on 28.2.2015.
 */
public class WifiServiceSearcher  implements WifiP2pManager.ChannelListener{

    static final public String DSS_WIFISS_VALUES = "test.microsoft.com.mywifimesh.DSS_WIFISS_VALUES";
    static final public String DSS_WIFISS_MESSAGE = "test.microsoft.com.mywifimesh.DSS_WIFISS_MESSAGE";

    static final public String DSS_WIFISS_PEERCOUNT = "test.microsoft.com.mywifimesh.DSS_WIFISS_PEERCOUNT";
    static final public String DSS_WIFISS_COUNT = "test.microsoft.com.mywifimesh.DSS_WIFISS_COUNT";

    static final public String DSS_WIFISS_PEERAPINFO = "test.microsoft.com.mywifimesh.DSS_WIFISS_PEERAPINFO";
    static final public String DSS_WIFISS_INFOTEXT = "test.microsoft.com.mywifimesh.DSS_WIFISS_INFOTEXT";

    WifiServiceSearcher that = this;
    LocalBroadcastManager broadcaster;
    Context context;

    private BroadcastReceiver receiver;
    private IntentFilter filter;

    private WifiP2pManager p2p;
    private WifiP2pManager.Channel channel;
    private WifiP2pManager.DnsSdServiceResponseListener serviceListener;
    private WifiP2pManager.PeerListListener peerListListener;

    enum ServiceState{
        NONE,
        DiscoverPeer,
        DiscoverService
    }
    ServiceState myServiceState = ServiceState.NONE;


    public WifiServiceSearcher(Context Context) {
        this.context = Context;
        this.broadcaster = LocalBroadcastManager.getInstance(this.context);
    }


    public void Start() {

        p2p = (WifiP2pManager) this.context.getSystemService(this.context.WIFI_P2P_SERVICE);
        if (p2p == null) {
            debug_print("This device does not support Wi-Fi Direct");
        }else {

            channel = p2p.initialize(this.context, this.context.getMainLooper(), this);

            receiver = new ServiceSearcherReceiver();
            filter = new IntentFilter();
            filter.addAction(WIFI_P2P_STATE_CHANGED_ACTION);
            filter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
            filter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
            filter.addAction(WIFI_P2P_PEERS_CHANGED_ACTION);

            this.context.registerReceiver(receiver, filter);

            peerListListener = new WifiP2pManager.PeerListListener() {

                public void onPeersAvailable(WifiP2pDeviceList peers) {

                    final WifiP2pDeviceList pers = peers;
                    int numm = 0;
                    for (WifiP2pDevice peer : pers.getDeviceList()) {
                        numm++;
                        debug_print("\t" + numm + ": "  + peer.deviceName + " " + peer.deviceAddress);
                    }

                    if(broadcaster != null) {
                        Intent intent = new Intent(DSS_WIFISS_PEERCOUNT);
                        intent.putExtra(DSS_WIFISS_COUNT, numm);
                        broadcaster.sendBroadcast(intent);
                    }

                    if(numm > 0){
                        startServiceDiscovery();
                    }else{
                        startPeerDiscovery();
                    }
                }
            };

            serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {

                public void onDnsSdServiceAvailable(String instanceName, String serviceType, WifiP2pDevice device) {

                     if (serviceType.startsWith(MainActivity.SERVICE_TYPE)) {

                         //instance name has AP information for Client connection
                        if(broadcaster != null) {
                            Intent intent = new Intent(DSS_WIFISS_PEERAPINFO);
                            intent.putExtra(DSS_WIFISS_INFOTEXT, instanceName);
                            broadcaster.sendBroadcast(intent);
                        }


                    } else {
                        debug_print("Not our Service, :" + MainActivity.SERVICE_TYPE + "!=" + serviceType + ":");
                    }

                    startPeerDiscovery();
                }
            };

            p2p.setDnsSdResponseListeners(channel, serviceListener, null);
            startPeerDiscovery();
        }
    }


    public void Stop() {
        this.context.unregisterReceiver(receiver);
        stopDiscovery();
        stopPeerDiscovery();
    }

    @Override
    public void onChannelDisconnected() {
        //
    }
    private void startPeerDiscovery() {
        p2p.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                myServiceState = ServiceState.DiscoverPeer;
                debug_print("Started peer discovery");
            }
            public void onFailure(int reason) {debug_print("Starting peer discovery failed, error code " + reason);}
        });
    }

    private void stopPeerDiscovery() {
        p2p.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {debug_print("Stopped peer discovery");}
            public void onFailure(int reason) {debug_print("Stopping peer discovery failed, error code " + reason);}
        });
    }

    private void startServiceDiscovery() {

        WifiP2pDnsSdServiceRequest request = WifiP2pDnsSdServiceRequest.newInstance(MainActivity.SERVICE_TYPE);
        final Handler handler = new Handler();
        p2p.addServiceRequest(channel, request, new WifiP2pManager.ActionListener() {

            public void onSuccess() {
                debug_print("Added service request");
                handler.postDelayed(new Runnable() {
                    //There are supposedly a possible race-condition bug with the service discovery
                    // thus to avoid it, we are delaying the service discovery start here
                    public void run() {
                        p2p.discoverServices(channel, new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                debug_print("Started service discovery");
                                myServiceState = ServiceState.DiscoverService;
                            }
                            public void onFailure(int reason) {debug_print("Starting service discovery failed, error code " + reason);}
                        });
                    }
                }, 1000);
            }

            public void onFailure(int reason) {
                debug_print("Adding service request failed, error code " + reason);
                // No point starting service discovery
            }
        });

    }

    private void stopDiscovery() {
        p2p.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {debug_print("Cleared service requests");}
            public void onFailure(int reason) {debug_print("Clearing service requests failed, error code " + reason);}
        });
    }
    private void debug_print(String buffer) {

        if(broadcaster != null) {
            Intent intent = new Intent(DSS_WIFISS_VALUES);
            if (buffer != null)
                intent.putExtra(DSS_WIFISS_MESSAGE, buffer);
            broadcaster.sendBroadcast(intent);
        }
    }
    private class ServiceSearcherReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {

                } else {

                }
            }else if(WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if(myServiceState != ServiceState.DiscoverService) {
                    p2p.requestPeers(channel, peerListListener);
                }
            } else if(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                //WifiP2pDevice device = intent.getParcelableExtra(EXTRA_WIFI_P2P_DEVICE);
                //addText("Local device: " + MyP2PHelper.deviceToString(device));
            } else if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {

                int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED);
                String persTatu = "Discovery state changed to ";

                if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED){
                    persTatu = persTatu + "Stopped.";
                    startPeerDiscovery();
                }else if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED){
                    persTatu = persTatu + "Started.";
                }else{
                    persTatu = persTatu + "unknown  " + state;
                }
                debug_print(persTatu);

            } else if (WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    debug_print("Connected");
                    startPeerDiscovery();
                } else{
                    debug_print("DIS-Connected");
                    startPeerDiscovery();
                }
            }
        }
    }
}
