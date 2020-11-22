package chris.example.ble_esp32_2020;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Address;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Button bScan, bEmpfangen, bVerbinden;
    private ListView lvDevices;
    private ArrayList<String> discoveredDevices = new ArrayList<String>();
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothManager bluetoothManager;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothLeService bluetoothLeService;
    private boolean isScanning = false, isConnected = false;
    private String deviceAddress;
    private ArrayAdapter adapter;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.BT_not_available, Toast.LENGTH_SHORT).show();
            finish();
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.BLE_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        init();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!bluetoothAdapter.isEnabled()) {
            Intent turnBTOn =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTOn, 1);
        }
        if (adapter != null)
            adapter.clear();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bluetoothLeService != null && isConnected) {
            final boolean result = bluetoothLeService.connect(deviceAddress);
            Log.d(TAG, "Connect request result: " + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Aufräumen
        bluetoothLeScanner.stopScan(scanCallback);
        bluetoothLeService.disconnect();
        unbindService(serviceConnection);
        bluetoothLeService = null;
    }

    public void onClickScan(View v) {
        /*if (bluetoothLeScanner != null) {
            bluetoothLeScanner.startScan(null, scanSettings, scanCallback);
            Log.d(TAG, "Scan gestartet: " + bluetoothLeScanner);
        } else
            Log.d(TAG, "Kein Scanner Objekt!");*/
        scanLeDevice(true);

    }

    public void onClickEmpfangen(View v) {

    }

    public void onClickVerbinden(View v) {
        if (deviceAddress != null && !isConnected) {
            bluetoothLeService.connect(deviceAddress);
            isConnected = true;
            bVerbinden.setText(R.string.Trennen);
        } else if (deviceAddress == null)
            Log.d(TAG, "deviceAddress ist null!");
        else if (isConnected) {
            bluetoothLeService.disconnect();
            isConnected = false;
            bVerbinden.setText(R.string.Verbinden);
        }
    }

    private void init() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bScan = findViewById(R.id.bScan);
        bVerbinden = findViewById(R.id.bVerbinden);
        bEmpfangen = findViewById(R.id.bEmpfangen);
        lvDevices = findViewById(R.id.lvDevices);
        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Object o = lvDevices.getItemAtPosition(position);
                deviceAddress = o.toString();
                Log.d(TAG, "Ausgewählt: " + deviceAddress);
            }
        });
        adapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, discoveredDevices);
        lvDevices.setAdapter(adapter);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "Es wird gescannt");
            super.onScanResult(callbackType, result);
            runOnUiThread(() -> {
                String deviceInfo = result.getDevice().getName() + "\n" + result.getDevice().getAddress();
                Log.i(TAG, "Device found: " + deviceInfo);

                //gefundenes Gerät der Liste hinzufügen, wenn es noch nicht aufgeführt ist
                if (!discoveredDevices.contains(deviceInfo)) {
                    discoveredDevices.add(deviceInfo);
                }
                adapter.notifyDataSetChanged();
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {

        }

        @Override
        public void onScanFailed(int errorCode) {

        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!bluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
        }
    };

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i(TAG, "connected");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i(TAG, "disconnected");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.i(TAG, "services discovered");

                // alle Services und Characteristics im Log ausgeben
                for (BluetoothGattService gattService : bluetoothLeService.getSupportedGattServices()) {
                    Log.i(TAG, "Gatt Service: " + gattService.getUuid().toString());
                    for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                        Log.i(TAG, "Gatt Characteristic: " + gattCharacteristic.getUuid().toString());
                    }
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.i(TAG, intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    ScanSettings scanSettings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(0L)
            .build();

    /**
     * Ab hier LeScanCallback und ScanCallback von dem alten Skript
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(() -> {
                isScanning = false;
                bluetoothLeScanner.stopScan(mScanCallback);
                Log.w(TAG, "scanLeDevice: " + discoveredDevices);
                Log.d(TAG, "scanLeDevice: Scannen wird gestoppt...");
            }, 15000);
            isScanning = true;
            bluetoothLeScanner.startScan(mScanCallback);
            Log.d(TAG, "scanLeDevice: Scan wird gestartet...");
        } else {
            isScanning = false;
            bluetoothLeScanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            runOnUiThread(() -> {
                Log.d(TAG, "Device found: " + result.getDevice().getAddress());
                if (!discoveredDevices.contains(result.getDevice().getAddress()))
                    discoveredDevices.add(result.getDevice().getAddress());
                adapter.notifyDataSetChanged();
            });
        }
    };
}