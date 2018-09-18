package wavrecorder.com.fabian.aron.wavrecorder;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;


/**
 * Created by Aron Fabian on 2018. 04. 16..
 */
public class SettingsFragment extends PreferenceFragmentCompat {
    private SharedPreferences prefs;
    SharedPreferences.OnSharedPreferenceChangeListener listener;

    public SettingsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.app_preferences, rootKey);

        prefs = getPreferenceManager().getSharedPreferences();
        listener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                        switch (key) {
                            case "class_type":
                                RecorderService.classType = prefs.getString("class_type", Constants.MEASUREMENT_CLASS.CLASS_ONE);
                                break;
                            case "rms_time":
                                String rmsTime = prefs.getString("rms_time", "sec");
                                RecorderService.setRmsUpdateTime(rmsTime);
                                break;
//                            case "calibration":
//                                RecorderService.saveFile = prefs.getBoolean("calibration", false);
//                                break;
                            default:
                                break;
                        }
                    }
                };

        prefs.registerOnSharedPreferenceChangeListener(listener);
    }


    @Override
    public void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onResume() {
        super.onResume();
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }
}
