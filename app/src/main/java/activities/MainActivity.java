package activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.pertempto.valvetimer.R;

public class MainActivity extends AppCompatActivity {

    private static final String VALVE_IP_KEY = "VALVE_IP_KEY";
    private static final String AUTO_REFRESH_KEY = "AUTO_REFRESH_KEY";
    private static final String MAX_LENGTH_KEY = "MAX_LENGTH_KEY";
    String valveIP = "192.168.1.116";
    int autoRefreshSeconds = 10;
    int maxTimerLength = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateValveStatus();

        loadSettings();

        updateSettingsText();
    }

    /* Load settings from shared preferences */
    void loadSettings() {
        SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
        valveIP = sharedPref.getString(VALVE_IP_KEY, getString(R.string.default_valve_ip));
        int defaultAutoRefresh = getResources().getInteger(R.integer.default_auto_refresh);
        autoRefreshSeconds = sharedPref.getInt(AUTO_REFRESH_KEY, defaultAutoRefresh);
        int defaultMaxLength = getResources().getInteger(R.integer.default_max_length);
        maxTimerLength = sharedPref.getInt(MAX_LENGTH_KEY, defaultMaxLength);
    }

    /* Update the TextViews that display the current settings */
    void updateSettingsText() {
        TextView ipStatusText = findViewById(R.id.ipStatusText);
        ipStatusText.setText(String.format(getString(R.string.valve_ip_text_format), valveIP));

        TextView autoRefreshText = findViewById(R.id.autoRefreshText);
        autoRefreshText.setText(String.format(getString(R.string.auto_refresh_text_format), autoRefreshSeconds));

        TextView maxLengthText = findViewById(R.id.maxLengthText);
        maxLengthText.setText(String.format(getString(R.string.max_length_text_format), maxTimerLength));
    }

    /* Update the TextView and Button that display the valve status */
    void updateValveStatus() {
        TextView statusText = findViewById(R.id.statusText);
        statusText.setText(R.string.valve_off);

        Button valveButton = findViewById(R.id.valveButton);
        valveButton.setText(R.string.turn_on);
    }
}
