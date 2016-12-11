package com.rushabhs.curiouspotato;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlacePhotoMetadata;
import com.google.android.gms.location.places.PlacePhotoMetadataBuffer;
import com.google.android.gms.location.places.PlacePhotoMetadataResult;
import com.google.android.gms.location.places.Places;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by Rushabh on 12/4/16.
 */

public class PlaceListAdapter extends ArrayAdapter<Place> {

    private List<Place> places = null;
    private Context context = null;
    private BitmapDbHelper bitmapDbHelper;
    private SQLiteDatabase sqLiteDatabase;

    // Cache for Images so that list does not seem laggy.
    private LruCache<String, Bitmap> mMemoryCache;

    public PlaceListAdapter(Context context, int resource, List<Place> places) {
        super(context, resource, places);
        this.context = context;
        this.places = places;
        bitmapDbHelper = new BitmapDbHelper(context);

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    @Override
    public int getCount() {
        return places.size();
    }

    @Override
    public Place getItem(int i) {
        return places.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        ImageView imageView;
        TextView textView;
        ImageButton imageButton;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.card_object_view, parent, false);
            imageView = (ImageView) convertView.findViewById(R.id.placeImage);
            textView = (TextView) convertView.findViewById(R.id.placeTitle);
            imageButton = (ImageButton) convertView.findViewById(R.id.save);
            convertView.setTag(new ViewHolder(imageView, imageButton, textView));
        } else {
            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            imageView = viewHolder.imageView;
            textView = viewHolder.textView;
            imageButton = viewHolder.imageButton;
        }
        final Bitmap bitmap = getBitmapFromMemCache(getItem(i).getId());
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            // Default to no image, if not found.
            Drawable res = ContextCompat.getDrawable(context, R.drawable.noimage);
            imageView.setImageDrawable(res);
            imageButton.setImageResource(R.drawable.save);
            new GetPlacesTask(imageView, imageView.getMaxWidth(), imageView.getMaxHeight()).execute(getItem(i).getId());
        }

        textView.setText(getItem(i).getName());

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Save this image to SQLite.
                new SavePlacesTask(((ViewGroup) view.getParent().getParent())).execute();
            }
        });
        return convertView;
    }

    /**
     * View holder class for getView method that updates the car_view_object.
     */
    private static class ViewHolder {
        public final ImageView imageView;
        public final ImageButton imageButton;
        public final TextView textView;

        public ViewHolder(ImageView imageView, ImageButton imageButton, TextView textView) {
            this.imageView = imageView;
            this.textView = textView;
            this.imageButton = imageButton;
        }
    }

    /**
     * AsyncTask to fetch Image of the nearby place on a background thread.
     */
    private class GetPlacesTask extends AsyncTask<String, Void, GetPlacesTask.AttributedPhoto> {
        private GoogleApiClient mGoogleApiClient;
        private ImageView imageView;
        private int width;
        private int height;

        public GetPlacesTask(ImageView view, int width, int height){
            // Connect to Google api client
            mGoogleApiClient = new GoogleApiClient
                    .Builder(context)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
            this.imageView = view;
            this.width = width;
            this.height = height;
        }

        protected void onPreExecute(){
            // Default to no image, if not found.
            Drawable res = ContextCompat.getDrawable(context, R.drawable.noimage);
            imageView.setImageDrawable(res);
            Log.d("NOT Loaded from cache", "");
        }

        protected AttributedPhoto doInBackground(String... p) {
            if(isCancelled()){return null;}
            if(p.length != 1){return null;}
            String placeID = p[0];
            PlacePhotoMetadataResult result = Places.GeoDataApi.getPlacePhotos(mGoogleApiClient, placeID).await();
            AttributedPhoto attributedPhoto = null;

            if (result.getStatus().isSuccess()) {
                PlacePhotoMetadataBuffer photoMetadataBuffer = result.getPhotoMetadata();
                if (photoMetadataBuffer.getCount() > 0 && !isCancelled()) {
                    // Get the first bitmap and its attributions.
                    PlacePhotoMetadata photo = photoMetadataBuffer.get(0);
                    CharSequence attribution = photo.getAttributions();
                    // Load a scaled bitmap for this photo.
                    Bitmap image = photo.getPhoto(mGoogleApiClient).await().getBitmap();
                    if(image != null){
                        addBitmapToMemoryCache(placeID, image);
                    }
                    attributedPhoto = new AttributedPhoto(attribution, image);
                }
                // Release the PlacePhotoMetadataBuffer.
                photoMetadataBuffer.release();
            }

            return attributedPhoto;
        }

        protected void onProgressUpdate(Void... progress) {
            // Nothing onProgress
        }

        protected void onPostExecute(AttributedPhoto result) {
            if(result != null && result.bitmap != null){
                // Successfully downloaded the photo. Display it on the screen.
                imageView.setImageBitmap(result.bitmap);
            }
            mGoogleApiClient.disconnect();
        }

        /**
         * Holder for an image and its attribution.
         */
        class AttributedPhoto {

            public final CharSequence attribution;

            public final Bitmap bitmap;

            public AttributedPhoto(CharSequence attribution, Bitmap bitmap) {
                this.attribution = attribution;
                this.bitmap = bitmap;
            }
        }
    }

    /**
     * AsyncTask to add images to SQLite db. Handled in separate thread.
     */
    private class SavePlacesTask extends AsyncTask<Void, Void, Void> {
        // Handle saving place in background.
        private String placeName;
        private Bitmap imageBitmap;
        private View rootView;

        public SavePlacesTask(View rootView){
            sqLiteDatabase = bitmapDbHelper.getWritableDatabase();
            this.rootView = rootView;
            this.imageBitmap = ((BitmapDrawable) ((ImageView) rootView.findViewById(R.id.placeImage)).getDrawable()).getBitmap();
            this.placeName = ((TextView) rootView.findViewById(R.id.placeTitle)).getText().toString();
        }

        protected void onPreExecute(){
            ((ImageView) rootView.findViewById(R.id.save)).setImageResource(R.drawable.saved);
        }

        protected Void doInBackground(Void... v) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            ContentValues cv = new ContentValues();
            cv.put(BitmapDbHelper.KEY_NAME, placeName);
            sqLiteDatabase.insert(BitmapDbHelper.DB_TABLE, null, cv);

            // SAVE BITMAP TO INTERNAL STORAGE.
            FileOutputStream fos;
            try {
                fos = context.openFileOutput(placeName, Context.MODE_PRIVATE);
                imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            }
            catch (FileNotFoundException e) {
                Log.e("Error: ", "file not found");
                e.printStackTrace();
            }
            catch (IOException e) {
                Log.e("Error: ", "io exception");
                e.printStackTrace();
            }

            return null;
        }

        protected void onProgressUpdate(Void... progress) {
            // Nothing onProgress
        }

        protected void onPostExecute(Void result) {
            // Update the icon in for this view.
            String text = "Saved "  + placeName;
            Snackbar.make(rootView,text,Snackbar.LENGTH_SHORT).show();
        }
    }
}
