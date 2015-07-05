package com.example.mat.stmbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * ***************************************************************************************
 *
 * @author Mateusz Linke, Politechnika Poznañska - Informatyka, Wydzia³ Elektryczny
 * @version v1.0
 * @file MainActivity.java
 * @brief Podstawowa klasa aplikacji, uruchamiana w momencie uruchamiania aplikacji.
 * ***************************************************************************************
 */
public class MainActivity extends ActionBarActivity {

    private Button btWyszukaj, btReset; //Przyciski aplikacji
    private ToggleButton mBtPattern1, mBtPattern2, mBtPattern3; //Przyciski aplickaji (ToggleButton)
    private Switch mRedSwitch, mGreenSwitch, mBlueSwitch, mOrangeSwitch; //Prze³¹czniki od poszczególnych LED
    private SeekBar mRedSeekBar, mGreenSeekBar, mBlueSeekBar, mOrangeSeekBar, mPatternSeekBar; //Suwaki od poszczególnych LED'ow oraz wzoru
    private BluetoothAdapter mBA;
    private Set<BluetoothDevice> pairedDevices;
    private ListView lv;
    private ArrayAdapter mArrayAdapter;
    private ProgressBar pbLista;
    private ConnectedThread manageConnectedSocket;

    private boolean pattern_holded1 = false;
    private boolean pattern_holded2 = false;
    private boolean pattern_holded3 = false;

