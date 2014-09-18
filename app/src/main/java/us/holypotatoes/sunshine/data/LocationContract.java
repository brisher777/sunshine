package us.holypotatoes.sunshine.data;

import android.provider.BaseColumns;

/**
 * Created by ben on 9/17/14.
 */
public class LocationContract {
    public static final class LocationEntry implements BaseColumns {
        public static final String TABLE_NAME = "location";

        //public static final String COLUMN_WEATHER_KEY =
        public static final String COLUMN_LOCATION_SETTING = "location_setting";
        public static final String COLUMN_CITY_NAME = "city_name";
        public static final String COLUMN_COORD_LAT = "coord_lat";
        public static final String COLUMN_COORD_LONG = "coord_long";
    }
}
