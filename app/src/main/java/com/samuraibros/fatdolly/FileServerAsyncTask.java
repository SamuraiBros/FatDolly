package com.samuraibros.fatdolly;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by Takondwa Kakusa on 12/31/2016.
 */

public class FileServerAsyncTask extends AsyncTask<Void, Void, Void> {
    private Context context;
    public static final int BUFFER_SIZE = 2048;
    InputStream nis; //Network Input Stream
    OutputStream nos; //Network Output Stream


    public FileServerAsyncTask(Context context) {
        this.context = context;
    }


    @Override
    protected Void doInBackground(Void... params) {
        while (true) {
            try {
                Log.i("AsyncTask", "doInBackground: Backgroud Task Started: ");
                /**
                 * Create a server socket and wait for client connections. This
                 * call blocks until a connection is accepted from a client
                 */
                ServerSocket serverSocket = new ServerSocket(8888);
                Log.i("AsyncTask", "doInBackground: Server Socket Created: " + getIpAddress());
                Socket client = serverSocket.accept();


                /**
                 * If this code is reached, a client has connected and transferred data
                 * Save the input stream from the client as a JPEG file
                 */

                Log.d("AsyncTask", "doInBackground: Socket created, streams assigned");
                nis = client.getInputStream();

                String msgOut = receiveDataFromServer();

                Intent i = new Intent(msgOut);
                context.sendBroadcast(i);




                Log.i("AsyncTask", "doInBackground: Message Received: " + msgOut);
                //Toast.makeText(this.context, "Message Received!", Toast.LENGTH_SHORT).show();
                serverSocket.close();


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
    }

    public String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress
                            .nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        if (inetAddress.getHostAddress().toString().toLowerCase().contains("192.168")) {
                            ip += "Server running at : "
                                    + inetAddress.getHostAddress();
                        }
                    }
                }
            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }
        return ip;
    }

    public String receiveDataFromServer() {
        try {
            String message = "";
            int charsRead = 0;
            byte[] buffer = new byte[BUFFER_SIZE];

            while ((charsRead = nis.read(buffer)) != -1) {
                message += new String(buffer).substring(0, charsRead);
            }

            return message;
        } catch (IOException e) {
            return "Error receiving response:  " + e.getMessage();
        }
    }
}
