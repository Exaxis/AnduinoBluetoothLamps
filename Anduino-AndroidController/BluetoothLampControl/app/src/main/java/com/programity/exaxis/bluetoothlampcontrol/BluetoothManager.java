package com.programity.exaxis.bluetoothlampcontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Mahlon on 2/28/2015.
 */
public class BluetoothManager {
    // CONSTANTS -----

    // FIELDS -----
    private BluetoothAdapter btAdapter;
    private BluetoothDevice btDevice;
    private BluetoothSocket btSocket;
    private static final BluetoothManager manager = new BluetoothManager();  // Singleton Instance

    // ACCESSOR METHODS -----
    // Get
    public BluetoothAdapter getBluetoothAdapter(){return btAdapter;}
    public BluetoothDevice getBluetoothDevice(){return btDevice;}
    public BluetoothSocket getBluetoothSocket(){return btSocket;}
    public static BluetoothManager getManager(){return manager;}      // Singleton Instancing
    // Set
    public void setBluetoothAdapter(BluetoothAdapter adapter){btAdapter = adapter;}
    public void setBluetoothDevice(BluetoothDevice device){btDevice = device;}
    public void setBluetoothSocket(BluetoothSocket socket){btSocket = socket;}

    // PUBLIC METHODS -----
    public boolean isAdapterEnabled(){
        if(btAdapter != null) {
            return btAdapter.isEnabled();
        } else {
            return false;
        }
    }

    public boolean isDicovering(){
        if(btAdapter != null) {
            return btAdapter.isDiscovering();
        } else{
            return false;
        }
    }

    public Set<BluetoothDevice> getBondedDevices(){
        if(btAdapter != null) {
            return btAdapter.getBondedDevices();
        } else{
            return new HashSet<BluetoothDevice>();
        }
    }

    public void startDiscovery(){
        if(!btAdapter.isDiscovering()){
            btAdapter.startDiscovery();
        }
    }

    public void stopDiscovery(){
        btAdapter.cancelDiscovery();
    }

    public boolean isDeviceConnected(){
        if(btAdapter != null && btSocket != null) {
            return btSocket.isConnected();
        } else{
            return false;
        }
    }

    public void closeConnection(){
        if(isDeviceConnected()){
            try {
                btSocket.close();
            } catch (IOException ioex){

            }
        }
    }

    public void clearDeviceAndConnection(){
        closeConnection();
        btSocket = null;
        btDevice = null;
    }

    // PRIVATE METHODS -----
}
