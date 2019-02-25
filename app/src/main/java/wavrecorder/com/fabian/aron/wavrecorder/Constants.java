package wavrecorder.com.fabian.aron.wavrecorder;

import android.Manifest;

/**
 * Created by Aron Fabian on 2018. 03. 23..
 */

public class Constants {


    public interface ACTION {

        String STARTFOREGROUND_ACTION = "wavrecorder.com.fabian.aron.wavrecorder.startforeground";
        String STOPFOREGROUND_ACTION = "wavrecorder.com.fabian.aron.wavrecorder.stopforeground";
        String NOTIFSTOPFOREGROUND_ACTION = "wavrecorder.com.fabian.aron.wavrecorder.notifstopforeground";
        String DBA_DBC_BROADCAST_ACTION = "wavrecorder.com.fabian.aron.wavrecorder.dbadbc";
        String LAEQ_BROADCAST_ACTION = "wavrecorder.com.fabian.aron.wavrecorder.laeq";
        String SECBUTTON_ACTION = "wavrecorder.com.fabian.aron.wavrecorder.secbutton";
        String MILLISECBUTTON_ACTION = "wavrecorder.com.fabian.aron.wavrecorder.millisecbutton";
        String RECORDERSTOPPED_ACTION = "wavrecorder.com.fabian.aron.wavrecorder.recorderstopped";
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 1;
    }

    public static final int PERMISSION_ALL = 1;
    public static final String[] PERMISSIONS = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};

    public static String deviceUniqueID = null;
    public static String deviceModel = null;
    public static String deviceMarketName = null;
    public static CalibrationType calibrationType = CalibrationType.NOT_CALIBRATED;

    public interface MEASUREMENT_CLASS {
        String CLASS_ONE = "1";
        String CLASS_TWO = "2";
    }

    public static final int HPF = 0;
    public static final int LPF = 1;
    public static final int PARAMETRIC = 2;

    // SharedPreferences keys
    public static final String FORM_TYPE = "form_type";
    public static final String FORM_LOCATION = "form_location";
    public static final String FORM_TIME = "form_time";
    public static final String FORM_SPL = "form_spl";
    public static final String FORM_DISTANCE = "form_distance";
    public static final String FORM_LOUDNESS = "form_loudness";
    public static final String FORM_COMMENT = "form_comment";
    public static final String FORM_EVENTLENGTH = "form_eventlength";
    public static final String FORM_SOUNDSYS = "form_soundsys";
    public static final String FORM_TARGETAUD = "form_targetaud";
    public static final String LAEQ_HISTORY = "laeq_history";
    public static final String LAEQ_LAST = "laeq_last";
    public static final String CALIBTYPE = "calibration_type";

    public static String fileName = "";

    public static final String CHANNEL_ID = "notification_channel";


}
