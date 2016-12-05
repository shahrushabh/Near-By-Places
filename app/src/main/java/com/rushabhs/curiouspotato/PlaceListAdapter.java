package com.rushabhs.curiouspotato;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlacePhotoMetadata;
import com.google.android.gms.location.places.PlacePhotoMetadataBuffer;
import com.google.android.gms.location.places.PlacePhotoMetadataResult;
import com.google.android.gms.location.places.Places;
import java.util.List;

/**
 * Created by Rushabh on 12/4/16.
 */

public class PlaceListAdapter extends ArrayAdapter<Place> {

    private List<Place> places = null;
    private Context context = null;

    public PlaceListAdapter(Context context, int resource, List<Place> places) {
        super(context, resource, places);
        this.context = context;
        this.places = places;
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
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.card_object_view, parent, false);
            imageView = (ImageView) convertView.findViewById(R.id.placeImage);
            textView = (TextView) convertView.findViewById(R.id.placeTitle);
            convertView.setTag(new ViewHolder(imageView, textView));
        } else {
            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            imageView = viewHolder.imageView;
            textView = viewHolder.textView;
        }

        textView.setText(getItem(i).getName());
        new GetPlacesTask(imageView, imageView.getMaxWidth(), imageView.getMaxHeight()).execute(getItem(i).getId());

        return convertView;
    }

    /**
     * View holder class for getView method that updates the car_view_object.
     */
    private static class ViewHolder {
        public final ImageView imageView;
        public final TextView textView;

        public ViewHolder(ImageView imageView, TextView textView) {
            this.imageView = imageView;
            this.textView = textView;
        }
    }

    /**
     * AsyncTask to fetch Image of the nearby place on a background thread.
     */
    private class GetPlacesTask extends AsyncTask<String, Void, GetPlacesTask.AttributedPhoto> {
        private GoogleApiClient mGoogleApiClient;
        private ImageView imageView;
        private int width;
        private int heigth;

        public GetPlacesTask(ImageView view, int width, int heigth){
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
            this.heigth = heigth;
        }

        protected AttributedPhoto doInBackground(String... p) {
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
                    if(image != null){Log.d("Test ", image.toString());}
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
}
