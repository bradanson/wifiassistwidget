package antsinapps.WifiAssistWidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Brad on 11/23/2017.
 */

public class WifiAssistWidgetSmall extends WifiAssistWidget {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        WIDGET_SIZE = WIDGET_SMALL;
       // Log.d("SmallWidget", "onUpdate");
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        WIDGET_SIZE = WIDGET_SMALL;
       // Log.d("SmallWidget", "onReceive");
        super.onReceive(context, intent);
    }
}
