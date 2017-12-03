package antsinapps.WifiAssistWidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.List;

public class WifiAssistWidget extends AppWidgetProvider {

    String username;
    String password;
    String ssid;
    String WIDGET_SIZE;
    public static final String WIDGET_LARGE = "LARGE";
    public static final String WIDGET_SMALL = "SMALL";
    public static final String ACTION_LARGE_AUTO_UPDATE = "LARGE_AUTO_UPDATE";
    public static final String ACTION_SMALL_AUTO_UPDATE = "SMALL_AUTO_UPDATE";
    public static final String ACTION_LARGE_APPWIDGET_UPDATE = "LARGE_APPWIDGET_UPDATE";
    public static final String ACTION_SMALL_APPWIDGET_UPDATE = "SMALL_APPWIDGET_UPDATE";
    public static final String KNOWS_STATE = "KNOWS_STATE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        username = Utils.readSessionData(context, "username");
        password = Utils.readSessionData(context, "password");
        ssid = Utils.readSessionData(context, "ssid");

        //  Log.d("onUpdate Widget","ssid: " + ssid);
        //  Log.d("onUpdate Widget","username: " + username);
        //  Log.d("onUpdate Widget","password: " + password);

        if(username.equals("DNE")){
            username = "Username";
        }
        if(ssid == null || ssid.equals("DNE")){
            ssid = "No Network Provided";
        }

        if(appWidgetIds.length > 0) {
            checkStatus(context, appWidgetIds, appWidgetManager, 0);
        }else{
            //  Log.d("onUpdate", "no appWidgetID's.. Quitting!");
        }
        RemoteViews remoteViews = getRemoteViewsBySize(context);

        // Configure button for next click
        Intent intent = getIntentBySize(context);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.actionButton, pendingIntent);

        // Configure image for next click
        Intent configureIntent = new Intent(context, ConfigureActivity.class);
        configureIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        PendingIntent configPendingIntent = PendingIntent.getActivity(context,
                0, configureIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.imageView, configPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {

        username = Utils.readSessionData(context, "username");
        password = Utils.readSessionData(context, "password");
        ssid = Utils.readSessionData(context, "ssid");
        //Log.d("onReceive Widget","ssid: " + ssid);
        //Log.d("onReceive Widget","username: " + username);
        //Log.d("onReceive Widget","password: " + password);
        //Log.d("MainonReceive", "Intent received with action: " + intent.getAction());

        if(intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_DELETED) ||
                intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_DISABLED)){
            return;
        }
        if(username.equals("Username") || username.equals("DNE") ||
                ssid.equals("No Network Provided") || ssid.equals("DNE")){
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = getComponentNameBySize(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
            return;
        }


        if(intent.getAction().equals(ACTION_SMALL_AUTO_UPDATE) || intent.getAction().equals(ACTION_LARGE_AUTO_UPDATE)) {
            //Log.d("onReceive", ACTION_AUTO_UPDATE + " = " + ACTION_AUTO_UPDATE + ", Returning Void");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = getComponentNameBySize(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            alarmCheckStatus(context, appWidgetIds, appWidgetManager, 0);
            return;
        }

        final Context c = context;
        ThreadManager.runInBackgroundThenUi(new Runnable() {
            @Override
            public void run() {
                ConnectToSSID(c);
            }
        }, new Runnable() {
            @Override
            public void run() {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(c);
                ComponentName thisAppWidget = getComponentNameBySize(c);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
                onUpdate(c, appWidgetManager, appWidgetIds);            }
        });
    }

    @Override
    public void onDisabled(Context context){
        // stop alarm
        stopAlarm(context);
    }

    private void ConnectToSSID(final Context c) {
        WifiManager wifi = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()){
            //Log.d("WifiCheck", "WIFI NOT ENABLED");
            wifi.setWifiEnabled(true);
            ThreadManager.runOnUi(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(c.getApplicationContext(), c.getText(R.string.toast_enabling_wifi), Toast.LENGTH_LONG).show();
                }
            });
        }

        //boolean ssidFound = false;
        //ssidFound = connectedToSSID(wifi, ssidFound);
