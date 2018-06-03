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


}
