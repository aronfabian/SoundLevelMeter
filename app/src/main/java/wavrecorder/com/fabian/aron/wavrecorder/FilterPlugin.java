package wavrecorder.com.fabian.aron.wavrecorder;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Aron Fabian on 2018. 04. 17..
 */
public class FilterPlugin {

    static {
        System.loadLibrary("FilterProcess");
    }

    /**
     * Creates C++ object in order to do some signal processing using SuperpoweredSDK
     *
     * @param samplerate
     */
    public static native void filterProcessCreate(int samplerate);


    /**
     * Deletes FilterProcess instance.
     */
    public static native void filterProcessDelete();


    /**
     * Creates a parametric filter and add to the series.
     *
     * @param frequency   frequency in Hertz
     * @param octaveWidth bandwidth in octave
     * @param dbGain      gain in dB
     * @return number of filters in the series
     */
    public static native int addParametricFilterA(float frequency, float octaveWidth, float dbGain);


    /**
     * Creates a parametric filter and add to the series.
     *
     * @param filterType     frequency in Hertz
     * @param cutOffFreqency bandwidth in octave
     * @param resonance      gain in dB
     * @return number of filters in the series
     */
    public static native int addResonantFilterA(int filterType, float cutOffFreqency, float resonance);

    /**
     * Process the input data with the predefined filters
     *
     * @param input           input buffer
     * @param output          output buffer (can be the same as input)
     * @param numberOfSamples number of samples
     */
    public static native void filterProcessingA(float[] input, float[] output, int numberOfSamples);

    /**
     * Creates a parametric filter and add to the series.
     *
     * @param frequency   frequency in Hertz
     * @param octaveWidth bandwidth in octave
     * @param dbGain      gain in dB
     * @return number of filters in the series
     */
    public static native int addParametricFilterC(float frequency, float octaveWidth, float dbGain);

    /**
     * Creates a parametric filter and add to the series.
     *
     * @param filterType     frequency in Hertz
     * @param cutOffFreqency bandwidth in octave
     * @param resonance      gain in dB
     * @return number of filters in the series
     */
    public static native int addResonantFilterC(int filterType, float cutOffFreqency, float resonance);

    /**
     * Process the input data with the predefined filters
     *
     * @param input           input buffer
     * @param output          output buffer (can be the same as input)
     * @param numberOfSamples number of samples
     */

    public static native void filterProcessingC(float[] input, float[] output, int numberOfSamples);

    /**
     * Gets filter information from the DB and setting up the filter series.
     *
     * @param context   DB context
     * @param classType "1" = Class1 or "2" = Class2
     */
    public static void setFiltersFromPref(Context context, String classType) {
        String filters = null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            JSONObject classSelect;
            if (classType.equals(Constants.MEASUREMENT_CLASS.CLASS_ONE)) {
                classSelect = new JSONObject(prefs.getString("classOneCalibData",null));
            } else {
                classSelect = new JSONObject(prefs.getString("classTwoCalibData",null));
            }
            JSONObject weightingSelect = classSelect.getJSONObject("A_weighted");
            JSONArray parametric = weightingSelect.getJSONArray("Parametric");
            for (int i = 0; i < parametric.length(); i++) {
                JSONObject f = parametric.getJSONObject(i);
                RecorderService.filterNumA = addParametricFilterA(
                        Float.valueOf(f.getString("fc")),
                        Float.valueOf(f.getString("bandwidth")),
                        Float.valueOf(f.getString("gain")));
            }
            JSONArray hpf = weightingSelect.getJSONArray("HPF");
            for (int i = 0; i < hpf.length(); i++) {
                JSONObject f = hpf.getJSONObject(i);
                RecorderService.filterNumA = addResonantFilterA(
                        0,
                        Float.valueOf(f.getString("fc")),
                        (float)(1.0 / Math.sqrt(2.0) / 10.0));
            }
            JSONArray lpf = weightingSelect.getJSONArray("LPF");
            for (int i = 0; i < lpf.length(); i++) {
                JSONObject f = lpf.getJSONObject(i);
                RecorderService.filterNumA = addResonantFilterA(
                        1,
                        Float.valueOf(f.getString("fc")),
                        (float)(1.0 / Math.sqrt(2.0) / 10.0));
            }

            weightingSelect = classSelect.getJSONObject("C_weighted");
            parametric = weightingSelect.getJSONArray("Parametric");
            for (int i = 0; i < parametric.length(); i++) {
                JSONObject f = parametric.getJSONObject(i);
                RecorderService.filterNumA = addParametricFilterC(
                        Float.valueOf(f.getString("fc")),
                        Float.valueOf(f.getString("bandwidth")),
                        Float.valueOf(f.getString("gain")));
            }
            hpf = weightingSelect.getJSONArray("HPF");
            for (int i = 0; i < hpf.length(); i++) {
                JSONObject f = hpf.getJSONObject(i);
                RecorderService.filterNumA = addResonantFilterC(
                        0,
                        Float.valueOf(f.getString("fc")),
                        (float)(1.0 / Math.sqrt(2.0) / 10.0));
            }
            lpf = weightingSelect.getJSONArray("LPF");
            for (int i = 0; i < lpf.length(); i++) {
                JSONObject f = lpf.getJSONObject(i);
                RecorderService.filterNumA = addResonantFilterC(
                        1,
                        Float.valueOf(f.getString("fc")),
                        (float)(1.0 / Math.sqrt(2.0) / 10.0));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
