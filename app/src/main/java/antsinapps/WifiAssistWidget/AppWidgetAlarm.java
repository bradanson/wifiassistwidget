package antsinapps.WifiAssistWidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

class AppWidgetAlarm
{
    private final int ALARM_ID = 0;
    private final int INTERVAL_MILLIS = 60000;

    private Context mContext;
    private String mSize;


    AppWidgetAlarm(Context context, String size)
    {
        mContext = context;
        mSize = size;
    }

    void startAlarm()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, INTERVAL_MILLIS);
        Intent alarmIntent = new Intent(WifiAssistWidget.ACTION_SMALL_AUTO_UPDATE);
        if(mSize.equals(WifiAssistWidget.WIDGET_LARGE)) {
            alarmIntent = new Intent(WifiAssistWidget.ACTION_LARGE_AUTO_UPDATE);
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, ALARM_ID, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        // RTC does not wake the device up
        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), INTERVAL_MILLIS, pendingIntent);
    }


    void stopAlarm()
    {
        Intent alarmIntent = new Intent(WifiAssistWidget.ACTION_SMALL_AUTO_UPDATE);
        if(mSize.equals(WifiAssistWidget.WIDGET_LARGE)) {
            alarmIntent = new Intent(WifiAssistWidget.ACTION_LARGE_AUTO_UPDATE);
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, ALARM_ID, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }
}