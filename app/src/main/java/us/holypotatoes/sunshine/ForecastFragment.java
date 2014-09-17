package us.holypotatoes.sunshine;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by ben on 9/13/14.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    //on create happens before oncreateview, this will call sethasoptionsmenu with param true
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateWeather() {
        FetchWeatherTask task = new FetchWeatherTask();
        String location = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        task.execute(location);
    }

    //fragment is inflated below
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        /**
         * Begin array adapter / listview setup
         */

        //activity to pass as a context in the arrayadapter
        final Activity activity = getActivity();

        mForecastAdapter = new ArrayAdapter<String>(activity,
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                new ArrayList<String>());

        // this line was provided by android studio
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // get reference to the listview in fragment_main
        final ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);

        listView.setAdapter(mForecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent detailIntent = new Intent(activity, DetailActivity.class);
                detailIntent.putExtra(Intent.EXTRA_TEXT, mForecastAdapter.getItem(i));
                startActivity(detailIntent);
            }
        });

        return rootView;
    }

    /**
     * Begin http boilerplate code snippet
     */

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String TAG = FetchWeatherTask.class.getSimpleName();

        private String getReadableDateString(long time){
            //API returns a unix time from epoch timestamp, need to convert it
            //it needs to be converted to milliseconds before it can be turned into a proper date
            Date date = new Date(time * 1000);
            SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
            return format.format(date).toString();
        }

        private String formatHighLows(double high, double low) {
            //strip the decimal place from the returned temps
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }


        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays) throws JSONException {
            int i;
            String[] resultStrs = new String[numDays];

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DATETIME = "dt";
            final String OWM_DESCRIPTION = "main";

            JSONObject json = new JSONObject(forecastJsonStr);
            JSONArray jsonArray = json.getJSONArray(OWM_LIST);

            StringBuilder tempStr = new StringBuilder();

            for (i = 0; i < jsonArray.length(); i++) {
                json = jsonArray.getJSONObject(i);
                tempStr.append(getReadableDateString(json.getLong(OWM_DATETIME)));
                tempStr.append(" - ");
                double min = json.getJSONObject(OWM_TEMPERATURE).getDouble(OWM_MIN);
                double max = json.getJSONObject(OWM_TEMPERATURE).getDouble(OWM_MAX);
                JSONArray tempArray = json.getJSONArray(OWM_WEATHER);
                json = tempArray.getJSONObject(0);
                tempStr.append(json.getString(OWM_DESCRIPTION));
                tempStr.append(" - ");
                tempStr.append(formatHighLows(max, min));
                resultStrs[i] = tempStr.toString();
                tempStr.delete(0,tempStr.length());
            }


            return resultStrs;
        }

        @Override
        protected String[] doInBackground(String... strings) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;


            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try

            {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                int i;
                Resources res = getResources();
                Uri.Builder builder = new Uri.Builder();

                String scheme = res.getString(R.string.url_scheme);
                String authority = res.getString(R.string.url_authority);
                String[] path = res.getStringArray(R.array.url_path);
                String[] params = res.getStringArray(R.array.url_params);
                String[] queries = res.getStringArray(R.array.url_queries);

                //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

                //params[0] = prefs.getString(getString(R.string.pref_location_key),
                //        getString(R.string.pref_location_default));

                String location = strings[0];
                params[0] = location;

                builder.scheme(scheme).authority(authority);
                for (i = 0; i < path.length; i++) {
                    builder.appendPath(path[i]);
                }

                for (i = 0; i < params.length; i++){
                    builder.appendQueryParameter(queries[i], params[i]);
                }

                String string_url = builder.toString();
                URL url = new URL(string_url);

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();
            } catch (
                    IOException e
                    )

            {
                Log.e(TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally

            {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                String [] weatherData = getWeatherDataFromJson(forecastJsonStr, 7);
                    return weatherData;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            return null;
            }

        @Override
        protected void onPostExecute(String[] strings) {
            super.onPostExecute(strings);
            mForecastAdapter.clear();
            mForecastAdapter.addAll(strings);
        }
    }
}

