package wavrecorder.com.fabian.aron.wavrecorder;


import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FormActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    SeekBar loudnessSeekBar;
    TextView loudnessValueText;
    SeekBar eventLengthSeekBar;
    TextView eventLengthValueText;
    Button sendButton;
    RadioGroup soundSysGroup;
    RadioGroup targetAudGroup;
    EditText typeText;
    EditText locationText;
    EditText distanceText;
    EditText commentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        loudnessSeekBar = findViewById(R.id.input_loudness);
        loudnessSeekBar.setOnSeekBarChangeListener(this);
        loudnessValueText = findViewById(R.id.text_loudnesvalue);
        eventLengthSeekBar = findViewById(R.id.input_eventlength);
        eventLengthSeekBar.setOnSeekBarChangeListener(this);
        eventLengthValueText = findViewById(R.id.text_eventlengthvalue);
        sendButton = findViewById(R.id.button_formsend);
        sendButton.setOnClickListener(this);
        soundSysGroup = findViewById(R.id.sound_system);
        targetAudGroup = findViewById(R.id.target_audience);
        typeText = findViewById(R.id.input_type);
        locationText = findViewById(R.id.input_location);
        distanceText = findViewById(R.id.input_distance);
        commentText = findViewById(R.id.input_comment);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.input_loudness:
                switch (progress) {
                    case 0:
                        loudnessValueText.setText(R.string.loudnessvalue_0);
                        break;
                    case 1:
                        loudnessValueText.setText(R.string.loudnessvalue_1);
                        break;
                    case 2:
                        loudnessValueText.setText(R.string.loudnessvalue_2);
                        break;
                    case 3:
                        loudnessValueText.setText(R.string.loudnessvalue_3);
                        break;
                    default:
                }
                break;
            case R.id.input_eventlength:
                switch (progress) {
                    case 0:
                        eventLengthValueText.setText(R.string.eventlengthsvalue_0);
                        break;
                    case 1:
                        eventLengthValueText.setText(R.string.eventlengthsvalue_1);
                        break;
                    case 2:
                        eventLengthValueText.setText(R.string.eventlengthsvalue_2);
                        break;
                    case 3:
                        eventLengthValueText.setText(R.string.eventlengthsvalue_3);
                        break;
                    case 4:
                        eventLengthValueText.setText(R.string.eventlengthsvalue_4);
                        break;
                    default:
                }
                break;
            default:

        }


    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_formsend:
                if (checkInputs()) {
                    saveInputs();

                }
                findViewById(R.id.button_formsend).setEnabled(false);
                break;
            default:
        }
    }

    private void saveInputs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putString(Constants.FORM_TYPE, typeText.getText().toString());
        prefEditor.putString(Constants.FORM_LOCATION, locationText.getText().toString());
        prefEditor.putString(Constants.FORM_DISTANCE, distanceText.getText().toString());
        prefEditor.putString(Constants.FORM_LOUDNESS, String.valueOf(loudnessSeekBar.getProgress()));
        prefEditor.putString(Constants.FORM_EVENTLENGTH, String.valueOf(eventLengthSeekBar.getProgress()));
        switch (soundSysGroup.getCheckedRadioButtonId()) {
            case R.id.soundsys_0:
                prefEditor.putString(Constants.FORM_SOUNDSYS, "0");
                break;
            case R.id.soundsys_1:
                prefEditor.putString(Constants.FORM_SOUNDSYS, "1");
                break;
            case R.id.soundsys_2:
                prefEditor.putString(Constants.FORM_SOUNDSYS, "2");
                break;
            case R.id.soundsys_3:
                prefEditor.putString(Constants.FORM_SOUNDSYS, "3");
                break;
            default:
        }
        switch (targetAudGroup.getCheckedRadioButtonId()) {
            case R.id.input_audience1:
                prefEditor.putString(Constants.FORM_TARGETAUD, "0");
                break;
            case R.id.input_audience2:
                prefEditor.putString(Constants.FORM_TARGETAUD, "1");
                break;
            case R.id.input_audience3:
                prefEditor.putString(Constants.FORM_TARGETAUD, "2");
                break;
            case R.id.input_audience4:
                prefEditor.putString(Constants.FORM_TARGETAUD, "3");
                break;
            default:
        }
        prefEditor.putString(Constants.FORM_COMMENT, commentText.getText().toString());

        prefEditor.apply();
        saveToFile();
        Toast.makeText(getBaseContext(), R.string.form_sent, Toast.LENGTH_LONG).show();
    }

    private void saveToFile() {
        Thread fileWriterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                String startTime = prefs.getString(Constants.START_TIME, "0");
                File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/Zajszintmero/");
                dir.mkdirs();
                File file = new File(dir,startTime+"_info.csv");
                CSVWriter writer = null;
                try {
                    writer = new CSVWriter(new FileWriter(file));
                    List<String[]> data = new ArrayList<>();
                    data.add(new String[] {"type", prefs.getString(Constants.FORM_TYPE, "")});
                    data.add(new String[] {"location", prefs.getString(Constants.FORM_LOCATION, "")});
                    data.add(new String[] {"time", prefs.getString(Constants.FORM_TIME, "")});
                    data.add(new String[] {"spl_rms", prefs.getString(Constants.LAEQ_LAST, "")});
                    data.add(new String[] {"distance", prefs.getString(Constants.FORM_DISTANCE, "")});
                    data.add(new String[] {"phone", Constants.deviceUniqueID});
                    data.add(new String[] {"application", getString(R.string.app_name)});
                    data.add(new String[] {"loudness", prefs.getString(Constants.FORM_LOUDNESS, "")});
                    data.add(new String[] {"comment", prefs.getString(Constants.FORM_COMMENT, "")});
                    data.add(new String[] {"calibrated", (Constants.calibrationType.ordinal() > 0) ? "1" : "0"});
                    data.add(new String[] {"target_audience", prefs.getString(Constants.FORM_TARGETAUD, "0")});
                    data.add(new String[] {"event_duration", prefs.getString(Constants.FORM_EVENTLENGTH, "0")});
                    data.add(new String[] {"reinforcement_type", prefs.getString(Constants.FORM_SOUNDSYS, "0")});
                    writer.writeAll(data);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        writer.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                scheduleUpload();

                try {
                    Thread.sleep(2*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });
        fileWriterThread.start();
    }

    private void scheduleUpload() {
        JobScheduler jobScheduler =
                (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(new JobInfo.Builder(4,
                new ComponentName(getBaseContext(), UploadJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setRequiresCharging(false)
                .build());
    }

    private boolean checkInputs() {
        //TODO check string's length
        boolean result = true;
        if (typeText.getText().toString().isEmpty()
                || locationText.getText().toString().isEmpty()
                || distanceText.getText().toString().isEmpty()) {
            result = false;

        }
        if (soundSysGroup.getCheckedRadioButtonId() == -1) {
            result = false;
        }
        if (targetAudGroup.getCheckedRadioButtonId() == -1) {
            result = false;
        }
        if (!result) {
            Toast.makeText(this, R.string.form_not_filled, Toast.LENGTH_LONG).show();
        }
        return result;
    }
}
