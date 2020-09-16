package activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.github.pertempto.valvetimer.R;

import common.Util;
import models.Timer;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";
    private static final String VALVE_NAME_KEY = "VALVE_NAME_KEY";
    private static final String DEFAULT_LENGTH_KEY = "DEFAULT_LENGTH_KEY";
    private static final int SERVER_INTERVAL = 10;
    private static final int LAST_SEEN_THRESH = 10;
    String valveName;
    Timer timer;
    int defaultTimerLength;
    Handler repeatingHandler;
    Runnable repeatingRunnable;
    int lastServerCheck = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateValveStatus();
        loadSettings();
        updateSettingsText();
        setupRepeatingRunnable();
    }

    @Override
    protected void onStart() {
        super.onStart();
        timer = null;
        // resume the repeating runnable
        repeatingHandler.post(repeatingRunnable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // remove the repeating runnable
        repeatingHandler.removeCallbacks(repeatingRunnable);
    }

    /* Load settings from shared preferences */
    void loadSettings() {
        SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
        valveName = sharedPref.getString(VALVE_NAME_KEY, getString(R.string.default_valve_name));
        int defaultDefaultLength = getResources().getInteger(R.integer.default_default_length);
        defaultTimerLength = sharedPref.getInt(DEFAULT_LENGTH_KEY, defaultDefaultLength);
    }

    /* Set the default length to the new value, save it in the shared preferences */
    private void setDefaultLength(int newLength) {
        SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        defaultTimerLength = newLength;
        editor.putInt(DEFAULT_LENGTH_KEY, newLength);
        editor.apply();
    }

    /* Set the valve name to the new value, save it in the shared preferences */
    private void setValveName(String newValveName) {
        SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        valveName = newValveName;
        editor.putString(VALVE_NAME_KEY, valveName);
        editor.apply();
    }

    /* Setup the runnable that runs repeatedly checking the server and updating the timer text */
    void setupRepeatingRunnable() {
        repeatingHandler = new Handler();
        repeatingRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    int now = (int) (System.currentTimeMillis() / 1000);
                    if (now - lastServerCheck >= SERVER_INTERVAL) {
                        serverUpdate();
                    }
                    updateValveStatus();
                } finally {
                    repeatingHandler.postDelayed(repeatingRunnable, 1000);
                }
            }
        };
    }

    /* Update the timer from the server */
    void serverUpdate() {
        Util.runInBackground(new Runnable() {
            @Override
            public void run() {
                timer = Util.getTimer(valveName);
                if (timer != null) {
                    lastServerCheck = (int) (System.currentTimeMillis() / 1000);
                }
            }
        });
    }

    /* Update the TextViews that display the current settings */
    void updateSettingsText() {
        TextView nameText = findViewById(R.id.nameText);
        nameText.setText(String.format(getString(R.string.valve_name_text_format), valveName));

        TextView defaultLengthText = findViewById(R.id.defaultLengthText);
        defaultLengthText.setText(String.format(getString(R.string.default_length_text_format), defaultTimerLength));
    }

    /* Update the TextView and Button that display the valve status */
    void updateValveStatus() {
        if (timer == null) {
            ConstraintLayout valveDisconnectedLayout = findViewById(R.id.valveDisconnectedLayout);
            valveDisconnectedLayout.setVisibility(View.GONE);
            ConstraintLayout valveOffLayout = findViewById(R.id.valveOffLayout);
            valveOffLayout.setVisibility(View.GONE);
            ConstraintLayout valveOnLayout = findViewById(R.id.valveOnLayout);
            valveOnLayout.setVisibility(View.GONE);
            ConstraintLayout connectingLayout = findViewById(R.id.connectingLayout);
            connectingLayout.setVisibility(View.VISIBLE);

        } else {
            ConstraintLayout connectingLayout = findViewById(R.id.connectingLayout);
            connectingLayout.setVisibility(View.GONE);

            int lastSeenLength = timer.getTimeSinceLastSeen();
            if (lastSeenLength >= LAST_SEEN_THRESH) {
                TextView lastSeenText = findViewById(R.id.lastSeenText);
                if (lastSeenLength < 3600) {
                    // under one hour
                    lastSeenText.setText(String.format(getString(R.string.offline_minutes_format), lastSeenLength / 60, lastSeenLength % 60));
                } else if (lastSeenLength < 7200) {
                    // one hour
                    lastSeenText.setText(R.string.offline_one_hour);
                } else {
                    // more than one hour
                    lastSeenText.setText(String.format(getString(R.string.offline_hours_format), lastSeenLength / 3600));
                }

                ConstraintLayout valveOffLayout = findViewById(R.id.valveOffLayout);
                valveOffLayout.setVisibility(View.GONE);
                ConstraintLayout valveOnLayout = findViewById(R.id.valveOnLayout);
                valveOnLayout.setVisibility(View.GONE);
                ConstraintLayout valveDisconnectedLayout = findViewById(R.id.valveDisconnectedLayout);
                valveDisconnectedLayout.setVisibility(View.VISIBLE);
            } else {
                ConstraintLayout valveDisconnectedLayout = findViewById(R.id.valveDisconnectedLayout);
                valveDisconnectedLayout.setVisibility(View.GONE);

                int timerLength = timer.getLength();
                if (timerLength > 0) {
                    ConstraintLayout valveOffLayout = findViewById(R.id.valveOffLayout);
                    valveOffLayout.setVisibility(View.GONE);
                    ConstraintLayout valveOnLayout = findViewById(R.id.valveOnLayout);
                    valveOnLayout.setVisibility(View.VISIBLE);

                    TextView lengthText = findViewById(R.id.lengthText);
                    String lengthString = String.format("%d seconds", timerLength);
                    if (timerLength == 1) {
                        lengthString = "One moment";
                    } else if (timerLength >= 60) {
                        lengthString = String.format("%d:%02d", timerLength / 60, timerLength % 60);
                    }
                    lengthText.setText(lengthString);
                } else {
                    ConstraintLayout valveOnLayout = findViewById(R.id.valveOnLayout);
                    valveOnLayout.setVisibility(View.GONE);
                    ConstraintLayout valveOffLayout = findViewById(R.id.valveOffLayout);
                    valveOffLayout.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    /* The edit button for the default length was tapped */
    public void onEditDefaultLengthClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_edit_default_length, null);
        final EditText lengthInput = dialogView.findViewById(R.id.lengthInput);
        lengthInput.setText(Integer.toString(defaultTimerLength));

        final Context context = this;
        builder.setTitle("Set Default Timer Length")
                .setView(dialogView)
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newLengthString = lengthInput.getText().toString();
                        boolean badInput = false;
                        try {
                            int newLength = Integer.valueOf(newLengthString);
                            if (newLength > 0) {
                                setDefaultLength(newLength);
                                updateSettingsText();
                            } else {
                                badInput = true;
                            }
                        } catch (NumberFormatException e) {
                            badInput = true;
                        }
                        // display error message for bad input
                        if (badInput) {
                            Toast.makeText(context, "Invalid timer length", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null);
        builder.show();
    }

    /* The edit button for the name was tapped */
    public void onEditNameClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_edit_name, null);
        final EditText nameInput = dialogView.findViewById(R.id.nameInput);
        nameInput.setText(valveName);

        final Context context = this;
        builder.setTitle("Set Valve Name")
                .setView(dialogView)
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newValveName = nameInput.getText().toString();
                        Log.d(LOG_TAG, String.format("New valve name: %s", newValveName));
                        setValveName(newValveName);
                        updateSettingsText();
                        serverUpdate();
                    }
                })
                .setNegativeButton("Cancel", null);
        builder.show();
    }

    /* A button to set the timer was tapped */
    public void onSetTimerClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_turn_valve_on, null);
        final NumberPicker lengthInput = dialogView.findViewById(R.id.lengthInput);
        lengthInput.setMinValue(1);
        lengthInput.setMaxValue(60);
        lengthInput.setValue(defaultTimerLength);

        builder.setTitle(getString(R.string.set_timer))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.set), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final int newLength = lengthInput.getValue();
                        Util.runInBackground(new Runnable() {
                            @Override
                            public void run() {
                                timer = Util.setTimerLength(timer, newLength * 60);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateValveStatus();
                                    }
                                });
                            }
                        });
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    /* The stop button was tapped */
    public void onStopTimerClick(View v) {
        Log.d(LOG_TAG, "stop button pressed");
        Util.runInBackground(new Runnable() {
            @Override
            public void run() {
                timer = Util.setTimerLength(timer, 0);
                Log.d(LOG_TAG, "timer stopped");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateValveStatus();
                    }
                });
            }
        });
    }
}
