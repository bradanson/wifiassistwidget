package antsinapps.WifiAssistWidget;

import android.Manifest;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.HashSet;
import java.util.Set;

public class ConfigureActivity extends AppCompatActivity {

    String username;
    String password;
    String ssid;
    BroadcastReceiver srr;
    boolean srrRegistered;
    static final int LOCATION_REQUEST_CODE = 1000;
    static final int GOOD_RESULT = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        srrRegistered = false;
        int widgetID = 0;
        if (extras != null) {
            widgetID = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        setResult(RESULT_OK, resultValue);

        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            displayLocationSettingsRequest(this);
            return;
        }
        onActivityResult(LOCATION_REQUEST_CODE, GOOD_RESULT, new Intent());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        //Log.d("onActivityResult", "requestCode: " + requestCode + ", resultCode: " + resultCode);

        if(requestCode == LOCATION_REQUEST_CODE && resultCode == GOOD_RESULT){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
            {
                // Log.d("ConfigureActivity", "Requesting Permissions..");
                ActivityCompat.requestPermissions(ConfigureActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0x2345);
            }else{
                // Log.d("ConfigureActivity", "Permissions Request Not Necessary");
                // Either app already granted permissions or device is older than Marshmallow
                // Enable WIFI.. Scan.. Prompt User
                handleWifiScan();
            }
        }else{
            showUserInfoDialog("");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == 0x2345) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    showUserInfoDialog("");
                    return;
                }
            }

            // Enable WIFI.. Scan.. Prompt User
            handleWifiScan();
        }
    }

    private void handleWifiScan() {
        // Enable WIFI
        final WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()){
            //Log.d("WifiCheck", "WIFI NOT ENABLED");
            wifi.setWifiEnabled(true);
            Toast.makeText(getApplicationContext(), getText(R.string.toast_enabling_wifi), Toast.LENGTH_LONG).show();
        }

        wifi.startScan();

        // Wait for scan to finish, receiver will catch call
        final Context c = this;
        srr = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                srrRegistered = false;
                c.unregisterReceiver(srr);
                // Log.d("srrOnReceive", "Scan results intent complete:" + intent.getAction());
                // Prune for duplicates in scanned list
                Set<String> hs = new HashSet<>();
                for(ScanResult i : wifi.getScanResults()){
                    //Log.d("ScanResult", i.SSID);
                    hs.add(i.SSID);
                }

                // Show SSID Dialog, then User Info Dialog
                showDialogs(hs);
            }
        };
        this.registerReceiver(srr, new IntentFilter("android.net.wifi.SCAN_RESULTS"));
        srrRegistered = true;
    }

    private void showDialogs(Set<String> hs) {
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                ConfigureActivity.this,
                android.R.layout.select_dialog_singlechoice);
        //Log.d("ShowDialogs" , "Called");

        // Add individual entries
        for(String j : hs){
            arrayAdapter.add(j);
        }

        if (hs.size() == 0) {
            Toast.makeText(getApplicationContext(), getText(R.string.toast_no_networks), Toast.LENGTH_LONG).show();
            showUserInfoDialog("");
        }else {
            //Log.d("wifiSelectBuilder", "Called");
            AlertDialog.Builder wifiSelectBuilder = new AlertDialog.Builder(this);
            wifiSelectBuilder.setTitle(getResources().getString(R.string.wifiDialogTitle));
            wifiSelectBuilder.setNegativeButton(
                    "cancel",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    });

            wifiSelectBuilder.setAdapter(
                    arrayAdapter,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String selectedSSID = arrayAdapter.getItem(which);

                            // Dialog for Username/password
                            showUserInfoDialog(selectedSSID);

                        }
                    });
            wifiSelectBuilder.setCancelable(false);
            wifiSelectBuilder.show();

        }
    }

    private void showUserInfoDialog(String selectedSSID) {
        //Log.d("ShowUserInfoDialog", "Called");
        ViewGroup contentView = (ViewGroup) findViewById(android.R.id.content);
        AlertDialog.Builder userInfoBuilder = new AlertDialog.Builder(this);
        userInfoBuilder.setTitle(getResources().getString(R.string.userDialogTitle));
        View userInfoViewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_input_layout, contentView, false);
        final EditText ssidInput = (EditText) userInfoViewInflated.findViewById(R.id.ssidInput);
        final EditText usernameInput = (EditText) userInfoViewInflated.findViewById(R.id.usernameInput);
        final EditText passwordInput = (EditText) userInfoViewInflated.findViewById(R.id.passwordInput);
        userInfoBuilder.setView(userInfoViewInflated);
        ssidInput.setText(selectedSSID);

        final Context c = this;


        userInfoBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ssid = ssidInput.getText().toString();
                username = usernameInput.getText().toString();
                password = passwordInput.getText().toString();
                Utils.writeSessionData(c, "ssid", ssid);
                Utils.writeSessionData(c, "username", username);
                Utils.writeSessionData(c, "password", password);
                dialog.dismiss();
                finish();
            }
        });

        userInfoBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                finish();
            }
        });
        userInfoBuilder.setCancelable(false);

        userInfoBuilder.show();
    }

    private void displayLocationSettingsRequest(Context context) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        //Log.i("locationRequest", "All location settings are satisfied.");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        //Log.i("locationRequest", "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(ConfigureActivity.this, LOCATION_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            //Log.i("locationRequest", "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        //Log.i("locationRequest", "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(srrRegistered) this.unregisterReceiver(srr);
    }

    /*private void callGPSEnableDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Your location services are turned off. Please click CONTINUE and enable location so the widget can scan for WiFi. Then restart the widget.")
                .setCancelable(false)
                .setPositiveButton("CONTINUE", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        finish();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        finish();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }*/

}
