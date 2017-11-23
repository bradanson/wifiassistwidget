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
    public static final String ACTION_AUTO_UPDATE = "AUTO_UPDATE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        username = Utils.readSessionData(context, "username");
        password = Utils.readSessionData(context, "password");

        //Log.d("onUpdate Widget","username: " + username);
        //Log.d("onUpdate Widget","password: " + password);

        if(username.equals("DNE")){
            username = "Username";
        }

        if(appWidgetIds.length > 0) {
            checkStatus(context, appWidgetIds, appWidgetManager, 0);
        }else{
            //Log.d("onUpdate", "no appWidgetID's.. Quitting!");
        }
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.simple_widget);
        remoteViews.setTextViewText(R.id.textView, username);

        Intent intent = new Intent(context, WifiAssistWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        remoteViews.setOnClickPendingIntent(R.id.actionButton, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        //Log.d("onReceive", "Intent received with action: " + intent.getAction());
        if(intent.getAction().equals(ACTION_AUTO_UPDATE)) {
            //Log.d("onReceive", ACTION_AUTO_UPDATE + " = " + ACTION_AUTO_UPDATE + ", Returning Void");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), WifiAssistWidget.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            checkStatusWithoutLogin(context,appWidgetIds, appWidgetManager, 0);
            return;
        }

        final Context c = context;
        ThreadManager.runInBackgroundThenUi(new Runnable() {
            @Override
            public void run() {
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
                final String desiredSSID = "\"101GLOBAL\"";
                if (wifiInfo.getSupplicantState() != SupplicantState.COMPLETED || !wifiInfo.getSSID().equals(desiredSSID)) {
                    //NOT CONNECTED TO A NETWORK OR NOT CONNECTED TO 101GLOBAL
                    if (!wifiInfo.getSSID().equals(desiredSSID)) {
                        //Log.d("WifiInfo", "NOT CONNECTED TO 101GLOBAL");
                    }
                    WifiConfiguration conf = new WifiConfiguration();
                    conf.SSID = desiredSSID;
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifi.addNetwork(conf);
                    List<WifiConfiguration> list = wifi.getConfiguredNetworks();
                    for( WifiConfiguration i : list ) {
                        if(i.SSID != null && i.SSID.equals(desiredSSID)) {
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
                }
                if (!wifiInfo.getSSID().equals(desiredSSID)) {
                    //Log.d("WifiInfo", "STILL NOT CONNECTED TO 101GLOBAL");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else {
                    //Log.d("WifiInfo", "CONNECTED TO 101GLOBAL AFTER EVERYTHING");
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(c);
                ComponentName thisAppWidget = new ComponentName(c.getPackageName(), WifiAssistWidget.class.getName());
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
                onUpdate(c, appWidgetManager, appWidgetIds);            }
        });
    }

    @Override
    public void onDisabled(Context context){
        // stop alarm
        stopAlarm(context);
    }

    private void checkStatusWithoutLogin(final Context context, final int[] appWidgetIds, final AppWidgetManager appWidgetManager, final int iter) {
        //Log.d("CheckStatusWithoutLogin", "Checking Status Without Logging In..");

        StringRequest sr = new StringRequest(Request.Method.POST, "http://login.101global.net/status", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //Log.d("statusRequest", response.toString());

                if(response.contains("101Global | Login Page")){
                    //Log.d("CheckStatusWithoutLogin", "BAD - Not logged in. Setting star/stopping alarm.");
                    showLogIn(context, appWidgetManager, appWidgetIds[0]);
                    stopAlarm(context);
                }else if(response.contains("Status | 101 Global Solutions")){
                    //Log.d("CheckStatusWithoutLogin","GOOD - LOGGED IN ALREADY");
                }
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError response) {
                //Log.d("Error", response.toString());

                if(response instanceof NoConnectionError){
                    //Log.d("CheckStatusWithoutLogin", "ERROR/BAD - NOT CONNECTED TO WIFI");
                    showLogIn(context, appWidgetManager, appWidgetIds[0]);
                    stopAlarm(context);
                }
            }
        });
        SingleRequestQueue.getInstance(context).add(sr);
    }

    private void showLogIn(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.simple_widget);
        remoteViews.setImageViewResource(R.id.imageView, R.drawable.bad_signal);
        remoteViews.setTextViewText(R.id.actionButton, context.getText(R.string.login));
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }


    private void checkStatus(final Context context, final int[] appWidgetIds, final AppWidgetManager appWidgetManager, final int iter) {

        StringRequest sr = new StringRequest(Request.Method.POST, "http://login.101global.net/status", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //Log.d("statusRequest", response.toString());
                RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                        R.layout.simple_widget);
                remoteViews.setTextViewText(R.id.textView, username);
                Intent intent = new Intent(context, WifiAssistWidget.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                if(response.contains("101Global | Login Page")){
                    //Log.d("statusRequest", "BAD - Not logged in, attempting login");
                    remoteViews.setImageViewResource(R.id.imageView, R.drawable.bad_signal);
                    if(!username.equals("DNE") && !username.equals("username")) {
                        attemptLogin(context, appWidgetIds, appWidgetManager);
                    } else{
                        //Log.d("statusRequest", "BAD - username not set");
                    }
                }else if(response.contains("Status | 101 Global Solutions")){
                   // Log.d("statusRequest","GOOD - ALREADY LOGGED IN");
                    requestLogout(context, appWidgetIds, appWidgetManager);
                }
                remoteViews.setOnClickPendingIntent(R.id.actionButton, pendingIntent);
                appWidgetManager.updateAppWidget(appWidgetIds[0], remoteViews);
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError response) {
                Log.d("Error", response.toString());

                if(response instanceof NoConnectionError){
                    //Log.d("Error", "BAD - NOT CONNECTED TO WIFI");
                    if (iter < 10000) {
                        final int newIter = iter + 1;
                        checkStatus(context, appWidgetIds, appWidgetManager, newIter);
                    }
                }
            }
        });
        SingleRequestQueue.getInstance(context).add(sr);
    }

    private void attemptLogin(final Context context, final int[] appWidgetIds, final AppWidgetManager appWidgetManager) {
        String loginUrl = "http://login.101global.net/login?username=" + username + "&password=" + password;
        //Log.d("loginRequest", "url: " + loginUrl);
        StringRequest sr = new StringRequest(Request.Method.POST, loginUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
//                Log.d("loginRequest", response.toString());
                RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                        R.layout.simple_widget);
                remoteViews.setTextViewText(R.id.textView, username);

                Intent intent = new Intent(context, WifiAssistWidget.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                if(response.contains("You are already logged in")){
                    Toast.makeText(context, context.getText(R.string.toast_already_logged_in), Toast.LENGTH_SHORT).show();
                    //Log.d("loginRequest", "BAD - ALREADY LOGGED IN ON ANOTHER DEVICE");
                    showLogIn(context, appWidgetManager, appWidgetIds[0]);
                    stopAlarm(context);
                }else if(response.contains("invalid username or password")){
                    Toast.makeText(context, context.getText(R.string.toast_invalid_un_or_pw), Toast.LENGTH_SHORT).show();
                    //Log.d("loginRequest", "BAD - invalid username or password");
                    showLogIn(context, appWidgetManager, appWidgetIds[0]);
                    stopAlarm(context);
                }else if(response.contains("access denied")){
                    //Log.d("loginRequest", "BAD - ACCESS DENIED");
                    showLogIn(context, appWidgetManager, appWidgetIds[0]);
                    stopAlarm(context);
                }else if(response.contains("You are logged in")) {
                    Toast.makeText(context, context.getText(R.string.toast_login), Toast.LENGTH_SHORT).show();
                    //Log.d("loginRequest", "GOOD - LOGGED IN");
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
                    //Log.d("LoginError", "NOT CONNECTED TO WIFI");
                    Toast.makeText(context, context.getText(R.string.toast_not_connected_to_101global),
                            Toast.LENGTH_SHORT).show();
                    stopAlarm(context);
                }
            }
        });
        SingleRequestQueue.getInstance(context).add(sr);
    }

    private void requestLogout(final Context context, final int[] appWidgetIds, final AppWidgetManager appWidgetManager) {
        stopAlarm(context);

        StringRequest sr = new StringRequest(Request.Method.POST, "http://login.101global.net/logout?", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                        R.layout.simple_widget);
                remoteViews.setTextViewText(R.id.textView, username);

                Intent intent = new Intent(context, WifiAssistWidget.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
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
        AppWidgetAlarm appWidgetAlarm = new AppWidgetAlarm(context.getApplicationContext());
        appWidgetAlarm.startAlarm();
        //Log.d("startAlarm", "Alarm Started");
    }

    private void stopAlarm(Context context) {
        AppWidgetAlarm appWidgetAlarm = new AppWidgetAlarm(context.getApplicationContext());
        appWidgetAlarm.stopAlarm();
        //Log.d("stopAlarm", "Alarm Stopped");
    }


}
