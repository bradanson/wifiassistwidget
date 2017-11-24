package antsinapps.WifiAssistWidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.List;

public class WifiAssistWidget extends AppWidgetProvider {

    String username;
    String password;
    String ssid;
    String WIDGET_SIZE;
    static String WIDGET_LARGE = "LARGE";
    static String WIDGET_SMALL = "SMALL";
    public static final String ACTION_LARGE_AUTO_UPDATE = "LARGE_AUTO_UPDATE";
    public static final String ACTION_SMALL_AUTO_UPDATE = "SMALL_AUTO_UPDATE";
    public static final String ACTION_LARGE_APPWIDGET_UPDATE = "LARGE_APPWIDGET_UPDATE";
    public static final String ACTION_SMALL_APPWIDGET_UPDATE = "SMALL_APPWIDGET_UPDATE";

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

        Intent intent = getIntentBySize(context);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        remoteViews.setOnClickPendingIntent(R.id.actionButton, pendingIntent);
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
            checkStatusWithoutLogin(context, appWidgetIds, appWidgetManager, 0);
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

        WifiInfo wifiInfo = wifi.getConnectionInfo();
        final String desiredSSID = "\"" + ssid.toUpperCase() + "\"";
        if (wifiInfo.getSupplicantState() != SupplicantState.COMPLETED || !wifiInfo.getSSID().equals(desiredSSID)) {
            //NOT CONNECTED TO A NETWORK OR NOT CONNECTED TO SPECIFIED SSID
            if (!wifiInfo.getSSID().equals(desiredSSID)) {
                //Not connected to desired network
            }
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = desiredSSID;
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifi.addNetwork(conf);
            List<WifiConfiguration> list = wifi.getConfiguredNetworks();
            boolean ssidFound = false;
            for( WifiConfiguration i : list ) {
                if(i.SSID != null && i.SSID.equals(desiredSSID)) {
                    ssidFound = true;
                    wifi.disconnect();
                    wifi.enableNetwork(i.networkId, true);
                    wifi.reconnect();
                    ThreadManager.runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(c.getApplicationContext(), "Connecting to "+ desiredSSID,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    break;
                }
            }
            if(!ssidFound){
                Toast.makeText(c.getApplicationContext(), "Network "+ desiredSSID +" not found.",
                        Toast.LENGTH_LONG).show();
            }
        }
        if (!wifiInfo.getSSID().equals(desiredSSID)) {
            //Log.d("WifiInfo", "STILL NOT CONNECTED TO SSID");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {
            //Log.d("WifiInfo", "CONNECTED TO SSID AFTER EVERYTHING");
        }
    }

    private void checkStatusWithoutLogin(final Context context, final int[] appWidgetIds, final AppWidgetManager appWidgetManager, final int iter) {
        //Log.d("CheckStatusWithoutLogin", "Checking Status Without Logging In..");

        StringRequest sr = new StringRequest(Request.Method.POST, "http://login."+ssid+".net/status", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //Log.d("statusRequest", response.toString());

                if(response.contains(" | Login Page")){
                    // Log.d("CheckStatusWithoutLogin", "BAD - Not logged in. Setting symbol/stopping alarm.");
                    showLogIn(context, appWidgetManager, appWidgetIds[0]);
                    stopAlarm(context);
                }else if(response.contains("Status | ")){
                    // Log.d("CheckStatusWithoutLogin","GOOD - LOGGED IN ALREADY");
                }
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError response) {
                //   Log.d("Error", response.toString());

                if(response instanceof NoConnectionError){
                    // Log.d("CheckStatusWithoutLogin", "ERROR/BAD - NOT CONNECTED TO WIFI");
                    showLogIn(context, appWidgetManager, appWidgetIds[0]);
                    stopAlarm(context);
                }
            }
        });
        SingleRequestQueue.getInstance(context).add(sr);
    }

    private void showLogIn(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews remoteViews = getRemoteViewsBySize(context);
        remoteViews.setImageViewResource(R.id.imageView, R.drawable.bad_signal);
        remoteViews.setTextViewText(R.id.actionButton, context.getText(R.string.login));
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
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
        StringRequest sr = new StringRequest(Request.Method.POST, "http://login."+ssid+".net/status", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //Log.d("statusRequest", response.toString());
                RemoteViews remoteViews = getRemoteViewsBySize(context);
                Intent intent = getIntentBySize(context);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                if(response.contains(" | Login Page")){
                    //Log.d("statusRequest", "BAD - Not logged in, attempting login");
                    remoteViews.setImageViewResource(R.id.imageView, R.drawable.bad_signal);
                    if(!username.equals("DNE") && !username.equals("username")) {
                        attemptLogin(context, appWidgetIds, appWidgetManager);
                    } else{
                        // Log.d("statusRequest", "BAD - username not set");
                    }
                }else if(response.contains("Status | ")){
                    //Log.d("statusRequest","GOOD - ALREADY LOGGED IN");
                    requestLogout(context, appWidgetIds, appWidgetManager);
                }else {
                    //Log.d("statusRequest", "Response from website not recognized.");
                }
                remoteViews.setOnClickPendingIntent(R.id.actionButton, pendingIntent);
                appWidgetManager.updateAppWidget(appWidgetIds[0], remoteViews);
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError response) {
                // Log.d("Error", response.toString());

                if(response instanceof NoConnectionError){
                    //Log.d("Error", "BAD - NOT CONNECTED TO WIFI");
                    if (iter < 1000) {
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
        String loginUrl = "http://login."+ssid+".net/login?username=" + username + "&password=" + password;
        // Log.d("loginRequest", "url: " + loginUrl);
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
                    showLogIn(context, appWidgetManager, appWidgetIds[0]);
                    stopAlarm(context);
                }else if(response.contains("invalid username or password")){
                    Toast.makeText(context, context.getText(R.string.toast_invalid_un_or_pw), Toast.LENGTH_SHORT).show();
                    //Log.d("loginRequest", "BAD - invalid username or password");
                    showLogIn(context, appWidgetManager, appWidgetIds[0]);
                    stopAlarm(context);
                }else if(response.contains("access denied")){
                    // Log.d("loginRequest", "BAD - ACCESS DENIED");
                    showLogIn(context, appWidgetManager, appWidgetIds[0]);
                    stopAlarm(context);
                }else if(response.contains("You are logged in")) {
                    Toast.makeText(context, context.getText(R.string.toast_login), Toast.LENGTH_SHORT).show();
                    // Log.d("loginRequest", "GOOD - LOGGED IN");
                    remoteViews.setTextViewText(R.id.actionButton, context.getText(R.string.logout));
                    remoteViews.setImageViewResource(R.id.imageView, R.drawable.good_signal);
                    startAlarm(context);
                }
                remoteViews.setOnClickPendingIntent(R.id.actionButton, pendingIntent);
                appWidgetManager.updateAppWidget(appWidgetIds[0], remoteViews);
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError response) {
                //Log.d("LoginError", response.toString());
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
                showLogIn(context, appWidgetManager, appWidgetIds[0]);
                remoteViews.setOnClickPendingIntent(R.id.actionButton, pendingIntent);
                appWidgetManager.updateAppWidget(appWidgetIds[0], remoteViews);

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
