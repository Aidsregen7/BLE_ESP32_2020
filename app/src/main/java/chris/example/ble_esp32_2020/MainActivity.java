package chris.example.ble_esp32_2020;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Button bScan;
    private ListView lvDevices;
    private ArrayList<String> discoveredDevices = new ArrayList<String>();
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothManager bluetoothManager;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {
            Toast.makeText(this, R.string.BT_not_available, Toast.LENGTH_SHORT).show();
            finish();
        }

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.BLE_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }

        init();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(!bluetoothAdapter.isEnabled())
        {
            Intent turnBTOn =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTOn, 1);
        }
    }

    public void onClickScan (View v) {
        if(!isScanning) {
            bluetoothLeScanner.startScan(scanCallback);
            isScanning = true;
        }
        if(isScanning) {
            bluetoothLeScanner.stopScan(scanCallback);
            isScanning = false;
        }
    }

    private void init() {
        bScan = findViewById(R.id.bScan);
        lvDevices = findViewById(R.id.lvDevices);
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String deviceInfo = result.getDevice().getName() + "\n" + result.getDevice().getAddress();
            Log.i(TAG, "Device found: " + deviceInfo);

            //gefundenes Gerät der Liste hinzufügen, wenn es noch nicht aufgeführt ist
            if(!discoveredDevices.contains(deviceInfo)) {
                discoveredDevices.add(deviceInfo);
            }
            final ArrayAdapter adapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, discoveredDevices);
            lvDevices.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    };
}