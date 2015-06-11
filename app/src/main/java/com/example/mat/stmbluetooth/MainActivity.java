package com.example.mat.stmbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

    private Button btTurnOn,mBtPattern1, mBtPattern2, mBtPattern3,btZatrzymaj, btWznow,btWyszukaj;

    private Switch mRedSwitch, mGreenSwitch, mBlueSwitch, mOrangeSwitch;
    private SeekBar mRedSeekBar, mGreenSeekBar, mBlueSeekBar, mOrangeSeekBar, mPatternSeekBar;

    private EditText etTest;
    private BluetoothAdapter mBA;
    private Set<BluetoothDevice> pairedDevices;
    private ListView lv;
    private ArrayList list;
    private ArrayAdapter mArrayAdapter;
    private ProgressBar pbLista;
    public ConnectedThread manageConnectedSocket;

    private final int GREEN_LED = 1;
    private final int ORANGE_LED = 2;
    private final int RED_LED = 3;
    private final int BLUE_LED = 4;

    private final int PWM_CHANGE = 5; //ktora dioda? % wypelnienia? 0-100
    private final int PATTERN_1 = 6; //predkosc dzialania wzoru? 0-100
    private final int PATTERN_2 = 7; //j.w
    private final int PATTERN_3 = 8; //j.w
    private final int PATTERN_FINISH = 9; //zatrzymuje wykonywanie patternu
    private final int PATTERN_RESUME = 10; //wznawia wykonywanie patternu

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBA = BluetoothAdapter.getDefaultAdapter();
        pbLista = (ProgressBar) findViewById(R.id.progressBar);


        // ----BUTTON INIT ---
        mBtPattern1 = (Button) findViewById(R.id.pattern1);
        mBtPattern2 = (Button) findViewById(R.id.pattern2);
        mBtPattern3 = (Button) findViewById(R.id.pattern3);
        btWyszukaj = (Button) findViewById(R.id.btWyszukaj);
        btWznow = (Button) findViewById(R.id.btWznow);
        btZatrzymaj = (Button) findViewById(R.id.btZatrzymaj);
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


        pbLista.setVisibility(View.INVISIBLE);


        list = new ArrayList();
        lv = (ListView)findViewById(R.id.listView);
        mArrayAdapter = new ArrayAdapter
                (this, android.R.layout.simple_list_item_1);
        lv.setAdapter(mArrayAdapter);
        //CONNECT TO DEFAULT MAC

        mBtPattern1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //TODO: wysh�a� 1!
                int pattern_pwm = mPatternSeekBar.getProgress();
