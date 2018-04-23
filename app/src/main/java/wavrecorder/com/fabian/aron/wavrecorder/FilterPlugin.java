package wavrecorder.com.fabian.aron.wavrecorder;

import android.content.Context;
import android.os.Build;

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
    public static void setFilters(Context context, String classType) {
        PhoneDB db = new PhoneDB(context);
        String filters = null;
        switch (Constants.calibrationType) {
            case NOT_CALIBRATED:
                return;
            case MODELLY_CALIBRATED:
                filters = db.getModelFilters(Build.MODEL, Constants.deviceMarketName);
                break;
            case UNIQUELY_CALIBRATED:
                filters = db.getUniqueFilters(Constants.deviceUniqueID);
                break;
            default:
                return;
        }
        JSONObject reader;
        try {
            reader = new JSONObject(filters);
            JSONObject classSelect;
            if (classType.equals(Constants.MEASUREMENT_CLASS.CLASS_ONE)) {
                classSelect = reader.getJSONObject("class1");
            } else {
                classSelect = reader.getJSONObject("class2");
            }
            for (int n = 0; n < 2; n++) {
                JSONObject weightingSelect;
                if (n == 0) {
                    weightingSelect = classSelect.getJSONObject("A");
                } else {
                    weightingSelect = classSelect.getJSONObject("C");
                }
                JSONArray parametric = weightingSelect.getJSONArray("parametric");
                for (int i = 0; i < parametric.length(); i++) {
                    JSONObject f = parametric.getJSONObject(i);
                    if (n == 0) {
                        RecorderService.filterNumA = addParametricFilterA(
                                Float.valueOf(f.getString("fc")),
                                Float.valueOf(f.getString("bw")),
                                Float.valueOf(f.getString("gain")));
                    } else {
                        RecorderService.filterNumC = addParametricFilterC(
                                Float.valueOf(f.getString("fc")),
                                Float.valueOf(f.getString("bw")),
                                Float.valueOf(f.getString("gain")));
                    }

//                    Log.i(LOG_TAG, "gain: " + f.getString("gain"));
//                    Log.i(LOG_TAG, "fc: " + f.getString("fc"));
//                    Log.i(LOG_TAG, "bw: " + f.getString("bw"));
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
