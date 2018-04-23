package wavrecorder.com.fabian.aron.wavrecorder;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;


/**
 * Created by Aron Fabian on 2018. 04. 04..
 */
public class PhoneDB extends SQLiteAssetHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "PhoneFilter.db";
    public static final String TABLE_NAME = "phones";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_MARKETNAME = "market_name";
    public static final String COLUMN_MODEL = "model";
    public static final String COLUMN_IMEI = "imei";
    public static final String COLUMN_FILTERS = "filters";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    public PhoneDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public String getModelFilters(String model, String marketName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res;
        if (marketName == null) {
            res = db.rawQuery("select * from " + TABLE_NAME + " where model=?", new String[]{model});
        } else {
            res = db.rawQuery("select * from " + TABLE_NAME + " where market_name='" + marketName + "'", null);
        }
        res.moveToFirst();
        String filters = res.getString(res.getColumnIndex(COLUMN_FILTERS));
        res.close();
        return filters;
    }

    public String getUniqueFilters(String imei) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_NAME + " where imei=?", new String[]{imei});
        res.moveToFirst();
        String filters = res.getString(res.getColumnIndex(COLUMN_FILTERS));
        res.close();
        return filters;
    }


    public String getCalibType(String model, String imei, String marketName) {
        String calibType;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor imeiRes = db.rawQuery("select * from " + TABLE_NAME + " where imei ='" + imei + "'", null);
        Cursor modelRes = db.rawQuery("select * from " + TABLE_NAME + " where model='" + model + "'", null);
        Cursor marketRes = db.rawQuery("select * from " + TABLE_NAME + " where market_name='" + marketName + "'", null);
        imeiRes.moveToFirst();
        modelRes.moveToFirst();
        marketRes.moveToFirst();
        int imeiRows = imeiRes.getCount();
        int modelRows = modelRes.getCount();
        int marketRows = marketRes.getCount();
        modelRes.close();
        imeiRes.close();

        if (imeiRows > 0) {
            calibType = "Uniquely Calibrated";
            Constants.calibrationType = CalibrationType.UNIQUELY_CALIBRATED;
        } else if (marketRows > 0) {
            calibType = "Modelly Calibrated";
            Constants.calibrationType = CalibrationType.MODELLY_CALIBRATED;
        } else if (modelRows > 0) {
            calibType = "Modelly Calibrated";
            Constants.calibrationType = CalibrationType.MODELLY_CALIBRATED;
        } else {
            calibType = "Not Calibrated";
            Constants.calibrationType = CalibrationType.NOT_CALIBRATED;
        }

        return calibType;
    }


}
