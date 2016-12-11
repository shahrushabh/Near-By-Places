package com.rushabhs.curiouspotato;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.google.android.gms.location.places.Place;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by rushabhs on 12/10/16.
 */

public class FavoriteListAdapter extends ArrayAdapter<String> {
    private List<String> places = new ArrayList<>();
    private Context context = null;
    private BitmapDbHelper bitmapDbHelper;
    private SQLiteDatabase sqLiteDatabase;

    public FavoriteListAdapter(Context context, int resource, List<String> places) {
        super(context, resource, places);
        this.context = context;
        this.places = places;
        bitmapDbHelper = new BitmapDbHelper(context);
    }

    @Override
    public int getCount() {
        return places.size();
    }

    @Override
    public String getItem(int i) {
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
        new GetSavedImageTask(imageView).execute(getItem(i));

        imageButton.setImageResource(R.drawable.saved);
        textView.setText(getItem(i));
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
     * AsyncTask to get place names from SQLite db. Handled in separate thread.
     */
    private class GetSavedImageTask extends AsyncTask<String, Void, Bitmap> {
        // Handle saving place in background.
        private String placeName;
        private ImageView imageView;

        public GetSavedImageTask (ImageView imageView){
            sqLiteDatabase = bitmapDbHelper.getReadableDatabase();
            this.imageView = imageView;
        }

        protected void onPreExecute(){
        }

        protected Bitmap doInBackground(String... v) {
            if(v.length < 0){return null;}
            placeName = v[0];
            // LOAD BITMAP FROM INTERNAL STORAGE
            Bitmap bitmap = null;
            FileInputStream inputStream;
            try {
                inputStream = context.openFileInput(placeName);
                bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            }
            catch (FileNotFoundException e) {
                Log.e("ERROR " , "file not found");
                e.printStackTrace();
            }
            catch (IOException e) {
                Log.e("ERROR ", "io exception");
                e.printStackTrace();
            }
            return bitmap;
        }

        protected void onProgressUpdate(Void... progress) {
            // Nothing onProgress
        }

        protected void onPostExecute(Bitmap result) {
            if(result != null){
                // Update the icon in for this view.
                (imageView).setImageBitmap(result);
            }else{
                // Image not found or could not be decoded, set it to default.
                Drawable res = ContextCompat.getDrawable(context, R.drawable.noimage);
                imageView.setImageDrawable(res);
            }

        }
    }
}
