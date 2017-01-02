package com.samuraibros.fatdolly;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Takondwa Kakusa on 12/31/2016.
 */

public class FileServerAsyncTask extends AsyncTask<Void, Void, Void> {
    private Context context;
    private TextView statusText;
    InputStream nis; //Network Input Stream
    OutputStream nos; //Network Output Stream

    /*
    public FileServerAsyncTask(Context context, View statusText) {
        this.context = context;
        this.statusText = (TextView) statusText;
    }
    */

    @Override
    protected Void doInBackground(Void... params) {
        try {
            /**
             * Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */
            ServerSocket serverSocket = new ServerSocket(8888);
            Socket client = serverSocket.accept();

            /**
             * If this code is reached, a client has connected and transferred data
             * Save the input stream from the client as a JPEG file
             */

            nis = client.getInputStream();
            Log.i("AsyncTask", "doInBackground: Socket created, streams assigned");
            Toast.makeText(this.context, "Message Received!", Toast.LENGTH_SHORT).show();
            serverSocket.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
