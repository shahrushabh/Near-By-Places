package com.rushabhs.curiouspotato;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Favories extends AppCompatActivity {

    private ArrayList<String> placeNames = new ArrayList<>();
    private BitmapDbHelper bitmapDbHelper;
    private SQLiteDatabase sqLiteDatabase;
    private FavoriteListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favories);
        bitmapDbHelper = new BitmapDbHelper(this);
        new GetSavedPlacesTask().execute();
    }

    /**
     * AsyncTask to get place names from SQLite db. Handled in separate thread.
     */
    private class GetSavedPlacesTask extends AsyncTask<Void, Void, ArrayList<String>> {
        // Handle saving place in background.
        public GetSavedPlacesTask (){
            sqLiteDatabase = bitmapDbHelper.getReadableDatabase();
        }

        protected void onPreExecute(){
        }

        protected ArrayList<String> doInBackground(Void... v) {
            ArrayList<String> names = new ArrayList<>();
            Cursor c = sqLiteDatabase.rawQuery("SELECT DISTINCT " + BitmapDbHelper.KEY_NAME + " FROM " + BitmapDbHelper.DB_TABLE, null);
            c.moveToFirst();
            if (c.getCount() > 0) {
                do {
                    names.add(c.getString(c.getColumnIndex(BitmapDbHelper.KEY_NAME)));
                    Log.d("Name is", c.getString(c.getColumnIndex(BitmapDbHelper.KEY_NAME)));
                }while(c.moveToNext());
            }
            c.close();
            return names;
        }

        protected void onProgressUpdate(Void... progress) {
            // Nothing onProgress
        }

        protected void onPostExecute(ArrayList<String> result) {
            placeNames = result;
            Log.v("placeNames size", Integer.toString(placeNames.size()));
            ListView listView = (ListView) findViewById(R.id.list);
            adapter = new FavoriteListAdapter(Favories.this, R.layout.card_object_view, placeNames);
            listView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    }
}
