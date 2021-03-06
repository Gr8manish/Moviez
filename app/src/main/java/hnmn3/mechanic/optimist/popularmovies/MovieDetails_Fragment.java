package hnmn3.mechanic.optimist.popularmovies;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.vstechlab.easyfonts.EasyFonts;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import Trailer.Trailer;
import data.MovieContract;
import review.Review;
import review.ReviewAdapter;

/**
 * Created by Manish Menaria on 10-Jun-16.
 */
public class MovieDetails_Fragment extends Fragment implements View.OnClickListener {

    GetReviewAndTrailers getReviewAndTrailersAsyncTask;
    View rootView;
    TextView tvReleaseDate, tvRating, tvOverview, noReviewAvailable;
    String id, vote_average, release_date, poster_path, overview, original_title;
    String path;
    ImageView imageView;
    LinearLayout trailerLayout;
    ProgressBar pBar;
    private List<Review> reviewList = new ArrayList<>();
    private List<Trailer> trailerList = new ArrayList<>();
    private RecyclerView recyclerView;
    private ReviewAdapter mAdapter;
    private FloatingActionButton floatingActionButton;
    private List<Integer> trailerIdList = new ArrayList<Integer>();
    Boolean isFavorite = false;
    ScrollView scrollViewDetailView;
    TextView tvNoMovieSelected;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.detail_fragment, container, false);
        if (savedInstanceState == null) {
            scrollViewDetailView=(ScrollView) rootView.findViewById(R.id.scrollViewDetailView);
            tvNoMovieSelected = (TextView) rootView.findViewById(R.id.tvNoMovieSelected);

            tvReleaseDate = (TextView) rootView.findViewById(R.id.tvReleaseDate);
            tvRating = (TextView) rootView.findViewById(R.id.tvRating);
            noReviewAvailable = (TextView) rootView.findViewById(R.id.tvNoReviewAvailbale);
            tvOverview = (TextView) rootView.findViewById(R.id.tvOverview);
            imageView = (ImageView) rootView.findViewById(R.id.ivPoster);
            pBar = (ProgressBar) rootView.findViewById(R.id.progressBarReview);
            trailerLayout = (LinearLayout) rootView.findViewById(R.id.linearLayoutYoutube);
            setRetainInstance(true);

            setTypeFaceTv();

            //Is favorite
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            String url_end = preferences.getString("filter", "/movie/popular");
            if (url_end.equals("favorite")) {
                isFavorite = true;
            }

            if (getArguments() != null) {
                Bundle bundle = getArguments();
                id = bundle.getString("id");
                vote_average = bundle.getString("getvote_average");
                release_date = bundle.getString("getrelease_date");
                //poster_path,
                overview = bundle.getString("getoverview");
                original_title = bundle.getString("getoriginal_title");


                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(original_title);
                tvReleaseDate.setText(release_date);
                tvRating.setText(vote_average + "/10");
                tvOverview.setText(overview);
                //Toast.makeText(getContext(), "Saved id=" + id, Toast.LENGTH_SHORT).show();

                if (isFavorite) {
                    load(bundle.getString("getURL"), id);

                } else {
                    Picasso
                            .with(getContext())
                            .load(bundle.getString("getURL"))
                            .into(imageView);
                }
            }


            recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);

            mAdapter = new ReviewAdapter(reviewList);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
            recyclerView.setLayoutManager(mLayoutManager);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(mAdapter);

            floatingActionButton = (FloatingActionButton) rootView.findViewById(R.id.viewFloatingButton);
            floatingActionButton.setOnClickListener(this);

            Uri uri = Uri.parse(MovieContract.BASE_CONTENT_URI + "/" + MovieContract.FavoriteTableContents.TABLE_NAME + "/check");
            String[] selectionArgs = {id};
            Cursor cursor=null;
            try {
                cursor = getContext().getContentResolver().query(uri, null, null, selectionArgs, null);
                scrollViewDetailView.setVisibility(View.VISIBLE);
                floatingActionButton.setVisibility(View.VISIBLE);
                tvNoMovieSelected.setVisibility(View.GONE);
            }catch (IllegalArgumentException e){
                Toast.makeText(getActivity(), "Toast1", Toast.LENGTH_SHORT).show();
            }


            if (isFavorite || (cursor != null && cursor.moveToFirst())) {
                floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(
                        getResources().getColor(R.color.favorite)
                ));
                fatchReviewNTrailerDataFromContentProvider();

            } else {
                getReviewAndTrailersAsyncTask = new GetReviewAndTrailers();
                getReviewAndTrailersAsyncTask.execute(id);
            }
        }
        return rootView;
    }

    private void setTypeFaceTv() {
        Typeface myfont = Typeface.createFromAsset(getContext().getAssets(), "fonts/myfont.ttf");
        TextView tvReleaseDatetitle, tvRatingTitle, tvOverviewTitle, tvTrailerTitle, tvReviewTitle, tvReview;
        tvReleaseDatetitle = (TextView) rootView.findViewById(R.id.tvReleaseDatetitle);
        tvRatingTitle = (TextView) rootView.findViewById(R.id.tvRatingTitle);
        tvOverviewTitle = (TextView) rootView.findViewById(R.id.tvOverviewTitle);
        tvTrailerTitle = (TextView) rootView.findViewById(R.id.tvTrailerTitle);
        tvReviewTitle = (TextView) rootView.findViewById(R.id.tvReviewTitle);

        tvReleaseDatetitle.setTypeface(myfont);
        tvRatingTitle.setTypeface(myfont);
        tvOverviewTitle.setTypeface(myfont);
        tvTrailerTitle.setTypeface(myfont);
        tvReviewTitle.setTypeface(myfont);
        tvNoMovieSelected.setTypeface(myfont);

        tvReleaseDate.setTypeface(EasyFonts.droidSerifBold(getContext()));
        tvRating.setTypeface(EasyFonts.droidSerifBold(getContext()));
        tvOverview.setTypeface(EasyFonts.droidSerifBold(getContext()));
    }

    public void load(String path, String id) {
        try {
            File f = new File(path, id + ".jpg");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            imageView.setImageBitmap(b);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void load(String path, String id, String source, ImageView imageView) {
        try {
            File f = new File(path, source + id + ".jpg");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            imageView.setImageBitmap(b);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.viewFloatingButton:
                Uri uri = Uri.parse(MovieContract.BASE_CONTENT_URI + "/" + MovieContract.FavoriteTableContents.TABLE_NAME + "/check");
                String[] selectionArgs = {id};
                Cursor cursor=null;
                try {
                     cursor= getContext().getContentResolver().query(uri, null, null, selectionArgs, null);
                }catch (IllegalArgumentException e){
                    Toast.makeText(getActivity(), "Toast2", Toast.LENGTH_SHORT).show();
                }


                if (cursor.moveToFirst()) {
                    cursor.close();
                    String[] args = {id};
                    uri = Uri.parse(MovieContract.BASE_CONTENT_URI + "/" + MovieContract.FavoriteTableContents.TABLE_NAME);
                    getContext().getContentResolver().delete(uri,
                            MovieContract.FavoriteTableContents._ID + " = ? ", args
                    );
                    uri = Uri.parse(MovieContract.BASE_CONTENT_URI + "/" + MovieContract.TrailerTableContent.TABLE_NAME);
                    getContext().getContentResolver().delete(uri,
                            MovieContract.TrailerTableContent.COLUMN_movie_id + " = ? ", args);
                    uri = Uri.parse(MovieContract.BASE_CONTENT_URI + "/" + MovieContract.ReviewTableContent.TABLE_NAME);
                    getContext().getContentResolver().delete(uri,
                            MovieContract.ReviewTableContent.COLUMN_movie_id + " = ? ", args);
                    Snackbar.make(v, "Removed from favorite movie", Snackbar.LENGTH_LONG).show();
                    floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(
                            getResources().getColor(R.color.not_favorite)
                    ));
                } else {

                    try {
                        save("", imageView);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error occured while saving the Movie Poster!!", Toast.LENGTH_SHORT).show();
                    }

                    //Favorite movie insertion in db
                    ContentValues values = new ContentValues();
                    values.put(MovieContract.FavoriteTableContents.COLUMN_original_title, original_title);
                    values.put(MovieContract.FavoriteTableContents.COLUMN_overview, overview);
                    values.put(MovieContract.FavoriteTableContents.COLUMN_poster_path, poster_path);
                    values.put(MovieContract.FavoriteTableContents.COLUMN_release_date, release_date);
                    values.put(MovieContract.FavoriteTableContents.COLUMN_vote_average, vote_average);
                    values.put(MovieContract.FavoriteTableContents._ID, id);

                    uri = Uri.parse(MovieContract.BASE_CONTENT_URI + "/" + MovieContract.FavoriteTableContents.TABLE_NAME);

                    getContext().getContentResolver().insert(uri, values);

                    //Review insertion in db
                    values = new ContentValues();
                    Review review;
                    uri = Uri.parse(MovieContract.BASE_CONTENT_URI + "/" + MovieContract.ReviewTableContent.TABLE_NAME);
                    for (int i = 0; i < reviewList.size(); i++) {
                        review = reviewList.get(i);
                        values.put(MovieContract.ReviewTableContent.COLUMN_author, review.getAuthor());
                        values.put(MovieContract.ReviewTableContent.COLUMN_review, review.getReview());
                        values.put(MovieContract.ReviewTableContent.COLUMN_movie_id, id);
                        getContext().getContentResolver().insert(uri, values);
                    }

                    //trailer insertion in Table
                    values = new ContentValues();
                    uri = Uri.parse(MovieContract.BASE_CONTENT_URI + "/" + MovieContract.TrailerTableContent.TABLE_NAME);
                    Trailer trailer;

                    //Toast.makeText(getContext(), "trailerListsize=" + trailerList.size(), Toast.LENGTH_SHORT).show();
                    for (int i = 0; i < trailerList.size(); i++) {
                        values = new ContentValues();
                        trailer = trailerList.get(i);
                        values.put(MovieContract.TrailerTableContent.COLUMN_movie_id, id);
                        values.put(MovieContract.TrailerTableContent.COLUMN_Internet_Source, trailer.getSource());
                        try {
                            ImageView imageView = (ImageView) rootView.findViewById(trailerIdList.get(i));
                            save(trailer.getSource(), imageView);
                            values.put(MovieContract.TrailerTableContent.COLUMN_source, poster_path);
                            values.put(MovieContract.TrailerTableContent.COLUMN_Trailer_name, trailer.getName());
                            getContext().getContentResolver().insert(uri, values);
                        } catch (ClassCastException e) {
                            Snackbar.make(v, "Trailers are loading , Cant make it favorite", Snackbar.LENGTH_LONG).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                    Snackbar.make(v, "Added to favorite movies", Snackbar.LENGTH_LONG).show();
                    floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(
                            getResources().getColor(R.color.favorite)
                    ));
                }
                break;
        }
    }

    public void save(String s, ImageView v) throws IOException, ClassCastException {

        FileOutputStream fos = null;

        try {
            Bitmap bitmap = ((BitmapDrawable) v.getDrawable()).getBitmap();
            ContextWrapper cw = new ContextWrapper(getContext());
            // path to /data/data/yourapp/app_data/imageDir
            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
            // Create imageDir
            File mypath = new File(directory, s + id + ".jpg");


            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            poster_path = directory.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null)
                fos.close();
        }


    }

    private class GetReviewAndTrailers extends AsyncTask<String, Void, String> {


        HttpURLConnection conn;
        String result, s = "Sorry!! error occured while loading the data";

        @Override
        protected String doInBackground(String... params) {
            String url_ = "http://api.themoviedb.org/3/movie/" + params[0] + "?api_key=" + BuildConfig.MOVIE_API_KEY + "&append_to_response=trailers,reviews";
            try {
                URL url = new URL(url_);


                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.connect();

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
                    fatchReviewNTrailerDataFromJSON(jsonString);

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
            pBar.setVisibility(View.GONE);
            if (result.equals(s)) {
                Toast.makeText(getActivity(), "Sorry!! error occured while loading the data", Toast.LENGTH_SHORT).show();
            } else {
                if (reviewList.size() == 0) {
                    noReviewAvailable.setVisibility(View.VISIBLE);
                }
                mAdapter.notifyDataSetChanged();
                addTailersTolayout();
            }
            //GridAdapter.setGridData(GridData);
        }

    }

    @Override
    public void onStop() {
        if (getReviewAndTrailersAsyncTask != null && getReviewAndTrailersAsyncTask.getStatus() != AsyncTask.Status.FINISHED) {
            getReviewAndTrailersAsyncTask.cancel(false);
        }
        super.onStop();
    }

    private void addTailersTolayout() {
        trailerLayout.setPadding(5, 10, 5, 0);
        if (trailerList.size() > 0) {
            Random rand = new Random();
            trailerIdList = new ArrayList<>();
            int randomNum;
            for (int i = 0; i < trailerList.size(); i++) {
                final String source = trailerList.get(i).getSource();
                String url = "http://img.youtube.com/vi/" + source + "/mqdefault.jpg";
                ImageView myImage = new ImageView(getContext());
                randomNum = 1000 + rand.nextInt((8888888 - 1000) + 1);
                trailerIdList.add(randomNum);
                myImage.setId(randomNum);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(1, 1, 1, 1);
                myImage.setLayoutParams(params);
                myImage.setAdjustViewBounds(true);
                myImage.setScaleType(ImageView.ScaleType.FIT_START);
                if (isFavorite) {
                    load(path, id, source, myImage);
                } else {
                    Picasso.with(getContext())
                            .load(url)
                            .placeholder(R.drawable.progress_animation)
                            .into(myImage);
                }

                trailerLayout.addView(myImage);
                myImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        youtubeIntent(source);
                    }
                });
            }
        } else {
            TextView errorMsg = new TextView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            errorMsg.setLayoutParams(params);
            errorMsg.setText("Sorry , No trailers are available for this movie");
        }
    }

    private void youtubeIntent(String source) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + source));
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://www.youtube.com/watch?v=" + source));
            startActivity(intent);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void fatchReviewNTrailerDataFromContentProvider() {
        Review review;
        Trailer trailer;
        reviewList.clear();
        Cursor cursor = null;
        Uri uri = Uri.parse(MovieContract.BASE_CONTENT_URI + "/" + MovieContract.ReviewTableContent.TABLE_NAME);
        String[] args = {id};
        try {
            cursor = getContext().getContentResolver().query(uri, null, null, args, null);
        }catch (IllegalArgumentException e){
            Toast.makeText(getActivity(), "Toast3", Toast.LENGTH_SHORT).show();
        }
        if (cursor != null && cursor.moveToFirst()) {
            do {
                review = new Review();
                review.setAuthor(cursor.getString(1));
                review.setReview(cursor.getString(2));
                reviewList.add(review);
            } while (cursor.moveToNext());

        } else {
            noReviewAvailable.setVisibility(View.VISIBLE);
        }

        uri = Uri.parse(MovieContract.BASE_CONTENT_URI + "/" + MovieContract.TrailerTableContent.TABLE_NAME);
        try {
            cursor = getContext().getContentResolver().query(uri, null, null, args, null);
        }catch (IllegalArgumentException e){
            Toast.makeText(getActivity(), "Toast4", Toast.LENGTH_SHORT).show();
        }

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(2);
                String size = "dummy";
                String source = cursor.getString(3);
                path = cursor.getString(1);
                String type = "dummy";
                trailer = new Trailer(name, size, source, type);
                trailerList.add(trailer);
            } while (cursor.moveToNext());

        } else {
            noReviewAvailable.setVisibility(View.VISIBLE);
        }

        mAdapter.notifyDataSetChanged();
        pBar.setVisibility(View.GONE);
        addTailersTolayout();
    }

    private void fatchReviewNTrailerDataFromJSON(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONObject jsonObjectreviews = jsonObject.getJSONObject("reviews");
            JSONArray jsonArray = jsonObjectreviews.getJSONArray("results");
            Review review;
            Trailer trailer;
            reviewList.clear();

            for (int i = 0; i < jsonArray.length(); i++) {
                review = new Review();
                jsonObjectreviews = jsonArray.getJSONObject(i);
                review.setAuthor(jsonObjectreviews.getString("author"));
                review.setReview(jsonObjectreviews.getString("content"));
                reviewList.add(review);
            }


            JSONObject jsonObjectTrailer = jsonObject.getJSONObject("trailers");
            jsonArray = jsonObjectTrailer.getJSONArray("youtube");

            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObjectTrailer = jsonArray.getJSONObject(i);
                String name = jsonObjectTrailer.getString("name");
                String size = jsonObjectTrailer.getString("size");
                String source = jsonObjectTrailer.getString("source");
                String type = jsonObjectTrailer.getString("type");
                trailer = new Trailer(name, size, source, type);
                trailerList.add(trailer);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}
