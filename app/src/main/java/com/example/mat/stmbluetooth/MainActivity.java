package com.example.mat.stmbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Target;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends ActionBarActivity {

    private Button btZatrzymaj, btWznow, btWyszukaj, btReset;
    private ToggleButton mBtPattern1, mBtPattern2, mBtPattern3;

    private Switch mRedSwitch, mGreenSwitch, mBlueSwitch, mOrangeSwitch;
    private SeekBar mRedSeekBar, mGreenSeekBar, mBlueSeekBar, mOrangeSeekBar, mPatternSeekBar;

    private BluetoothAdapter mBA;
    private Set<BluetoothDevice> pairedDevices;
    private ListView lv;
    private ArrayAdapter mArrayAdapter;
    private ProgressBar pbLista;
    private ConnectedThread manageConnectedSocket;

    private final int GREEN_LED = 1;
    private final int ORANGE_LED = 2;
    private final int RED_LED = 3;
    private final int BLUE_LED = 4;

    private final int PWM_CHANGE = 5; //ktora dioda? % wypelnienia? 0-100
    private final int PATTERN_1 = 6; //predkosc dzialania wzoru? 0-100
    private final int PATTERN_2 = 7; //j.w
    private final int PATTERN_3 = 8; //j.w
    private final int PATTERN_HOLD = 9; //zatrzymuje wykonywanie patternu
    private final int PATTERN_RESUME = 10; //wznawia wykonywanie patternu
    private final int RESET = 11; //reset modulu
    private final String TAG = "STMBluetooth: ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBA = BluetoothAdapter.getDefaultAdapter();
        pbLista = (ProgressBar) findViewById(R.id.progressBar);
        pbLista.setVisibility(View.INVISIBLE); //wylacza progress bar wyszukiwania urzadzen

        // ----BUTTON INIT ---
        mBtPattern1 = (ToggleButton) findViewById(R.id.pattern1);
        mBtPattern2 = (ToggleButton) findViewById(R.id.pattern2);
        mBtPattern3 = (ToggleButton) findViewById(R.id.pattern3);

        btWyszukaj = (Button) findViewById(R.id.btWyszukaj);
        btWznow = (Button) findViewById(R.id.btWznow);
        btZatrzymaj = (Button) findViewById(R.id.btZatrzymaj);
        btReset = (Button) findViewById(R.id.btReset);
        // ----/BUTTON INIT ---

        // --- SWITCH INIT -----
        mRedSwitch = (Switch) findViewById(R.id.RedSwitch);
        mGreenSwitch = (Switch) findViewById(R.id.GreenSwitch);
        mBlueSwitch = (Switch) findViewById(R.id.BlueSwitch);
        mOrangeSwitch = (Switch) findViewById(R.id.OrangeSwitch);
        //----/SWITCH INIT/ ----

        //----SEEK BAR INIT -----
        mRedSeekBar = (SeekBar) findViewById(R.id.RedSeekBar);
        mGreenSeekBar = (SeekBar) findViewById(R.id.GreenSeekBar);
        mBlueSeekBar = (SeekBar) findViewById(R.id.BlueSeekBar);
        mOrangeSeekBar = (SeekBar) findViewById(R.id.OrangeSeekBar);
        mPatternSeekBar = (SeekBar) findViewById(R.id.patternSeekBar);
        //----/SEEK BAR INIT/ --- -

        //---DISABLE ALL BUTTON TILL CONNECT TO DEVICE---
        disablePatternButtons(true);
        disableSeekBars(true);
        disableSwitches(true);
        //---/DISABLE ALL BUTTON TILL CONNECT TO DEVICE/



        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        if(mBA.isEnabled()) {
            BluetoothDevice dev = mBA.getRemoteDevice((String)
                    getResources().getText(R.string.default_mac)); //automatycznie laczy sie z domyslnym urzadzeniem
            new BluetoothClient(dev).run();
        }
        // ------ INIT LISTA DOSTEPNYCH URZADZEN----
        lv = (ListView) findViewById(R.id.listView);
        mArrayAdapter = new ArrayAdapter
                (this, android.R.layout.simple_list_item_1);
        lv.setAdapter(mArrayAdapter);
        //--INIT LISTA DOSTEPNYCH URZADZEN--

        mPatternSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mBtPattern1.isChecked()) {
                    turnOnPattern1();
                } else if (mBtPattern2.isChecked()) {
                    turnOnPattern2();
                } else if (mBtPattern3.isChecked()) {
                    turnOnPattern3();
                }

            }
        });

        mBtPattern1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isChecked()) //wlaczony
                {
                    turnOnPattern1();
                } else
                {
                    manageConnectedSocket.write(PATTERN_HOLD);
                }
            }
        });
        mBtPattern2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isChecked()) //wlaczony
                {
                    turnOnPattern2();
                } else
                {
                    manageConnectedSocket.write(PATTERN_HOLD);
                }
            }
        });
        mBtPattern3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isChecked()) //wlaczony
                {
                    turnOnPattern3();
                } else {
                    manageConnectedSocket.write(PATTERN_HOLD);
                }
            }
        });

        btZatrzymaj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manageConnectedSocket.write(PATTERN_HOLD);
                disableSwitches(false);
            }
        });
        btWznow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manageConnectedSocket.write(PATTERN_RESUME);
            }
        });
        btReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manageConnectedSocket.write(RESET);

                unCheckSwitches();
                unCheckPatternButtons();

                mPatternSeekBar.setProgress(0);

                disablePatternButtons(false);
                disableSeekBars(false);
                disableSwitches(false);
            }

        });

        //CONNECT TO DEVICE FROM LIST
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String listItem = lv.getItemAtPosition(position).toString();
                String[] parts = listItem.split("\n");
                String mac = parts[1];
                BluetoothDevice serwer = mBA.getRemoteDevice(mac);
                new BluetoothClient(serwer).run();
            }
        });
        mRedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0)
                    mRedSwitch.setChecked(false);
                else
                    mRedSwitch.setChecked(true);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                LED_Switch(RED_LED, mRedSeekBar.getProgress());
                if (seekBar.getProgress() > 0)
                    mRedSwitch.setChecked(true);
            }
        });

        mGreenSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0)
                    mGreenSwitch.setChecked(false);
                else
                    mGreenSwitch.setChecked(true);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                LED_Switch(GREEN_LED, seekBar.getProgress());
                if (seekBar.getProgress() > 0)
                    mGreenSwitch.setChecked(true);
            }
        });
        mOrangeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0)
                    mOrangeSwitch.setChecked(false);
                else
                    mOrangeSwitch.setChecked(true);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                LED_Switch(ORANGE_LED, seekBar.getProgress());
                if (seekBar.getProgress() > 0)
                    mOrangeSwitch.setChecked(true);
            }
        });

        mBlueSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0)
                    mBlueSwitch.setChecked(false);
                else
                    mBlueSwitch.setChecked(true);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                LED_Switch(BLUE_LED, seekBar.getProgress());
                if (seekBar.getProgress() > 0)
                    mBlueSwitch.setChecked(true);
            }
        });
        mRedSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if(isChecked == true)