    private final int GREEN_LED = 1; /// Sta³a oznaczaj¹ca diode zielona
    private final int ORANGE_LED = 2;   //  Sta³a oznaczaj¹ca diode pomaranczowa
    private final int RED_LED = 3;  //  Sta³a oznaczaj¹ca diode czerwona
    private final int BLUE_LED = 4; //  Sta³a oznaczaj¹ca diode niebieska
    private final int PWM_CHANGE = 5;   //  Sta³a oznaczaj¹ca w³¹czenie kontroli PWM
    private final int PATTERN_1 = 6;    //  Sta³a oznaczaj¹ca w³¹czenie wzoru 1
    private final int PATTERN_2 = 7;    //  Sta³a oznaczaj¹ca w³¹czenie wzoru 2
    private final int PATTERN_3 = 8;    //  Sta³a oznaczaj¹ca w³¹czenie wzoru 3
    private final int PATTERN_HOLD = 9; //  Sta³a oznaczaj¹ca wstrzymanie wzoru
    private final int PATTERN_RESUME = 10;  // Sta³a oznaczaj¹ca wznowienie wzoru
    private final int RESET = 11;   //  Sta³a oznaczaj¹ca reset modu³u STM
    private final String TAG = "STMBluetooth: ";    //  Sta³a pomagaj¹ca w debugowaniu


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBA = BluetoothAdapter.getDefaultAdapter();
        initialize_Interface();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        // ------ INIT LISTA DOSTEPNYCH URZADZEN----
        lv = (ListView) findViewById(R.id.listView);
        mArrayAdapter = new ArrayAdapter
                (this, android.R.layout.simple_list_item_1);
        lv.setAdapter(mArrayAdapter);
        setPairedDevices(); // pokazuje na liscie sprarowane urzadzenia
        //------INIT LISTA DOSTEPNYCH URZADZEN--
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            mRedSwitch.setText("Red");
            mOrangeSwitch.setText("Orange");
            mBlueSwitch.setText("Blue");
            mGreenSwitch.setText("Green");
        }

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
        mBtPattern1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean on = ((ToggleButton) view).isChecked();
                pattern_holded2 = false;
                pattern_holded3 = false;
                if (on) //wlaczony
                {
                    if (pattern_holded1)
                        manageConnectedSocket.write(PATTERN_RESUME);
                    else
                        turnOnPattern1();
                    pattern_holded1 = true;
                } else {
                    manageConnectedSocket.write(PATTERN_HOLD);
                }
            }
        });
        mBtPattern2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean on = ((ToggleButton) view).isChecked();;
                pattern_holded1 = false;
                pattern_holded3 = false;
                if (on) //wlaczony
                {
                    if(pattern_holded2)
                        manageConnectedSocket.write(PATTERN_RESUME);
                    else
                        turnOnPattern2();
                    pattern_holded2 = true;
                } else {
                    manageConnectedSocket.write(PATTERN_HOLD);
                }
            }
        });
        mBtPattern3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean on = ((ToggleButton) view).isChecked();
                pattern_holded1 = false;
                pattern_holded2 = false;
                if (on) //wlaczony
                {
                    if(pattern_holded3)
                        manageConnectedSocket.write(PATTERN_RESUME);
                    else
                        turnOnPattern3();
                    pattern_holded3 = true;
                } else {
                    manageConnectedSocket.write(PATTERN_HOLD);
                }
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
                LED_Switch(buttonView, mRedSeekBar, isChecked, RED_LED, mRedSeekBar.getProgress());
            }
        });
        mGreenSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LED_Switch(buttonView, mGreenSeekBar, isChecked, GREEN_LED, mGreenSeekBar.getProgress());
            }
        });
        mOrangeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LED_Switch(buttonView, mOrangeSeekBar, isChecked, ORANGE_LED, mOrangeSeekBar.getProgress());
            }
        });
        mBlueSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LED_Switch(buttonView, mBlueSeekBar, isChecked, BLUE_LED, mBlueSeekBar.getProgress());
            }
        });

    }

    private void initialize_Interface() {
        pbLista = (ProgressBar) findViewById(R.id.progressBar);
        pbLista.setVisibility(View.INVISIBLE); //wylacza progress bar wyszukiwania urzadzen

        // ----BUTTON INIT ---
        mBtPattern1 = (ToggleButton) findViewById(R.id.pattern1);
        mBtPattern2 = (ToggleButton) findViewById(R.id.pattern2);
        mBtPattern3 = (ToggleButton) findViewById(R.id.pattern3);

        btWyszukaj = (Button) findViewById(R.id.btWyszukaj);
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
        btReset.setEnabled(false);
        //---/DISABLE ALL BUTTON TILL CONNECT TO DEVICE/
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
        mBtPattern1.setEnabled(enableAll);
        mBtPattern2.setEnabled(enableAll);
        mBtPattern3.setEnabled(enableAll);
        mPatternSeekBar.setEnabled(enableAll);
    }

    private void LED_Switch(CompoundButton buttonView, SeekBar seekbar, boolean isChecked, int mLED, int mPWM) {
        disablePatternButtons(true);
        if (isChecked)
            seekbar.setEnabled(true);
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
            switchOffLED(mLED);
    }

    private void LED_Switch(int mLED, int mPWM) {
        disablePatternButtons(true);
        if (mPWM < 50)
            mPWM /= 2;
        try {
            manageConnectedSocket.write(PWM_CHANGE);
            manageConnectedSocket.write(mLED);
            manageConnectedSocket.write(mPWM);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
            Log.i("blad wysylania", e.getMessage());
        }
    }

    /**
     * @param mLED numer diody ktory ma zostac wylaczony
     * @brief funkcja switchOffLEDs(int)
     * wysyla zadanie wylaczenia konkretnej diody
     */
    private void switchOffLED(int mLED) {
        disablePatternButtons(true);
        manageConnectedSocket.write(mLED);
    }

    public void setPairedDevices(){
        pairedDevices = mBA.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices)
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
          //  Toast.makeText(getApplicationContext(), "Showing Paired Devices",
          //          Toast.LENGTH_SHORT).show();
            lv.setAdapter(mArrayAdapter);
        }
    }
    public void list(View view) {
        btWyszukaj.setEnabled(false);
        pbLista.setVisibility(View.VISIBLE);
        mArrayAdapter.clear();
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

    /**
     * @class BluetoothClient
     * @brief Klasa odpowiedzialna za nawiazanie polaczenie z zadanym urzadzeniem
     */
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
                btReset.setEnabled(true);       //

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

    /**
     * @class ConnectedThread(BluetoothSocket socket)
     * Klasa odpowiedzialna za przesylanie i odbieranie danych przeslanych przez bluetooth
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        /**
         * @param socket gniazdo do polaczonego urzedzenia
         * @brief konstruktor klasy
         */
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        /**
         * @brief funkcja run()
         * funkcja nasluchuje przychodzacych danych
         */
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
                    Log.i("Blad danych wej", buffer.toString());
                }
                Log.i("STM respond", bytes + " bajtow: " + "--" + string);
            }
        }

        /**
         * @param message przechowuje wiadomosc ktora ma zostac wyslana
         * @brief write(int)
         * Wysyla dane do podlaczonego urzadzenia
         */
        public void write(int message) {
            try {
                mmOutStream.write(message);
            } catch (IOException e) {
                Log.i(TAG, e.getMessage());
            }
        }

        /* Call this from the main activity to shutdown the connection */

        /**
         * @brief cancel()
         * Konczy polaczenie
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.i(TAG, e.getMessage());
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

    /**
     * @brief onResume()
     * Funkcja wlacza bluetooth w momencie uruchomienia aplikacji
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mBA != null) {
            if (!mBA.isEnabled()) // wlacza BT przy starcie aplikacji
            {
                mBA.enable();
                Toast.makeText(getApplicationContext(), "Bluetooth Turn ON", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(getApplicationContext(), "Bluetooth Already ON", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * @brief onDestroy()
     * Wywo³ywana w momencie wcisniecia przycisku "cofnij". Wyl¹cza Bluetooth i zamyka aplikacje.
     */
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
