
// Copyright (c) 2016 Jianlin Hao. All rights reserved.
// Licensed under the Apache License Version 2.0. See LICENSE file in the project root for full license information.
// https://github.com/rabbitom

package net.erabbit.blesensor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.erabbit.ble.BleDevice;
import net.erabbit.ble.BleDevicesManager;
import net.erabbit.ble.BleSearchReceiver;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener {

    BleSearchReceiver searchReceiver = new BleSearchReceiver() {
        @Override
        public void onSearchStarted() {
            devices.clear();
            deviceAdapter.notifyDataSetChanged();
            progressDlg = ProgressDialog.show(MainActivity.this,
                    getString(R.string.scanning_title), //title
                    getString(R.string.scanning_msg), //msg
                    true, //indeterminate
                    true, //cancelable
                    new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface arg0) {
                            bleDevicesManager.stopSearch();
                            progressDlg.dismiss();
                        }
                    });
        }

        @Override
        public void onFoundDevice(String deviceID, int rssi, Map<Integer, byte[]> data, String deviceType) {
            BleDevice bleDevice = bleDevicesManager.findDevice(deviceID);
            if(bleDevice == null)
                bleDevice = bleDevicesManager.createDevice(deviceID, MainActivity.this, DialogIoTSensor.class, null);
            devices.add(bleDevice);
            deviceRSSIs.put(deviceID, rssi);
            deviceAdapter.notifyDataSetChanged();
        }

        @Override
        public void onRSSIUpdated(String deviceID, int rssi) {
            deviceRSSIs.put(deviceID, rssi);
        }

        @Override
        public void onSearchTimeOut() {
            progressDlg.dismiss();
        }
    };

    protected class DeviceAdapter extends ArrayAdapter<BleDevice> {
        int layout_res;

        DeviceAdapter(Context context, int resource, ArrayList<BleDevice> devicesList) {
            super(context, resource, devicesList);
            layout_res = resource;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View cell = convertView;
            if(cell == null) {
                LayoutInflater inflater = getLayoutInflater();
                cell = inflater.inflate(layout_res, parent, false);
            }
            TextView nameLabel = (TextView)cell.findViewById(R.id.nameText);
            TextView addressLabel = (TextView)cell.findViewById(R.id.addrText);
            TextView signalLabel = (TextView)cell.findViewById(R.id.rssiText);
            BleDevice device = getItem(position);
            nameLabel.setText(device.getDeviceName());
            addressLabel.setText(device.getDeviceKey());
            if(deviceRSSIs.containsKey(device.getDeviceKey()))
                signalLabel.setText(getString(R.string.rssi_format, deviceRSSIs.get(device.getDeviceKey())));
            return cell;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        bleDevicesManager.setCurDevice(devices.get(position));
        startActivity(new Intent(this, IoTSensorActivity.class));
    }

    protected ArrayList<BleDevice> devices = new ArrayList<>();
    protected DeviceAdapter deviceAdapter;
    protected ListView deviceList;

    protected Map<String,Integer> deviceRSSIs = new TreeMap<>();

    protected ProgressDialog progressDlg;

    BleDevicesManager bleDevicesManager ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bleDevicesManager = BleDevicesManager.getInstance(this);
        deviceList = (ListView)findViewById(R.id.deviceList);
        deviceAdapter = new DeviceAdapter(this, R.layout.device_list_row, devices);
        deviceList.setAdapter(deviceAdapter);
        deviceList.setOnItemClickListener(this);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        searchReceiver.registerReceiver(lbm);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                    PackageManager pm = getPackageManager();
                    try {
                        PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
                        String info = String.format("%s\n%s\n%s:%s",
                                getString(R.string.copyright),
                                getString(R.string.vendor_name),
                                getString(R.string.version),
                                pi.versionName);
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(getResources().getString(R.string.app_name))
                                .setMessage(info)
                                .setPositiveButton(R.string.ok_btn, null)
                                .show();
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.i("OptionsItem", e.toString());
                    }
                }
                break;
            case R.id.action_scan:
                bleDevicesManager.startSearch(this);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

}
