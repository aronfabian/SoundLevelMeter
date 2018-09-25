package wavrecorder.com.fabian.aron.wavrecorder;


import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class FormActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    SeekBar loudnessSeekBar;
    TextView loudnessValueText;
    SeekBar eventLengthSeekBar;
    TextView eventLengthValueText;
    SeekBar soundSystemSeekBar;
    TextView soundSystemValueText;
    Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        loudnessSeekBar = (SeekBar) findViewById(R.id.input_loudness);
        loudnessSeekBar.setOnSeekBarChangeListener(this);
        loudnessValueText = (TextView) findViewById(R.id.text_loudnesvalue);
        eventLengthSeekBar = (SeekBar) findViewById(R.id.input_eventlength);
        eventLengthSeekBar.setOnSeekBarChangeListener(this);
        eventLengthValueText = (TextView) findViewById(R.id.text_eventlengthvalue);
        soundSystemSeekBar = (SeekBar) findViewById(R.id.input_soundsystem);
        soundSystemSeekBar.setOnSeekBarChangeListener(this);
        soundSystemValueText = (TextView) findViewById(R.id.text_soundsystemvalue);
        sendButton = (Button) findViewById(R.id.button_formsend);
        sendButton.setOnClickListener(this);

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch(seekBar.getId()){
            case R.id.input_loudness:
                switch(progress){
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
                switch(progress){
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
            case R.id.input_soundsystem:
                switch(progress){
                    case 0:
                        soundSystemValueText.setText(R.string.soundsyssvalue_0);
                        break;
                    case 1:
                        soundSystemValueText.setText(R.string.soundsyssvalue_1);
                        break;
                    case 2:
                        soundSystemValueText.setText(R.string.soundsyssvalue_2);
                        break;
                    case 3:
                        soundSystemValueText.setText(R.string.soundsyssvalue_3);
                        break;
                    default:
                }
                break;
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
        switch (v.getId()){
            case R.id.button_formsend:
                if(checkInputs()){
                    saveInputs();
                    JobScheduler jobScheduler =
                            (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
                    jobScheduler.schedule(new JobInfo.Builder(4,
                            new ComponentName(this, UploadJobService.class))
                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NOT_ROAMING)
                            .setRequiresCharging(false)
                            .build());
                }
                break;
            default:
        }
    }

    private void saveInputs() {
        //TODO bemeneti mezők elmentése sharedpreferences-be

    }

    private boolean checkInputs() {
        //TODO bementek ellenörzése: minden ki van-e töltve
        return true;
    }
}
