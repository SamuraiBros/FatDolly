package com.samuraibros.fatdolly;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Created by Takondwa Kakusa on 1/2/2017.
 */

public class Client extends AsyncTask<Void, Void, Void> {
    String message = "";

    //Socket for sending as client
    Socket clientSocket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;

    //String used to store the host address
    String host;

    //Port that is being sent to
    int port;

    Client(String addr, int port, String message) {
        Log.d("FatDolly", "Client:Client Being Created");
        this.host = "192.168.49.1";
        this.port = port;
        this.message = message;
        Log.d("FatDolly", "Client:Client Created");
    }

    @Override
    protected Void doInBackground(Void... params) {
        sendMessage(message);
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
    }

    private void connectWithServer() {
        Log.d("Client", "Client:Connect to Server: Start Connection... " + getIpAddress());
        try {
            if (clientSocket == null) {
                clientSocket = new Socket(host, 8888);
                out = new PrintWriter(clientSocket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("Client", "Client:Connect to Server: Connection Started...");
    }

    private void disConnectWithServer() {
        if (clientSocket != null) {
            if (clientSocket.isConnected()) {
                try {
                    in.close();
                    out.close();
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendMessage(String message) {
        Log.d("Client", "Client:Send a Message: Start Sending... ");
        if (message != null) {
            connectWithServer();
            out.write(message);
            out.flush();
            disConnectWithServer();
        }
        Log.d("Client", "Client:Send a Message: Done Sending...");
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
                        if (inetAddress.getHostAddress().toString().toLowerCase().contains("192.168.49")) {
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

}