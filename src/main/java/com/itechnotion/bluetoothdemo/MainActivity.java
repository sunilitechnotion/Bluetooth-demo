/*
* Copyright 2013 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package com.itechnotion.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * {@link android.support.v4.app.Fragment} which can display a view.
 * <p>
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener {

    String[] values = new String[]{
            "Check Bluetooth Compatibility",
            "Turn On Bluetooth",
            "Make Discoverable",
            "Show Paired And Online BT devices",
            "Cancel Discovery",
            "Disconnect",
            "Turn Off Bluetooth",
    };

    BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVERABLE_BT = 0;

    private static final String TAG = "Bluetooth";

    ListView listView;
    private CoordinatorLayout coordinatorLayout;
    ArrayList<BluetoothDevice> devices;
    ArrayList<String> allDevices;
    private BluetoothDevice deviceToConnect;

    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private BluetoothSocket curBTSocket = null;

    ClientThread connectThread;
    DeviceConnectThread deviceConnectThread;
    ServerConnectThread serverConnectThread;

    AlertDialog alertDialogObject;
    ArrayAdapter<String> devicesListAdapter;

    LinearLayout linSendMessage;
    Button btnSend;
    EditText edtMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id
                .coordinatorLayout);

        linSendMessage = (LinearLayout) findViewById(R.id.l1);
        listView = (ListView) findViewById(R.id.list);
        btnSend = (Button) findViewById(R.id.btnSend);
        edtMessage = (EditText) findViewById(R.id.edtMessage);

        btnSend.setOnClickListener(this);


        ArrayAdapter<String> adapter;
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bReciever, filter);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mBluetoothAdapter.isEnabled()) {
            startAsServer();
        }
    }

    private void turnOn() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void makeDiscoverable() {
        if (!mBluetoothAdapter.isDiscovering()) {
            showMessage("Making Discoverable...");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivityForResult(enableBtIntent, REQUEST_DISCOVERABLE_BT);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {

        switch (position) {
            case 0:
                checkCompatibility();
                break;
            case 1:
                turnOn();
                break;
            case 2:
                makeDiscoverable();
                break;
            case 3:
                startDiscovery();
                break;
            case 4:
                cancelDiscovery();
                break;
            case 5:
                disconnect();
                break;
            default:
                turnOff();

        }

    }

    private void disconnect() {
        if (curBTSocket != null) {
            try {
                curBTSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private void startDiscovery() {
        showMessage("Starting Discovery...");
        getPairedDevices();
        mBluetoothAdapter.startDiscovery();
    }

    private void cancelDiscovery() {
        showMessage("Cancelling Discovery...");
        unregisterReceiver(bReciever);
        mBluetoothAdapter.cancelDiscovery();
    }

    private void getPairedDevices() {

        if (devices == null)
            devices = new ArrayList<BluetoothDevice>();
        else
            devices.clear();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice curDevice : pairedDevices) {
                devices.add(curDevice);
            }
            Log.i(TAG, "Paired Number of Devices : " + pairedDevices.size());
            showPairedList();
        }
    }

    private void turnOff() {
        mBluetoothAdapter.disable();
    }

    private void checkCompatibility() {
        // Phone does not support Bluetooth so let the user know and exit.
        if (mBluetoothAdapter == null) {
            showMessage("Your phone does not support Bluetooth");
        } else {
            showMessage("Your phone supports Bluetooth ");
        }
    }

    private final BroadcastReceiver bReciever = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice curDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devices.add(curDevice);
            }
            Log.i(TAG, "All BT Devices : " + devices.size());
            if (devices.size() > 0) {
                showPairedList();
            }
        }
    };

    public void connectAsClient() {
        showMessage("Connecting for online Bluetooth devices...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (deviceToConnect != null) {
                    if (connectThread != null) {
                        connectThread.cancel();
                        connectThread = null;
                        linSendMessage.setVisibility(View.GONE);
                    }
                    connectThread = new ClientThread();
                    curBTSocket = connectThread.connect(mBluetoothAdapter, deviceToConnect, MY_UUID_SECURE, mHandler);
                    connectThread.start();
                }
            }
        }).start();
    }

    public void killServerThread() {
        if (serverConnectThread != null) {
            serverConnectThread.closeConnection();
            serverConnectThread = null;
            linSendMessage.setVisibility(View.GONE);
        }
    }

    private void startAsServer() {
        showMessage("Listening for online Bluetooth devices...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                serverConnectThread = new ServerConnectThread();
                curBTSocket = serverConnectThread.acceptConnection(mBluetoothAdapter, MY_UUID_SECURE, mHandler);
            }
        }).start();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            byte[] buf = (byte[]) msg.obj;

            switch (msg.what) {

                case Constants.MESSAGE_WRITE:
                    // construct a string from the buffer
                    String writeMessage = new String(buf);
                    Log.i(TAG, "Write Message : " + writeMessage);
                    showMessage("Message Sent : " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(buf, 0, msg.arg1);
                    Log.i(TAG, "readMessage : " + readMessage);
                    showMessage("Message Received : " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    String mConnectedDeviceName = new String(buf);
                    showMessage("Connected to " + mConnectedDeviceName);
                    linSendMessage.setVisibility(View.VISIBLE);
                    sendMessageToDevice();
                    break;
                case Constants.MESSAGE_SERVER_CONNECTED:
                    showMessage("CONNECTED");
                    Log.i(TAG, "Connected...");
                    linSendMessage.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };

    public void sendMessageToDevice() {
        deviceConnectThread = new DeviceConnectThread(curBTSocket, mHandler);
        deviceConnectThread.start();
        String message = edtMessage.getText().toString().trim();
        if (message.length() > 0) {
            byte[] send = message.getBytes();
            deviceConnectThread.write(send);
        }
    }

    public void showMessage(String message) {
        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, message, Snackbar.LENGTH_LONG);
        View view = snackbar.getView();
        view.setBackgroundColor(Color.GREEN);
        TextView textView = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.BLACK);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
        params.gravity = Gravity.BOTTOM;
        view.setLayoutParams(params);
        snackbar.show();
    }

    public void showPairedList() {

        List<String> tempDevices = new ArrayList<String>();

        for (BluetoothDevice b : devices) {
            String paired = "Paired";
            if (b.getBondState() != 12) {
                paired = "Not Paired";
            }
            tempDevices.add(b.getName() + " - [ " + paired + " ] ");
        }

        if (allDevices == null)
            allDevices = new ArrayList<String>();
        else
            allDevices.clear();

        allDevices.addAll(tempDevices);

        if (devicesListAdapter == null) {

            ListView devicesList = new ListView(this);
            devicesList.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            devicesListAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, allDevices);
            devicesList.setAdapter(devicesListAdapter);
            //Create sequence of items
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Paired/Unpaired BT Devices");
            dialogBuilder.setView(devicesList);
            //Create alert dialog object via builder
            final AlertDialog alertDialogObject = dialogBuilder.create();
            devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    deviceToConnect = devices.get(position);
                    devicesListAdapter = null;
                    alertDialogObject.dismiss();
                    Log.i(TAG, "Connecting to device :" + deviceToConnect.getName());
                    showMessage("Connecting to device " + deviceToConnect.getName());

                    //Now this is not the server...
                    killServerThread();

                    //Connect to the other device which is a server...
                    connectAsClient();
                }
            });
            //Show the dialog
            alertDialogObject.show();
            alertDialogObject.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    devicesListAdapter = null;
                }
            });
        } else {
            devicesListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onClick(View v) {
        sendMessageToDevice();
    }

}