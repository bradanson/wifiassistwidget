package antsinapps.WifiAssistWidget;

import android.Manifest;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class ConfigureActivity extends AppCompatActivity {

    String username;
    String password;
    String ssid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        int widgetID = 0;
        if (extras != null) {
            widgetID = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }


        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        setResult(RESULT_OK, resultValue);
        final ViewGroup contentView = (ViewGroup) findViewById(android.R.id.content);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED))
        {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_NETWORK_STATE}, 0x12345);
        }else{
            // Either app already granted permissions or device is older than Marshmallow
            // Enable WIFI
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (!wifi.isWifiEnabled()){
                //Log.d("WifiCheck", "WIFI NOT ENABLED");
                wifi.setWifiEnabled(true);
                Toast.makeText(getApplicationContext(), getText(R.string.toast_enabling_wifi), Toast.LENGTH_LONG).show();
            }

            // Prune for duplicates in scanned list
            Set<String> hs = new HashSet<>();
            for(ScanResult i : wifi.getScanResults()){
                hs.add(i.SSID);
            }

            // Show SSID Dialog, then User Info Dialog
            showDialogs(hs, contentView);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        final ViewGroup contentView = (ViewGroup) findViewById(android.R.id.content);
        if (requestCode == 0x12345) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            // Enable WIFI
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (!wifi.isWifiEnabled()){
                //Log.d("WifiCheck", "WIFI NOT ENABLED");
                wifi.setWifiEnabled(true);
                Toast.makeText(getApplicationContext(), getText(R.string.toast_enabling_wifi), Toast.LENGTH_LONG).show();
            }

            // Prune for duplicates in scanned list
            Set<String> hs = new HashSet<>();
            for(ScanResult i : wifi.getScanResults()){
                hs.add(i.SSID);
            }

            // Show SSID Dialog, then User Info Dialog
            showDialogs(hs, contentView);
        }else{
            // User did not grant permissions to check for WiFi
            showUserInfoDialog(contentView,"");
        }
    }

    private void showDialogs(Set<String> hs, final ViewGroup contentView) {
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                ConfigureActivity.this,
                android.R.layout.select_dialog_singlechoice);

        // Add individual entries
        for(String j : hs){
            arrayAdapter.add(j);
        }

        if (hs.size() == 0) {
            Toast.makeText(getApplicationContext(), getText(R.string.toast_no_networks), Toast.LENGTH_LONG).show();
            showUserInfoDialog(contentView, "");
        }else {
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
                            showUserInfoDialog(contentView, selectedSSID);

                        }
                    });
            wifiSelectBuilder.setCancelable(false);
            wifiSelectBuilder.show();

        }
    }

    private void showUserInfoDialog(ViewGroup contentView, String selectedSSID) {
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

}
