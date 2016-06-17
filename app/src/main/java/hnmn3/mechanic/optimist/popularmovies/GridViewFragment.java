package hnmn3.mechanic.optimist.popularmovies;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import adapter.GridItem;
import adapter.GridViewAdapter;
import data.MovieContract;

/**
 * Created by Manish Menaria on 09-Jun-16.
 */

public class GridViewFragment extends Fragment implements AdapterView.OnItemClickListener {

    View rootView;
    private static final String TAG = MainActivity.class.getSimpleName();
    private android.widget.GridView GridView;
    String baseUrl;
    TextView tvNoFavMovie;
    ProgressBar progressBarWait;
    private GridViewAdapter GridAdapter;
    private ArrayList<GridItem> GridData;
    JSONArray jsonArray;
    private Boolean mTablet = false;
    Typeface font;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.grid_view_fragment, container, false);

        tvNoFavMovie = (TextView) rootView.findViewById(R.id.tvNoFavoriteMovies);
        GridData = new ArrayList<>();
        GridView = (GridView) rootView.findViewById(R.id.gridView);
        GridView.setOnItemClickListener(this);
        GridAdapter = new GridViewAdapter(getContext(), R.layout.gridview_item, GridData);
        GridView.setAdapter(GridAdapter);
        progressBarWait = (ProgressBar) rootView.findViewById(R.id.progressBarWait);
        font = Typeface.createFromAsset(getContext().getAssets(), "fonts/myfont.ttf");

        int densityDpi = getResources().getDisplayMetrics().densityDpi;
        if (densityDpi <= DisplayMetrics.DENSITY_MEDIUM) {
            baseUrl = "http://image.tmdb.org/t/p/w185/";
        } else if (densityDpi <= DisplayMetrics.DENSITY_HIGH) {
            baseUrl = "http://image.tmdb.org/t/p/w185/";
        } else if (densityDpi <= DisplayMetrics.DENSITY_XHIGH) {
            baseUrl = "http://image.tmdb.org/t/p/w342/";
        } else if (densityDpi <= DisplayMetrics.DENSITY_XXHIGH) {
            baseUrl = "http://image.tmdb.org/t/p/w500/";
        } else {
            baseUrl = "http://image.tmdb.org/t/p/w780/";
        }

        updateMovies();

        return rootView;
    }


    private void GetMoviesDataFromJson(String jsonString) {
        GridData = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            jsonArray = jsonObject.getJSONArray("results");
            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                String url = baseUrl + jsonObject.getString("poster_path");
                String overview = jsonObject.getString("overview");
                String release_date = jsonObject.getString("release_date");
                String original_title = jsonObject.getString("original_title");
                String vote_average = jsonObject.getString("vote_average");
                String id = jsonObject.getString("id");
                System.out.print("id=" + id);
                GridItem item = new GridItem(url, overview, release_date, original_title, vote_average, id);
                GridData.add(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void GetMoviesDataFromContentProvider() {
        GridData = new ArrayList<>();
        Cursor cursor=null;
        Uri uri = Uri.parse(MovieContract.BASE_CONTENT_URI + "/" + MovieContract.FavoriteTableContents.TABLE_NAME + "/all");

        try {
            cursor = getContext().getContentResolver().query(uri, null, null, null, null);
        }catch (IllegalArgumentException e){

        }
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String url = cursor.getString(3);
                String overview = cursor.getString(2);
                String release_date = cursor.getString(5);
                String original_title = cursor.getString(1);
                String vote_average = cursor.getString(4);
                String id = cursor.getInt(0) + "";
                GridItem item = new GridItem(url, overview, release_date, original_title, vote_average, id);
                GridData.add(item);
            } while (cursor.moveToNext());

            if (progressBarWait != null)
                progressBarWait.setVisibility(View.GONE);
            GridAdapter.setGridData(GridData);
        } else {
            //Toast.makeText(getContext(),"You have no favorite movies",Toast.LENGTH_LONG).show();
            if (progressBarWait != null)
                progressBarWait.setVisibility(View.GONE);
            tvNoFavMovie.setTypeface(font);
            GridView.setVisibility(View.GONE);
            tvNoFavMovie.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Toast.makeText(getActivity(), GridData.get(position).getoriginal_title(), Toast.LENGTH_SHORT).show();

        mTablet = ((MainActivity) getActivity()).isTablet();
        if (!mTablet) {
            Intent i = new Intent(getActivity(), MovieDetails_Activity.class);
            i.putExtra("getoriginal_title", GridData.get(position).getoriginal_title());
            i.putExtra("getoverview", GridData.get(position).getoverview());
            i.putExtra("getrelease_date", GridData.get(position).getrelease_date());
            i.putExtra("getURL", GridData.get(position).getURL());
            i.putExtra("getvote_average", GridData.get(position).getvote_average());
            i.putExtra("id", GridData.get(position).getId());
            startActivity(i);
        } else {
            ((MainActivity) getActivity()).replaceFragment(GridData.get(position).getoriginal_title(), GridData.get(position).getoverview(), GridData.get(position).getrelease_date()
                    , GridData.get(position).getURL(), GridData.get(position).getvote_average(), GridData.get(position).getId());
        }


    }

    private class GetMoviesInfo extends AsyncTask<String, Void, String> {


        HttpURLConnection conn;
        String s = "Sorry!! error occured while loading the data";

        @Override
        protected String doInBackground(String... params) {
            String result;
            String url_ = "http://api.themoviedb.org/3" + params[0];
            try {
                URL url = new URL(url_);


                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                //SharedPreferences userInfo = getSharedPreferences("mypref", 0);
                /*String urlParameters = "username="
                        + URLEncoder
                        .encode(userInfo.getString("username", "Null"),
                                "UTF-8");*/
                String urlParameters = "api_key="
                        + URLEncoder
                        .encode(BuildConfig.MOVIE_API_KEY,
                                "UTF-8");
                writer.write(urlParameters);
                writer.flush();
                writer.close();
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {

                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line + '\n');
                    }

                    String jsonString = stringBuilder.toString();
                    System.out.println(jsonString);
                    GetMoviesDataFromJson(jsonString);

                    return "Movies Data Loaded Sucessfully";

                } else {

                    return "Error occured while fatching data";

                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
                result = "Sorry!! error occured while loading the data";
            } catch (IOException e) {
                e.printStackTrace();
                result = "Sorry!! error occured while loading the data";
            } finally {
                conn.disconnect();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if (progressBarWait != null)
                progressBarWait.setVisibility(View.GONE);
            if (result.equals(s)) {
                Toast.makeText(getActivity(), "Sorry!! error occured while loading the data", Toast.LENGTH_SHORT).show();
            } else {
                GridAdapter.setGridData(GridData);
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        updateMovies();
    }

    void updateMovies() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        //Toast.makeText(getContext(), "pref ="+preferences.getString("filter","/movie/popular"), Toast.LENGTH_SHORT).show();
        String url_end = preferences.getString("filter", "/movie/popular");
        if (url_end.equals("favorite")) {
            //Toast.makeText(getContext(), "Here", Toast.LENGTH_SHORT).show();
            GetMoviesDataFromContentProvider();
        } else {
            new GetMoviesInfo().execute(url_end);
        }
    }

}
