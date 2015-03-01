package com.programity.exaxis.bluetoothlampcontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothDeviceSelectActivity extends ActionBarActivity {

    // CONSTANTS -----
    private static final int REQUEST_ENABLE_BT_SCAN = 1;           // Request codes used when launching bluetooth
    private static final int REQUEST_ENABLE_BT_DEVICE_SELECT = 2;  // enabling activity to know what we should do
    private static final int REQUEST_ENABLE_BT_SEARCH_PAIRED = 3;  // when it returns a value.


    // CONTROLS -----
    private Button scanButton;
    private Button disconnectDeviceButton;
    private ListView devicesListView;
    private TextView statusText;

    // PROPERTIES -----
    //private BluetoothAdapter btAdapter;
    private Set<BluetoothDevice> pairedDevices;        // Set of bluetooth devices
    private List<BluetoothDevice> pairedDeviceList;    // Indexed list of bluetooth devices
    private ArrayAdapter<String> btArrayAdapter;       // Adapter for device list view
    private boolean showDeviceLoading;                 // Whether or not to show device loading text
    private boolean scanningNewDevices;                // Whether or not we're scanning for new devices
    private BluetoothManager btManager;                // Holds bluetooth information / objects

    // EVENT RECEIVERS -----
    final BroadcastReceiver bReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(!pairedDeviceList.contains(device)) {
                    btArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    //pairedDevices.add(device);
                    pairedDeviceList.add(device);
                    btArrayAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    // OVERRIDES -----
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_device_select);

        btManager = BluetoothManager.getManager();

        btManager.setBluetoothAdapter(BluetoothAdapter.getDefaultAdapter());
        if(btManager.getBluetoothAdapter() == null){
            Toast.makeText(getApplicationContext(),
                    "Your device does not support Bluetooth connectivity.",
                    Toast.LENGTH_LONG).show();
            // TODO: Exit
        } else{
            pairedDeviceList = new ArrayList<BluetoothDevice>();
            // Grab contexts to our controls
            scanButton = (Button) findViewById(R.id.button_Scan);
            disconnectDeviceButton = (Button) findViewById(R.id.button_DisconnectDevice);
            devicesListView = (ListView) findViewById(R.id.list_Devices);
            statusText = (TextView) findViewById(R.id.text_Status);

            disconnectDeviceButton.setEnabled(false);

            // Configure array adapter for devices
            btArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
            devicesListView.setAdapter(btArrayAdapter);

            // Set initial control visibility / state
            devicesListView.setVisibility(View.INVISIBLE);
            statusText.setVisibility(View.VISIBLE);
            scanButton.setEnabled(false);

            scanningNewDevices = false;

            // Setup control listeners
            scanButton.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    if (!btManager.isAdapterEnabled()) {
                        // Prompt turning on bluetooth
                        enableBluetooth(REQUEST_ENABLE_BT_SCAN);
                    }

                    if(!scanningNewDevices) {
                        scanNewDevices();
                        scanButton.setText(R.string.scan_button_stop_text);
                    } else{
                        stopScanningNewDevices();
                        scanButton.setText(R.string.scan_button_start_text);
                    }
                }
            });

            disconnectDeviceButton.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v){
                    btManager.clearDeviceAndConnection();
                    disconnectDeviceButton.setEnabled(false);
                }
            });

            devicesListView.setOnItemClickListener(new OnItemClickListener(){
                public void onItemClick(AdapterView parent, View view, int position, long id){
                    statusText.setText(R.string.device_connecting_text);
                    showStatus();

                    try {
                        // Check for existing connection. If it exists, drop and attempt to reconnect
                        if(btManager.isDeviceConnected()){
                            btManager.clearDeviceAndConnection();
                        }

                        // Attempt to connect to the device
                        btManager.setBluetoothDevice(pairedDeviceList.get(position));
                        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
                        btManager.setBluetoothSocket(btManager.getBluetoothDevice().createRfcommSocketToServiceRecord(uuid));
                        btManager.getBluetoothSocket().connect();

                        // Device connected! Got to the color selector.
                        disconnectDeviceButton.setEnabled(true);
                        Intent intent = new Intent(BluetoothDeviceSelectActivity.this, ColorSelectActivity.class);
                        startActivity(intent);
                    } catch (IOException ex){
                        // Couldn't connect to device.
                        btManager.clearDeviceAndConnection();
                        Toast.makeText(getApplicationContext(), "Device not found. Check the device, or select another.", Toast.LENGTH_LONG).show();
                    } finally {
                        hideStatus();
                    }
                }
            });

            if(!btManager.isAdapterEnabled()){
                enableBluetooth(REQUEST_ENABLE_BT_SEARCH_PAIRED);
            }
            else {
                // Get and display list of paired devices
                scanPairedDevices();

                devicesListView.setVisibility(View.VISIBLE);
                hideStatus();
                scanButton.setEnabled(true);
            }
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try {
            unregisterReceiver(bReceiver);
            btManager.clearDeviceAndConnection();
        } catch (Exception ex){

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bluetooth_device_select, menu);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == REQUEST_ENABLE_BT_SEARCH_PAIRED){
            if(resultCode == RESULT_OK){
                // Get and display list of paired devices
                scanPairedDevices();

                devicesListView.setVisibility(View.VISIBLE);
                //statusText.setVisibility(View.GONE);
                hideStatus();
                scanButton.setEnabled(true);
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth must be enabled for this app to function.", Toast.LENGTH_LONG);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // PRIVATE METHODS -----
    /// Purpose: Make a request to enable bluetooth.
    /// requestCode: The request code passed to startActivityForResult.
    private void enableBluetooth(int requestCode){
        Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(turnOnIntent, requestCode);
    }

    private void disableBluetooth(){

    }

    private void cleanupAdapter(){

    }

    private void scanPairedDevices(){
        pairedDevices = btManager.getBondedDevices();

        for(BluetoothDevice device : pairedDevices){
            btArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            pairedDeviceList.add(device);
        }
        btArrayAdapter.notifyDataSetChanged();
    }

    private void scanNewDevices(){
        if(btManager.isDicovering()){
            stopScanningNewDevices();
            scanningNewDevices = false;
        } else{
            //btArrayAdapter.clear();
            btManager.startDiscovery();
            scanningNewDevices = true;
            // Register reciever
            registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }
    }

    private void stopScanningNewDevices()
    {
        btManager.stopDiscovery();
        try {
            unregisterReceiver(bReceiver);
        } catch(Exception ex){

        }
    }

    private void showStatus(){
        statusText.setVisibility(View.VISIBLE);
    }

    private void hideStatus(){
        statusText.setVisibility(View.GONE);
    }
}
