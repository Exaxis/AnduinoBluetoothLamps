package com.programity.exaxis.bluetoothlampcontrol;

import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SVBar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class ColorSelectActivity extends ActionBarActivity implements ColorPicker.OnColorChangedListener {

    // CONSTANTS -----
    private int HUE_INDEX = 0;
    private int SAT_INDEX = 1;
    private int BRIGHT_INDEX = 2;

    // CONTROLS -----
    ColorPicker colorPicker;
    SVBar svBar;
    TextView textView;

    // FIELDS -----
    BluetoothManager btManager;
    Thread workerThread;
    InputStream inputStream;
    OutputStream outputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_select);

        btManager = BluetoothManager.getManager();
        if(btManager.isDeviceConnected()) {
            try {
                outputStream = btManager.getBluetoothSocket().getOutputStream();
                inputStream = btManager.getBluetoothSocket().getInputStream();

                Bundle bundle = getIntent().getExtras();

                colorPicker = (ColorPicker) findViewById(R.id.colorPicker_Color);
                svBar = (SVBar) findViewById(R.id.svbar_SVBar);
                textView = (TextView) findViewById(R.id.text_Color);

                colorPicker.addSVBar(svBar);
                colorPicker.setOnColorChangedListener(this);
            } catch (IOException ioex){
                // TODO: Handle error
            }
        } else{
            // TODO: Handle error
        }
    }


    // OVERRIDES -----
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_color_select, menu);
        return true;
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

    @Override
    public void onColorChanged(int i) {
        sendData(formatColorDataStringHSB(i));
    }

    // PRIVATE METHODS -----
    public String formatColorDataStringHSB(int color){
        float[] hsb = new float[3];
        Color.colorToHSV(color, hsb);
        int h = Math.round(hsb[0]);
        int s = Math.round(hsb[1]*99);
        int b = Math.round(hsb[2]*99);

        return "C" + h + "," + s + "," + b + "\n";
    }

    private void sendData(String data){
        // Ensure our data has a termination character
        data += "\n";
        try {
            outputStream.write(data.getBytes());
        } catch (IOException ioex){
            // TODO: Handle error
        }


    }
}
