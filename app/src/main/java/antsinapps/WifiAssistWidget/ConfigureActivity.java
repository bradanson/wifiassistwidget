package antsinapps.WifiAssistWidget;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class ConfigureActivity extends AppCompatActivity {

    String username;
    String password;
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.dialogTitle));
        ViewGroup view = (ViewGroup) findViewById(android.R.id.content);
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.input_layout, view, false);
        final EditText usernameInput = (EditText) viewInflated.findViewById(R.id.usernameInput);
        final EditText passwordInput = (EditText) viewInflated.findViewById(R.id.passwordInput);
        builder.setView(viewInflated);

        final Context c = this;

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                username = usernameInput.getText().toString();
                password = passwordInput.getText().toString();
                Utils.writeSessionData(c, "username", username);
                Utils.writeSessionData(c, "password", password);
                dialog.dismiss();
                finish();
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                finish();
            }
        });
        builder.setCancelable(false);

        builder.show();

        Utils.writeSessionData(this, "username", username);
        Utils.writeSessionData(this, "password", password);
    }

}