//        if(!ssidFound){
//            ThreadManager.runOnUi(new Runnable() {
//                @Override
//                public void run() {
//                    if(!uncertainState) {
//                        Toast.makeText(c.getApplicationContext(), "Network " + desiredSSID + " not found." +
//                                        " Are location services turned on? Attempting to force connection..",
//                                Toast.LENGTH_LONG).show();
//                    }
//                }
//            });
//        }
        WifiInfo wifiInfo = wifi.getConnectionInfo();
        final String desiredSSID = "\"" + ssid.toUpperCase() + "\"";
        if (!wifiInfo.getSSID().equals(desiredSSID) || wifiInfo.getSupplicantState() != SupplicantState.COMPLETED) {
            // Log.d("WifiInfo", "STILL NOT CONNECTED TO SSID");
            try {
                //  Log.d("WifiInfo", "5seconds are starting!");
                Thread.sleep(5000);
                //  Log.d("WifiInfo", "5seconds are up!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {
            // Log.d("WifiInfo", "CONNECTED TO SSID AFTER EVERYTHING");
        }


        connectIfNotConnected(wifi);
    }

    private void connectIfNotConnected(WifiManager wifi) {
        // Log.d("connectIfNotConnected", "here");
        WifiInfo wifiInfo = wifi.getConnectionInfo();
        final String desiredSSID = "\"" + ssid.toUpperCase() + "\"";
        if (wifiInfo.getSupplicantState() != SupplicantState.COMPLETED ||
                wifiInfo.getSSID().length() < 5 ||
                desiredSSID.length() < 5 ||
                !(wifiInfo.getSSID().substring(0, 2).equals(desiredSSID.substring(0, 2)))) {
            //    Log.d("connectIfNotConnected", "attempting to connect..");
            // If user is not connected to a network or not the provided SSID, connect them
//            if (!wifiInfo.getSSID().equals(desiredSSID)) {
//                //Not connected to desired network
//            }
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = desiredSSID;
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifi.addNetwork(conf);
            List<WifiConfiguration> list = wifi.getConfiguredNetworks();
            for( WifiConfiguration i : list ) {
                if(i.SSID != null && i.SSID.equals(desiredSSID)) {
                    //     Log.d("WifiConfiguration","ssid found: " + i.SSID);
                    wifi.disconnect();
                    wifi.enableNetwork(i.networkId, true);
                    wifi.reconnect();
                    break;
                }
            }
        }
    }

    private boolean findSSID(WifiManager wifi, boolean ssidFound) {
        for(ScanResult scanResult : wifi.getScanResults()){
            if(scanResult.SSID.toUpperCase().equals(ssid.toUpperCase())){
                return true;
            }
        }
        return false;
    }

    private void alarmCheckStatus(final Context context, final int[] appWidgetIds, final AppWidgetManager appWidgetManager, final int iter) {
        //Log.d("CheckStatusWithoutLogin", "Checking Status Without Logging In..");
        String ssidToQuery = conditionSSIDToQuery();
        StringRequest sr = new StringRequest(Request.Method.POST, "http://login."+ssidToQuery+".net/status", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //Log.d("statusRequest", response.toString());

                if(response.contains(" | Login Page")){
                    // Log.d("CheckStatusWithoutLogin", "BAD - Not logged in. Setting symbol/stopping alarm.");
                    showLoggedOut(context, appWidgetManager, appWidgetIds);
                    stopAlarm(context);
                }else if(response.contains("Status | ")){
                    // Log.d("CheckStatusWithoutLogin","GOOD - LOGGED IN ALREADY");
                    showLoggedIn(context,appWidgetManager,appWidgetIds);
                }else{
                    showUncertain(context, appWidgetManager, appWidgetIds);
                    stopAlarm(context);
                }
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError response) {
                //   Log.d("Error", response.toString());


                //if(response instanceof NoConnectionError){
                // Log.d("CheckStatusWithoutLogin", "ERROR/BAD - NOT CONNECTED TO WIFI");
                showUncertain(context, appWidgetManager, appWidgetIds);
                stopAlarm(context);
                //}
            }
        });
        SingleRequestQueue.getInstance(context).add(sr);
    }

    private String conditionSSIDToQuery() {
        String ssidToQuery = ssid;
        if(ssid.contains(" ")){
            ssidToQuery = ssid.substring(0, ssid.indexOf(" "));
        }
        if(ssidToQuery.contains("-")){
            ssidToQuery = ssidToQuery.replace("-","");
        }
        return ssidToQuery;
    }

    private void showLoggedOut(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews remoteViews = getRemoteViewsBySize(context);
        remoteViews.setImageViewResource(R.id.imageView, R.drawable.bad_signal);
        remoteViews.setTextViewText(R.id.actionButton, context.getText(R.string.login));
        for(int i : appWidgetIds) {
            appWidgetManager.updateAppWidget(i, remoteViews);
        }
       // Log.d("knows_state", "(showLoggedOut) true");
        Utils.writeSessionData(context, KNOWS_STATE, "true");
    }

    private void showLoggedIn(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews remoteViews = getRemoteViewsBySize(context);
        remoteViews.setImageViewResource(R.id.imageView, R.drawable.good_signal);
        remoteViews.setTextViewText(R.id.actionButton, context.getText(R.string.logout));

        for(int i : appWidgetIds) {
            appWidgetManager.updateAppWidget(i, remoteViews);
        }
       // Log.d("knows_state", "(showLoggedIn) true");
        Utils.writeSessionData(context, KNOWS_STATE, "true");
    }

    private void showUncertain(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews remoteViews = getRemoteViewsBySize(context);
        remoteViews.setImageViewResource(R.id.imageView, R.drawable.unsure);
        remoteViews.setTextViewText(R.id.actionButton, context.getText(R.string.check_status));
        for(int i : appWidgetIds) {
            appWidgetManager.updateAppWidget(i, remoteViews);
        }
        //Log.d("knows_state", "(showUncertain) false");
        Utils.writeSessionData(context, KNOWS_STATE, "false");
    }

    private RemoteViews getRemoteViewsBySize(Context context) {
        if(WIDGET_SIZE.equals(WIDGET_SMALL)){
            return new RemoteViews(context.getPackageName(),
                    R.layout.small_widget);
        }
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.large_widget);
        remoteViews.setTextViewText(R.id.textView, username);
        return remoteViews;
    }

    private Intent getIntentBySize(Context context) {
        if(WIDGET_SIZE.equals(WIDGET_SMALL)){
            Intent intent = new Intent(context, WifiAssistWidgetSmall.class);
            intent.setAction(WifiAssistWidget.ACTION_SMALL_APPWIDGET_UPDATE);
            return intent;
        }
        Intent intent = new Intent(context, WifiAssistWidgetLarge.class);
        intent.setAction(WifiAssistWidget.ACTION_LARGE_APPWIDGET_UPDATE);
        return intent;
    }

    @NonNull
    private ComponentName getComponentNameBySize(Context context) {
        if(WIDGET_SIZE.equals(WIDGET_SMALL)){
            //  Log.d("getComponentNameBySize", "SMALL");
            return new ComponentName(context.getPackageName(), WifiAssistWidgetSmall.class.getName());
        }
        // Log.d("getComponentNameBySize", "LARGE");

        return new ComponentName(context.getPackageName(), WifiAssistWidgetLarge.class.getName());
    }


    void checkStatus(final Context context, final int[] appWidgetIds, final AppWidgetManager appWidgetManager, final int iter) {
        //  Log.d("statusRequest", "Sending request");
        //  Log.d("statusRequest", "url: "+ "http://login."+ssid+".net/status");
        String ssidToQuery = conditionSSIDToQuery();

        StringRequest sr = new StringRequest(Request.Method.POST, "http://login."+ssidToQuery+".net/status", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //    Log.d("statusRequest", response.toString());
                RemoteViews remoteViews = getRemoteViewsBySize(context);
                Intent intent = getIntentBySize(context);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                if(response.contains(" | Login Page")){
                    //     Log.d("statusRequest", "BAD - Not logged in, attempting login");
                    if(!username.equals("DNE") && !username.equals("username")){
                      //  Log.d("uncertainState", "Login Page: knows_status at runtime is: " + Utils.readSessionData(context, KNOWS_STATE));
                        if (Boolean.valueOf(Utils.readSessionData(context, KNOWS_STATE))) {
                            attemptLogin(context, appWidgetIds, appWidgetManager);
                        } else showLoggedOut(context, appWidgetManager, appWidgetIds);
                    }else{
                        //          Log.d("statusRequest", "BAD - username not set");
                    }
                }else if(response.contains("Status | ")){
                    //     Log.d("statusRequest","GOOD - ALREADY LOGGED IN");
                    //Log.d("uncertainState", "Status | checkStatus at runtime is: " + Utils.readSessionData(context, KNOWS_STATE));
                    if (Boolean.valueOf(Utils.readSessionData(context, KNOWS_STATE))) {
                        requestLogout(context, appWidgetIds, appWidgetManager);
                    } else {
                        showLoggedIn(context, appWidgetManager, appWidgetIds);
                        startAlarm(context);
                    }
                }else {
                    //       Log.d("statusRequest", "Response from website not recognized.");
                }
                remoteViews.setOnClickPendingIntent(R.id.actionButton, pendingIntent);
                for(int i : appWidgetIds) {
                    appWidgetManager.updateAppWidget(i, remoteViews);
                }
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError response) {
                Log.d("Error", response.toString());

                if(response instanceof ServerError){
                    Toast.makeText(context, context.getText(R.string.toast_server_error), Toast.LENGTH_LONG).show();
                }

                if(response instanceof AuthFailureError){
                    Toast.makeText(context, context.getText(R.string.toast_auth_failed), Toast.LENGTH_SHORT).show();
                }

                if(response instanceof NoConnectionError){
                    //Log.d("Error", "BAD - NOT CONNECTED TO WIFI");
                    if (iter < 100) {
                        final int newIter = iter + 1;
                        if(!ssid.equals("No Network Provided") && !ssid.equals("DNE")) {
                            checkStatus(context, appWidgetIds, appWidgetManager, newIter);
                        }
                    }
                }
            }
        });
        SingleRequestQueue.getInstance(context).add(sr);
    }

    private void attemptLogin(final Context context, final int[] appWidgetIds, final AppWidgetManager appWidgetManager) {
        String ssidToQuery = conditionSSIDToQuery();

        String loginUrl = "http://login."+ssidToQuery+".net/login?username=" + username + "&password=" + password;
        //     Log.d("loginRequest", "url: " + loginUrl);
        StringRequest sr = new StringRequest(Request.Method.POST, loginUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
//                Log.d("loginRequest", response.toString());
                RemoteViews remoteViews = getRemoteViewsBySize(context);
                Intent intent = getIntentBySize(context);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                if(response.contains("You are already logged in")){
                    Toast.makeText(context, context.getText(R.string.toast_already_logged_in), Toast.LENGTH_SHORT).show();
                    // Log.d("loginRequest", "BAD - ALREADY LOGGED IN ON ANOTHER DEVICE");
                    showLoggedOut(context, appWidgetManager, appWidgetIds);
                    stopAlarm(context);
                }else if(response.contains("invalid username or password")){
                    Toast.makeText(context, context.getText(R.string.toast_invalid_un_or_pw), Toast.LENGTH_SHORT).show();
                    //Log.d("loginRequest", "BAD - invalid username or password");
                    showLoggedOut(context, appWidgetManager, appWidgetIds);
                    stopAlarm(context);
                }else if(response.contains("access denied")){
                    // Log.d("loginRequest", "BAD - ACCESS DENIED");
                    showLoggedOut(context, appWidgetManager, appWidgetIds);
                    stopAlarm(context);
                }else if(response.contains("You are logged in")) {
                    Toast.makeText(context, context.getText(R.string.toast_login), Toast.LENGTH_SHORT).show();
                    // Log.d("loginRequest", "GOOD - LOGGED IN");
                    showLoggedIn(context, appWidgetManager, appWidgetIds);
                    startAlarm(context);
                }
                remoteViews.setOnClickPendingIntent(R.id.actionButton, pendingIntent);
                for(int i : appWidgetIds) {
                    appWidgetManager.updateAppWidget(i, remoteViews);
                }
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError response) {
                Log.d("LoginError", response.toString());

                if(response instanceof ServerError){
                    Toast.makeText(context, context.getText(R.string.toast_server_error), Toast.LENGTH_LONG).show();
                }

                if(response instanceof AuthFailureError){
                    Toast.makeText(context, context.getText(R.string.toast_auth_failed), Toast.LENGTH_SHORT).show();
                }

                if(response instanceof NoConnectionError){
                    //  Log.d("LoginError", "NOT CONNECTED TO WIFI");
                    Toast.makeText(context, context.getText(R.string.toast_not_connected_to_ssid) + ssid,
                            Toast.LENGTH_SHORT).show();
                    stopAlarm(context);
                }
            }
        });
        SingleRequestQueue.getInstance(context).add(sr);
    }

    private void requestLogout(final Context context, final int[] appWidgetIds, final AppWidgetManager appWidgetManager) {
        stopAlarm(context);

        StringRequest sr = new StringRequest(Request.Method.POST, "http://login."+ssid+ ".net/logout?", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                RemoteViews remoteViews = getRemoteViewsBySize(context);

                Intent intent = getIntentBySize(context);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                if(response.contains("You have log out")) {
                    Toast.makeText(context, context.getText(R.string.toast_logout), Toast.LENGTH_SHORT).show();
                    //Log.d("logoutRequest", "LOGGED OUT");
                }
                showLoggedOut(context, appWidgetManager, appWidgetIds);
                remoteViews.setOnClickPendingIntent(R.id.actionButton, pendingIntent);
                for(int i : appWidgetIds) {
                    appWidgetManager.updateAppWidget(i, remoteViews);
                }
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError response) {
                Log.d("Error", response.toString());
            }
        });
        SingleRequestQueue.getInstance(context).add(sr);
    }

    private void startAlarm(Context context) {
        AppWidgetAlarm appWidgetAlarm = new AppWidgetAlarm(context.getApplicationContext(), WIDGET_SIZE);
        appWidgetAlarm.startAlarm();
        // Log.d("startAlarm", "Alarm Started");
    }

    private void stopAlarm(Context context) {
        AppWidgetAlarm appWidgetAlarm = new AppWidgetAlarm(context.getApplicationContext(), WIDGET_SIZE);
        appWidgetAlarm.stopAlarm();
        // Log.d("stopAlarm", "Alarm Stopped");
    }
}