//                    mRedSeekBar.setEnabled(true);
//                else
//                    mRedSeekBar.setEnabled(false);
//               int mPWM = mRedSeekBar.getProgress();
//                if (isChecked)
//                {
//                    if (mPWM < 50)
//                        mPWM /= 2;
//                    try
//                    {
//                        manageConnectedSocket.write(PWM_CHANGE);
//                        manageConnectedSocket.write(RED_LED);
//                        manageConnectedSocket.write(mPWM);
//                        Toast.makeText(getApplicationContext(), "Turn On LED", Toast.LENGTH_SHORT).show();
//                        buttonView.setChecked(true);
//                    } catch (Exception e)
//                    {
//                        buttonView.setChecked(false);
//                        Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
//                        Log.i("blad wysylania", e.getMessage());
//                    }
//                }
//                else
//                    manageConnectedSocket.write(RED_LED);
                // LED_Switch(buttonView, isChecked, RED_LED, mRedSeekBar.getProgress());
                LED_Switch(buttonView, mRedSeekBar ,isChecked, RED_LED, mRedSeekBar.getProgress());
            }
        });
        mGreenSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LED_Switch(buttonView, mGreenSeekBar , isChecked, GREEN_LED, mGreenSeekBar.getProgress());
            }
        });
        mOrangeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LED_Switch(buttonView, mOrangeSeekBar , isChecked, ORANGE_LED, mOrangeSeekBar.getProgress());
            }
        });
        mBlueSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LED_Switch(buttonView, mBlueSeekBar ,isChecked, BLUE_LED, mBlueSeekBar.getProgress());
            }
        });

    }

    private void turnOnPattern3() {
        manageConnectedSocket.write(PATTERN_3);
        manageConnectedSocket.write(mPatternSeekBar.getProgress());

        unCheckSwitches();
        disableSwitches(true);
        disableSeekBars(true);

        mBtPattern1.setChecked(false);
        mBtPattern2.setChecked(false);
    }

    private void turnOnPattern2() {
        manageConnectedSocket.write(PATTERN_2);
        manageConnectedSocket.write(mPatternSeekBar.getProgress());

        unCheckSwitches();
        disableSwitches(true);
        disableSeekBars(true);

        mBtPattern1.setChecked(false);
        mBtPattern3.setChecked(false);
    }

    private void turnOnPattern1() {
        manageConnectedSocket.write(PATTERN_1);
        manageConnectedSocket.write(mPatternSeekBar.getProgress());

        unCheckSwitches();
        disableSwitches(true);
        disableSeekBars(true);

        mBtPattern2.setChecked(false);
        mBtPattern3.setChecked(false);
    }

    private void unCheckPatternButtons() {
        mBtPattern1.setChecked(false);
        mBtPattern2.setChecked(false);
        mBtPattern3.setChecked(false);
    }

    private void unCheckSwitches() {
        mRedSwitch.setChecked(false);
        mOrangeSwitch.setChecked(false);
        mBlueSwitch.setChecked(false);
        mGreenSwitch.setChecked(false);

        mRedSeekBar.setProgress(0);
        mOrangeSeekBar.setProgress(0);
        mBlueSeekBar.setProgress(0);
        mGreenSeekBar.setProgress(0);
    }

    private void disableSeekBars(boolean enableAll) {
        enableAll = !enableAll;
        mRedSeekBar.setEnabled(enableAll);
        mGreenSeekBar.setEnabled(enableAll);
        mBlueSeekBar.setEnabled(enableAll);
        mOrangeSeekBar.setEnabled(enableAll);
    }

    private void disableSwitches(boolean enableAll) {
        enableAll = !enableAll;
        mRedSwitch.setEnabled(enableAll);
        mGreenSwitch.setEnabled(enableAll);
        mBlueSwitch.setEnabled(enableAll);
        mOrangeSwitch.setEnabled(enableAll);
    }

    private void disablePatternButtons(boolean enableAll) {
        enableAll = !enableAll;
        btWznow.setEnabled(enableAll);
        btReset.setEnabled(enableAll);
        btZatrzymaj.setEnabled(enableAll);
        mBtPattern1.setEnabled(enableAll);
        mBtPattern2.setEnabled(enableAll);
        mBtPattern3.setEnabled(enableAll);
        mPatternSeekBar.setEnabled(enableAll);
    }

    private void LED_Switch(CompoundButton buttonView,SeekBar seekbar, boolean isChecked, int mLED, int mPWM) {
        if (isChecked)
            seekbar.setEnabled(true);
//        else
//            mRedSeekBar.setEnabled(false);
        if (isChecked) {
            if (mPWM < 50)
                mPWM /= 2;
            try {
                manageConnectedSocket.write(PWM_CHANGE);
                manageConnectedSocket.write(mLED);
                manageConnectedSocket.write(mPWM);
                Toast.makeText(getApplicationContext(), "Turn On LED", Toast.LENGTH_SHORT).show();
                buttonView.setChecked(true);
            } catch (Exception e) {
                buttonView.setChecked(false);
                Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                Log.i("blad wysylania", e.getMessage());
            }
        } else
            manageConnectedSocket.write(mLED);

//        //TODO: wyslac info o zmianie switcha
//        if (isChecked && (mPWM != 0))
//        {
//            if (mPWM < 50)
//                mPWM /= 2;
//            try
//            {
//                manageConnectedSocket.write(PWM_CHANGE);
//                manageConnectedSocket.write(mLED);
//                manageConnectedSocket.write(mPWM);
//                Toast.makeText(getApplicationContext(), "Turn On Green LED", Toast.LENGTH_SHORT).show();
//                buttonView.setChecked(true);
//            } catch (Exception e)
//            {
//                buttonView.setChecked(false);
//                Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
//                Log.i("blad wysylania", e.getMessage());
//            }
//        } else if (mPWM > 0 || !isChecked)
//        {
//            try
//            {
//                manageConnectedSocket.write(mLED);
//                Toast.makeText(getApplicationContext(), "Turn Off Green LED", Toast.LENGTH_SHORT).show();
//                buttonView.setChecked(false);
//            } catch (Exception e)
//            {
//                buttonView.setChecked(true);
//                Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
//                Log.i("blad wysylania", e.getMessage());
//            }
//        } else
//        {
//            buttonView.setChecked(false);
//        }
//        buttonView.setEnabled(true);

    }
    private void LED_Switch(int mLED, int mPWM)
    {
        if (mPWM < 50)
            mPWM /= 2;
        try {
            manageConnectedSocket.write(PWM_CHANGE);
            manageConnectedSocket.write(mLED);
            manageConnectedSocket.write(mPWM);
            Toast.makeText(getApplicationContext(), "Turn On LED", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
            Log.i("blad wysylania", e.getMessage());
        }
    }

    private void switchOffLEDs(int mLED) {
        manageConnectedSocket.write(mLED);
    }

    public void list(View view) {
        pairedDevices = mBA.getBondedDevices();
        btWyszukaj.setEnabled(false);
        pbLista.setVisibility(View.VISIBLE);
        mArrayAdapter.clear();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices)
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            Toast.makeText(getApplicationContext(), "Showing Paired Devices",
                    Toast.LENGTH_SHORT).show();
            lv.setAdapter(mArrayAdapter);
        }
        if (!mBA.isDiscovering())
            if (mBA.startDiscovery() == true)
                Toast.makeText(getApplicationContext(), "Discovering Devices", Toast.LENGTH_LONG).show();

    }

    final private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(getApplicationContext(), "FINISHED", Toast.LENGTH_LONG).show();
                btWyszukaj.setEnabled(true);
                pbLista.setVisibility(View.INVISIBLE);

            }
        }
    };

    private class BluetoothClient extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        public BluetoothClient(BluetoothDevice device) {
            mmDevice = device;
            try {
                mmSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
            } catch (Exception e) {
            }
        }

        public void run() {
            mBA.cancelDiscovery();
            try {
                mmSocket.connect();
                Toast.makeText(getApplicationContext(), "Conndected to: " + mmDevice.getName(), Toast.LENGTH_LONG).show();

                disablePatternButtons(false);   //
                disableSeekBars(false);         // ENABLE ALL
                disableSwitches(false);         //

                Log.i("Log", "za mmSocket.connect");
            } catch (Exception e) {
                Log.i("Log", "exception mmSocket.connect: " + e.getMessage());
                try {
                    mmSocket.close();
                } catch (Exception el) {
                }
            }
            manageConnectedSocket = new ConnectedThread(mmSocket);
            manageConnectedSocket.start();
            manageConnectedSocket.write(RESET);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (Exception e) {
            }
        }

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[20];  // buffer store for the stream
            int bytes; // bytes returned from read()
            String string = "";
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                } catch (IOException e) {
                    break;
                }
                try {
                    string = new String(buffer);
                } catch (Exception e1) {
                    Log.i("B³¹d danych wej", buffer.toString());
                }
                Log.i("STM respond", bytes + " bajtow: " + "--" + string);
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(int message) { //TODO:
            try {
                mmOutStream.write(message);
            } catch (IOException e) {
                //  Log.i(TAG , e.getMessage());
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                // Log.i(TAG , e.getMessage());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
    protected void onResume() {
        super.onResume();
        if (!mBA.isEnabled()) // wlacza BT przy starcie aplikacji
        {
            mBA.enable();
            Toast.makeText(getApplicationContext(), "Bluetooth Trun ON", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth Already ON", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBA != null) {
            mBA.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
        manageConnectedSocket.cancel();
        mBA.disable();
    }
}
