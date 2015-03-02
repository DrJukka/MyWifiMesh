
package test.microsoft.com.mywifimesh;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The implementation of a ServerSocket handler. This is used by the wifi p2p
 * group owner.
 */
public class GroupOwnerSocketHandler extends Thread {

    static final public String DSS_GROUP_VALUES = "test.microsoft.com.mywifimesh.DSS_GROUP_VALUES";
    static final public String DSS_GROUP_MESSAGE = "test.microsoft.com.mywifimesh.DSS_GROUP_MESSAGE";

    LocalBroadcastManager broadcaster;
    ServerSocket socket = null;
    private Handler handler;
    private static final String TAG = "GroupOwnerSocketHandler";
    private ChatManager chat;

    public GroupOwnerSocketHandler(Handler handler, int port,Context context) throws IOException {
        try {
            this.broadcaster = LocalBroadcastManager.getInstance(context);
            socket = new ServerSocket(port);
            this.handler = handler;
            Log.d("GroupOwnerSocketHandler", "Socket Started");
        } catch (Exception e) {
            if(broadcaster != null) {
                Intent intent = new Intent(DSS_GROUP_VALUES);
                intent.putExtra(DSS_GROUP_MESSAGE, e.toString());
                broadcaster.sendBroadcast(intent);
            }
            throw e;
        }

    }

    /**
     * A ThreadPool for client sockets.

    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

     */
    @Override
    public void run() {
        while (true) {
            try {
                // A blocking operation. Initiate a ChatManager instance when
                // there is a new connection
                Socket  s = socket.accept();
                Log.d(TAG, "Launching the Group I/O handler");
                chat = new ChatManager(s, handler, "Group");
                new Thread(chat).start();

            } catch (Exception e) {
                try {
                    if (socket != null && !socket.isClosed())
                        socket.close();
                } catch (Exception ioe) {
                    if(broadcaster != null) {
                        Intent intent = new Intent(DSS_GROUP_VALUES);
                        intent.putExtra(DSS_GROUP_MESSAGE, ioe.toString());
                        broadcaster.sendBroadcast(intent);
                    }
                }
                if(broadcaster != null) {
                    Intent intent = new Intent(DSS_GROUP_VALUES);
                    intent.putExtra(DSS_GROUP_MESSAGE, e.toString());
                    broadcaster.sendBroadcast(intent);
                }

                break;
            }
        }
    }



    public ChatManager getChat() {
        return chat;
    }

}
