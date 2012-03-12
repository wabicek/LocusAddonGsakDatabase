package net.kuratkoo.locusaddon.gsakdatabase.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import menion.android.locus.addon.publiclib.DisplayData;
import menion.android.locus.addon.publiclib.PeriodicUpdate;
import menion.android.locus.addon.publiclib.PeriodicUpdate.UpdateContainer;
import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;
import menion.android.locus.addon.publiclib.geoData.PointsData;
import menion.android.locus.addon.publiclib.utils.RequiredVersionMissingException;
import net.kuratkoo.locusaddon.gsakdatabase.util.Gsak;

/**
 * LocationReceiver
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class LocationReceiver extends BroadcastReceiver {

    private static final String TAG = "LocusAddonGsakDatabase|LocationReceiver";
    private Context context;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        this.context = context;

        PeriodicUpdate pu = PeriodicUpdate.getInstance();
        pu.setLocNotificationLimit(50.0);
        pu.onReceive(context, intent, new PeriodicUpdate.OnUpdate() {

            public void onUpdate(UpdateContainer update) {
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("livemap", false)) {
                    if ((update.newMapCenter || update.newZoomLevel) && update.mapVisible) {
                        Log.d(TAG, "Live map update");
                        new MapLoadAsyncTask().execute(update);
                    }
                }
            }

            public void onIncorrectData() {
            }
        });
    }

    private class MapLoadAsyncTask extends AsyncTask<UpdateContainer, Integer, Exception> {

        private PointsData pd;
        private SQLiteDatabase db;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            db = SQLiteDatabase.openDatabase(PreferenceManager.getDefaultSharedPreferences(context).getString("db", ""), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        }

        @Override
        protected Exception doInBackground(UpdateContainer... updateSet) {
            try {
                UpdateContainer update = updateSet[0];
                pd = new PointsData("Livemap data");

                String[] cond = new String[]{
                    String.valueOf(update.mapBottomRight.getLatitude()),
                    String.valueOf(update.mapTopLeft.getLatitude()),
                    String.valueOf(update.mapTopLeft.getLongitude()),
                    String.valueOf(update.mapBottomRight.getLongitude())
                };

                String sql = "SELECT Code, Name, Latitude, Longitude, CacheType, HasCorrected, PlacedBy, Status, Found FROM Caches WHERE (status = \"A\"";
                // Disable geocaches
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("disable", false)) {
                    sql = sql + " OR status = \"T\"";
                }

                // Archived geocaches
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("archive", false)) {
                    sql = sql + " OR status = \"X\"";
                }

                sql = sql + ") ";

                if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("found", false)) {
                    sql = sql + " AND Found = 0";
                }

                if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("own", false)) {
                    sql = sql + " AND PlacedBy != \"" + PreferenceManager.getDefaultSharedPreferences(context).getString("nick", "") + "\"";
                }

                List<String> geocacheTypes = Gsak.geocacheTypesFromFilter(PreferenceManager.getDefaultSharedPreferences(context));
                boolean first = true;
                String sqlType = "";
                for (String geocacheType : geocacheTypes) {
                    if (first) {
                        sqlType += "CacheType = \"" + geocacheType + "\"";
                        first = false;
                    } else {
                        sqlType += " OR CacheType = \"" + geocacheType + "\"";
                    }
                }
                if (!sqlType.isEmpty()) {
                    sql += "AND (" + sqlType + ")";
                }

                sql += " AND CAST(Latitude AS REAL) > ? AND CAST(Latitude AS REAL) < ? AND CAST(Longitude AS REAL) > ? AND CAST(Longitude AS REAL) < ?";

                Cursor c = db.rawQuery(sql, cond);

                while (c.moveToNext()) {
                    Location loc = new Location(TAG);
                    loc.setLatitude(c.getDouble(c.getColumnIndex("Latitude")));
                    loc.setLongitude(c.getDouble(c.getColumnIndex("Longitude")));
                    Point p = new Point(c.getString(c.getColumnIndex("Name")), loc);

                    PointGeocachingData gcData = new PointGeocachingData();
                    gcData.cacheID = c.getString(c.getColumnIndex("Code"));
                    gcData.name = c.getString(c.getColumnIndex("Name"));
                    gcData.owner = c.getString(c.getColumnIndex("PlacedBy"));
                    gcData.type = Gsak.convertCacheType(c.getString(c.getColumnIndex("CacheType")));
                    gcData.available = Gsak.isAvailable(c.getString(c.getColumnIndex("Status")));
                    gcData.archived = Gsak.isArchived(c.getString(c.getColumnIndex("Status")));
                    gcData.computed = Gsak.isCorrected(c.getInt(c.getColumnIndex("HasCorrected")));
                    gcData.found = Gsak.isFound(c.getInt(c.getColumnIndex("Found")));

                    p.setGeocachingData(gcData);
                    p.setExtraOnDisplay("net.kuratkoo.locusaddon.gsakdatabase", "net.kuratkoo.locusaddon.gsakdatabase.DetailActivity", "cacheId", gcData.cacheID);
                    pd.addPoint(p);
                }
                c.close();

            } catch (Exception ex) {
                return ex;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception exception) {
            super.onPostExecute(exception);

            if (exception != null) {
                Log.w(TAG, exception);
            }

            try {
                db.close();

                File externalDir = Environment.getExternalStorageDirectory();
                String filePath = externalDir.getAbsolutePath();
                if (!filePath.endsWith("/")) {
                    filePath += "/";
                }
                filePath += "/Android/data/net.kuratkoo.locusaddon.gsakdatabase/livemap.locus";
                
                ArrayList<PointsData> data = new ArrayList<PointsData>();
                data.add(pd);
                DisplayData.sendDataFileSilent(context, data, filePath, true);
            } catch (RequiredVersionMissingException ex) {
            }
        }
    }
}
