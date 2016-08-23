package com.kongtech.plutocon.template.scanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.kongtech.plutocon.sdk.Plutocon;
import com.kongtech.plutocon.sdk.PlutoconManager;

import java.util.ArrayList;
import java.util.List;

public class TemplateFragment extends Fragment {

    private List<Plutocon> plutoconList;
    private PlutoconAdpater plutoconAdpater;

    private PlutoconManager plutoconManager;

    public static Fragment newInstance(Context context) {
        TemplateFragment f = new TemplateFragment();
        return f;
    }


    @Override
    public void onResume() {
        super.onResume();
        if(checkPermission()) {
            plutoconManager.connectService(new PlutoconManager.OnReadyServiceListener() {
                @Override
                public void onReady() {
                    startMonitoring();
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        plutoconManager.close();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_template, null);

        plutoconAdpater = new PlutoconAdpater();

        ListView listView = (ListView) view.findViewById(R.id.list);
        listView.setAdapter(plutoconAdpater);

        plutoconManager = new PlutoconManager(getContext());

        plutoconList = new ArrayList<>();

        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.root);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                plutoconManager.getMonitoringResult().clear();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                },300);
            }
        });

        return view;
    }

    private void startMonitoring() {
        plutoconManager.startMonitoring(PlutoconManager.MONITORING_FOREGROUND, new PlutoconManager.OnMonitoringPlutoconListener() {
            @Override
            public void onPlutoconDiscovered(Plutocon plutocon, final List<Plutocon> plutocons) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        plutoconList.clear();
                        plutoconList.addAll(plutocons);
                        plutoconAdpater.refresh();
                    }
                });
            }
        });
    }

    private class PlutoconAdpater extends BaseAdapter {

        @Override
        public int getCount() {
            return plutoconList == null ? 0 : plutoconList.size();
        }

        @Override
        public Object getItem(int position) {
            return plutoconList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.item_plutocon, parent, false);
            }

            Plutocon plutocon = plutoconList.get(position);

            TextView tvName = (TextView) convertView.findViewById(R.id.deviceName);
            TextView tvAddress = (TextView) convertView.findViewById(R.id.deviceAddress);
            TextView tvRSSI = (TextView) convertView.findViewById(R.id.deviceRSSI);
            TextView tvInterval = (TextView) convertView.findViewById(R.id.deviceInterval);
            TextView tvUuid = (TextView) convertView.findViewById(R.id.tvUuid);
            TextView tvMajor = (TextView) convertView.findViewById(R.id.tvMajor);
            TextView tvMinor = (TextView) convertView.findViewById(R.id.tvMinor);
            TextView tvLatitude = (TextView) convertView.findViewById(R.id.tvLatitude);
            TextView tvLongitude = (TextView) convertView.findViewById(R.id.tvLongitude);

            tvName.setText(plutocon.getName());
            tvAddress.setText(plutocon.getMacAddress());
            tvRSSI.setText(plutocon.getRssi() + "dBm");
            tvInterval.setText(plutocon.getInterval() + "ms");
            tvUuid.setText(plutocon.getUuid().toString());
            tvMajor.setText(plutocon.getMajor() + "");
            tvMinor.setText(plutocon.getMinor() + "");
            tvLatitude.setText(plutocon.getLatitude() + "");
            tvLongitude.setText(plutocon.getLongitude() + "");

            return convertView;
        }

        private void refresh(){
            notifyDataSetChanged();
        }
    }

    private boolean checkPermission(){
        BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        if((mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())){
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return false;
        }


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return false;
            }

            LocationManager lm = (LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);
            boolean gps_enabled = false;
            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch(Exception ex) {}

            if(!gps_enabled){
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                return false;
            }
        }
        return true;
    }
}