//                if(pattern_pwm < 50)
//                    pattern_pwm /= 2;
                manageConnectedSocket.write(PATTERN_1);
                manageConnectedSocket.write(mPatternSeekBar.getProgress());

                patternOnDisableRest(true);
            }
        });
        mBtPattern2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manageConnectedSocket.write(PATTERN_2);
                manageConnectedSocket.write(mPatternSeekBar.getProgress());
                patternOnDisableRest(true);
            }
        });
        mBtPattern3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manageConnectedSocket.write(PATTERN_3);
                manageConnectedSocket.write(mPatternSeekBar.getProgress());
                patternOnDisableRest(true);
            }
        });
        btZatrzymaj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manageConnectedSocket.write(PATTERN_FINISH);
                patternOnDisableRest(false);
            }
        });
        btWznow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manageConnectedSocket.write(PATTERN_RESUME);
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

        mRedSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LED_Switch(buttonView, isChecked, RED_LED, mRedSeekBar.getProgress());
            }
        });
        mGreenSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LED_Switch(buttonView, isChecked, GREEN_LED, mGreenSeekBar.getProgress());
            }
        });
        mOrangeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LED_Switch(buttonView, isChecked, ORANGE_LED, mOrangeSeekBar.getProgress());
            }
        });
        mBlueSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LED_Switch(buttonView, isChecked, BLUE_LED, mBlueSeekBar.getProgress());
            }
        });


        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        if(!mBA.isEnabled()) // wlacza BT przy starcie aplikacji
        {
            mBA.enable();
        }
        BluetoothDevice dev = mBA.getRemoteDevice((String)
                getResources().getText(R.string.default_mac)); //automatycznie laczy sie z domyslnym urzadzeniem
        new BluetoothClient(dev).run();

    }

    private void patternOnDisableRest(boolean enableAll) {
        enableAll = !enableAll;
        mRedSwitch.setEnabled(enableAll);
        mRedSeekBar.setEnabled(enableAll);

        mGreenSwitch.setEnabled(enableAll);
        mGreenSeekBar.setEnabled(enableAll);

        mBlueSwitch.setEnabled(enableAll);
        mBlueSeekBar.setEnabled(enableAll);

        mOrangeSwitch.setEnabled(enableAll);
        mOrangeSeekBar.setEnabled(enableAll);
    }

    private void LED_Switch(CompoundButton buttonView, boolean isChecked, int mLED, int mPWM) {
        //TODO: wyslac info o zmianie switcha
        if(isChecked && (mPWM != 0)) {
            if(mPWM < 50)
                mPWM /= 2;
            try {
                manageConnectedSocket.write(PWM_CHANGE);
                manageConnectedSocket.write(mLED);
                manageConnectedSocket.write(mPWM);
                Toast.makeText(getApplicationContext(), "Turn On Green LED", Toast.LENGTH_SHORT).show();
                buttonView.setChecked(true);
            } catch (Exception e) {
                buttonView.setChecked(false);
                Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                Log.i("blad wysylania", e.getMessage());
            }
        }else if(mPWM >0 || !isChecked){
            try {
                manageConnectedSocket.write(mLED);
                Toast.makeText(getApplicationContext(), "Turn Off Green LED", Toast.LENGTH_SHORT).show();
                buttonView.setChecked(false);
            }catch (Exception e){
                buttonView.setChecked(true);
                Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                Log.i("blad wysylania", e.getMessage());
            }
        } else {
            buttonView.setChecked(false);
        }
        buttonView.setEnabled(true);
    }
    public void list(View view) {
        pairedDevices = mBA.getBondedDevices();

        mArrayAdapter.clear();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices)
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            Toast.makeText(getApplicationContext(), "Showing Paired Devices",
                    Toast.LENGTH_SHORT).show();
            lv.setAdapter(mArrayAdapter);
        }
        if(!mBA.isDiscovering())
            if(mBA.startDiscovery() == true)
                Toast.makeText(getApplicationContext(),"Discovering Devices", Toast.LENGTH_LONG).show();

    }
    final private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            btWyszukaj.setEnabled(false);
            pbLista.setVisibility(View.VISIBLE);
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Toast.makeText(getApplicationContext(),"FINISHED", Toast.LENGTH_LONG).show();
                btWyszukaj.setEnabled(true);
                pbLista.setVisibility(View.INVISIBLE);

            }
        }
    };

    private class BluetoothClient extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        public BluetoothClient(BluetoothDevice device){
            mmDevice = device;
            try{
                mmSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
            }
            catch (Exception e){ }
        }
        public void run() {
            mBA.cancelDiscovery();
            try {
                mmSocket.connect();
                Toast.makeText(getApplicationContext(), "Conndected to: " + mmDevice.getName(),Toast.LENGTH_LONG).show();
                Log.i("Log", "za mmSocket.connect");
            }catch (Exception e){
                Log.i("Log", "exception mmSocket.connect: "+e.getMessage());
                try{
                    mmSocket.close();
                }catch (Exception el) {}
            }
            manageConnectedSocket = new ConnectedThread(mmSocket);
            manageConnectedSocket.start();
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (Exception e) { }
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
            } catch (IOException e) { }

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
                   string = new String(buffer,"UTF-8");
                }catch (Exception e1){Log.i("B��d danych wej", buffer.toString());}
                Log.i("STM respond", bytes + " bajtow: " + buffer.toString()+"--" + string);
            }
        }
        /* Call this from the main activity to send data to the remote device */
        public void write(int message) { //TODO:
            try {
                mmOutStream.write(message);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
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
    protected void onDestroy(){
        super.onDestroy();
        if(mBA != null){
            mBA.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
        manageConnectedSocket.cancel();
        mBA.disable();
    }
}
