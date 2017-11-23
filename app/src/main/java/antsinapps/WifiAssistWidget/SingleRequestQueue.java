package antsinapps.WifiAssistWidget;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class SingleRequestQueue {
    private static RequestQueue mRequestQueue;

     public static synchronized RequestQueue getInstance(Context context) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return mRequestQueue;
    }
}
