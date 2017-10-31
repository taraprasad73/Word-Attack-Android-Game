package taraprasad73.wordattack;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Handles reading and writing of messages with socket buffers. Uses a Handler
 * to post messages to UI thread for UI updates.
 */
public class ChatManager implements Runnable {//Chat Manager has a run() method

    private static final String TAG = "ChatHandler";
    private Socket socket = null;
    private Handler handler;//This field is going to contain the Handler associated with the UI Thread
    private InputStream iStream;
    private OutputStream oStream;

    public ChatManager(Socket socket, Handler handler) {
        this.socket = socket;//associated with the device
        this.handler = handler;
    }

    @Override
    public void run() {
        try {

            iStream = socket.getInputStream();//device's input and output stream
            oStream = socket.getOutputStream();
            byte[] buffer = new byte[1024];
            int bytes;
            handler.obtainMessage(WiFiServiceDiscoveryActivity.MY_HANDLE, this)
                    .sendToTarget();//sends to the UI Thread, which is the target here as it created and hence
            //associated with this handler. The UI Thread then handles it by passing this chat Manager object
            //to the ChatFragment's setChatManager() method. Upon this, whenever the send button is pressed, it is
            //displayed in the chat fragment, as well as it is sent to the output stream
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = iStream.read(buffer);
                    if (bytes == -1) {
                        break;
                    }

                    // Send the obtained bytes to the UI Activity
                    Log.d(TAG, "Rec:" + String.valueOf(buffer));
                    handler.obtainMessage(WiFiServiceDiscoveryActivity.MESSAGE_READ,
                            bytes, -1, buffer).sendToTarget();//after reading the data from the input stream
                    //it sends a message to the UI Thread to add this data to its chat fragment
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(byte[] buffer) {
        try {
            oStream.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }

}
