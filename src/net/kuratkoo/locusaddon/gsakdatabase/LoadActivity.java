package net.kuratkoo.locusaddon.gsakdatabase;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import menion.android.locus.addon.publiclib.DisplayData;
import menion.android.locus.addon.publiclib.LocusIntents;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;
import menion.android.locus.addon.publiclib.geoData.PointsData;
import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingDataWaypoint;

public class LoadActivity extends Activity {

    private static final String TAG = "LocusAddonGsakDatabase|LoadActivity";
    private ProgressDialog progress;
    private ArrayList<PointsData> data;
    private File externalDir;
    private Point point;

    private class LoadAsyncTask extends AsyncTask<Point, Integer, Exception> {

        @Override
        protected void onPreExecute() {
            progress.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progress.setMessage(getString(R.string.loading) + " " + values[0] + " " + getString(R.string.geocaches));
        }

        @Override
        protected void onPostExecute(Exception ex) {
            progress.dismiss();

            if (ex != null) {
                Toast.makeText(LoadActivity.this, getString(R.string.unable_to_load_geocaches) + " (" + ex.getLocalizedMessage() + ")", Toast.LENGTH_LONG).show();
                ex.printStackTrace();
                LoadActivity.this.finish();
                return;
            }

            String filePath = externalDir.getAbsolutePath();
            if (!filePath.endsWith("/")) {
                filePath += "/";
            }
            filePath += "/Android/data/net.kuratkoo.locusaddon.gsakdatabase/data.locus";

            try {
                DisplayData.sendDataFile(LoadActivity.this,
                        data,
                        filePath,
                        PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("import", true));
            } catch (OutOfMemoryError e) {
                AlertDialog.Builder ad = new AlertDialog.Builder(LoadActivity.this);
                ad.setIcon(android.R.drawable.ic_dialog_alert);
                ad.setTitle(R.string.error);
                ad.setMessage(R.string.out_of_memory);
                ad.setPositiveButton(android.R.string.ok, new OnClickListener() {

                    public void onClick(DialogInterface di, int arg1) {
                        di.dismiss();
                    }
                });
                ad.show();
            }
        }

        protected Exception doInBackground(Point... pointSet) {
            try {
                Point pp = pointSet[0];
                Location curr = pp.getLocation();
                PointsData pd = new PointsData("GSAK data");
                Float radius = Float.valueOf(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("radius", "1")) / 70;

                SQLiteDatabase database = SQLiteDatabase.openDatabase(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("db", ""), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                String[] cond = new String[]{
                    String.valueOf(curr.getLatitude() - radius),
                    String.valueOf(curr.getLatitude() + radius),
                    String.valueOf(curr.getLongitude() - radius),
                    String.valueOf(curr.getLongitude() + radius)
                };
                Cursor c;
                String sql = "SELECT Latitude, Longitude, Code, PlacedBy FROM Caches WHERE (status = \"A\"";

                // Disable geocaches
                if (PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("disable", false)) {
                    sql = sql + " OR status = \"T\"";
                }

                // Archived geocaches
                if (PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("archive", false)) {
                    sql = sql + " OR status = \"X\"";
                }

                sql = sql + ") ";

                if (!PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("found", false)) {
                    sql = sql + " AND Found = 0";
                }

                if (!PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("own", false)) {
                    sql = sql + " AND PlacedBy != \"" + PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("nick", "") + "\"";
                }
                sql += " AND CAST(Latitude AS REAL) > ? AND CAST(Latitude AS REAL) < ? AND CAST(Longitude AS REAL) > ? AND CAST(Longitude AS REAL) < ?";

                c = database.rawQuery(sql, cond);
                Log.d(TAG, "Total: " + c.getCount());

                /** Load GC codes **/
                List<Pair> gcCodes = new ArrayList<Pair>();
                while (c.moveToNext()) {
                    Location loc = new Location(TAG);
                    loc.setLatitude(c.getDouble(c.getColumnIndex("Latitude")));
                    loc.setLongitude(c.getDouble(c.getColumnIndex("Longitude")));
                    if (loc.distanceTo(curr) < Float.valueOf(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("radius", "1")) * 1000) {
                        gcCodes.add(new Pair(loc.distanceTo(curr), c.getString(c.getColumnIndex("Code"))));
                    }
                }
                c.close();

                int count = 0;
                int limit = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("limit", "0"));

                if (limit > 0) {
                    Collections.sort(gcCodes, new Comparator<Pair>() {

                        public int compare(Pair p1, Pair p2) {
                            return p1.distance.compareTo(p2.distance);
                        }
                    });
                }

                for (Pair pair : gcCodes) {
                    if (limit > 0) {
                        if (count >= limit) {
                            break;
                        }
                    }
                    String gcCode = pair.gcCode;
                    publishProgress(++count);
                    c = database.rawQuery("SELECT * FROM Caches WHERE Code = ?", new String[]{gcCode});
                    c.moveToNext();
                    Location loc = new Location(TAG);
                    loc.setLatitude(c.getDouble(c.getColumnIndex("LatOriginal")));
                    loc.setLongitude(c.getDouble(c.getColumnIndex("LonOriginal")));
                    Point p = new Point(c.getString(c.getColumnIndex("Name")), loc);

                    PointGeocachingData gcData = new PointGeocachingData();
                    gcData.cacheID = c.getString(c.getColumnIndex("Code"));
                    gcData.name = c.getString(c.getColumnIndex("Name"));
                    gcData.owner = c.getString(c.getColumnIndex("PlacedBy"));
                    gcData.placedBy = c.getString(c.getColumnIndex("PlacedBy"));
                    gcData.difficulty = c.getFloat(c.getColumnIndex("Difficulty"));
                    gcData.terrain = c.getFloat(c.getColumnIndex("Terrain"));
                    gcData.country = c.getString(c.getColumnIndex("Country"));
                    gcData.state = c.getString(c.getColumnIndex("State"));
                    gcData.container = GsakUtils.convertContainer(c.getString(c.getColumnIndex("Container")));
                    gcData.type = GsakUtils.convertCacheType(c.getString(c.getColumnIndex("CacheType")));
                    gcData.available = GsakUtils.isAvailable(c.getString(c.getColumnIndex("Status")));
                    gcData.archived = GsakUtils.isArchived(c.getString(c.getColumnIndex("Status")));
                    gcData.found = GsakUtils.isFound(c.getInt(c.getColumnIndex("Found")));
                    gcData.premiumOnly = GsakUtils.isPremium(c.getInt(c.getColumnIndex("Found")));
                    gcData.computed = false;

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    Date date = new Date();
                    gcData.exported = dateFormat.format(date);

                    String lastUpdated = c.getString(c.getColumnIndex("LastUserDate"));
                    if (lastUpdated.length() == 10) {
                        gcData.lastUpdated = lastUpdated.substring(0, 4) + "-" + lastUpdated.substring(4, 6) + "-" + lastUpdated.substring(6, 8) + "T";
                    }

                    String hidden = c.getString(c.getColumnIndex("PlacedDate"));
                    gcData.hidden = hidden.substring(0, 4) + "-" + hidden.substring(4, 6) + "-" + hidden.substring(6, 8) + "T";


                    c.close();

                    /** Add waypoints to Geocache **/
                    ArrayList<PointGeocachingDataWaypoint> pgdws = new ArrayList<PointGeocachingDataWaypoint>();

                    Cursor wp = database.rawQuery("SELECT * FROM WayAll WHERE cParent = ?", new String[]{gcData.cacheID});
                    while (wp.moveToNext()) {
                        PointGeocachingDataWaypoint pgdw = new PointGeocachingDataWaypoint();
                        pgdw.lat = wp.getDouble(wp.getColumnIndex("cLat"));
                        pgdw.lon = wp.getDouble(wp.getColumnIndex("cLon"));
                        pgdw.name = wp.getString(wp.getColumnIndex("cName"));
                        pgdw.type = GsakUtils.convertWaypointType(wp.getString(wp.getColumnIndex("cType")));
                        pgdw.description = wp.getString(wp.getColumnIndex("cComment"));
                        pgdw.code = wp.getString(wp.getColumnIndex("cCode"));
                        pgdws.add(pgdw);
                    }
                    wp.close();

                    Cursor cc = database.rawQuery("SELECT * FROM Corrected WHERE kCode = ?", new String[]{gcData.cacheID});
                    while (cc.moveToNext()) {
                        PointGeocachingDataWaypoint pgdw = new PointGeocachingDataWaypoint();
                        pgdw.lat = cc.getDouble(cc.getColumnIndex("kAfterLat"));
                        pgdw.lon = cc.getDouble(cc.getColumnIndex("kAfterLon"));
                        pgdw.name = "Corrected coordinates";
                        pgdw.type = PointGeocachingData.CACHE_WAYPOINT_TYPE_FINAL;
                        pgdw.code = "FI";
                        pgdws.add(pgdw);
                    }
                    cc.close();
                    gcData.waypoints = pgdws;

                    p.setGeocachingData(gcData);
                    p.setExtraOnDisplay("net.kuratkoo.locusaddon.gsakdatabase", "net.kuratkoo.locusaddon.gsakdatabase.DetailActivity", "cacheId", gcData.cacheID);
                    pd.addPoint(p);

                }

                database.close();

                data = new ArrayList<PointsData>();
                data.add(pd);

                return null;
            } catch (Exception e) {
                return e;
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.loading_dots));
        progress.setIcon(android.R.drawable.ic_dialog_info);
        progress.setTitle(getString(R.string.loading));

        externalDir = Environment.getExternalStorageDirectory();
        if (externalDir == null || !(externalDir.exists())) {
            Toast.makeText(LoadActivity.this, R.string.no_external_storage, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        File fd = new File(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("db", ""));
        if (!GsakUtils.isGsakDatabase(fd)) {
            Toast.makeText(LoadActivity.this, R.string.no_db_file, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Intent fromIntent = getIntent();
        if (LocusIntents.isIntentOnPointAction(fromIntent)) {
            point = LocusIntents.handleIntentOnPointAction(fromIntent);
        } else if (LocusIntents.isIntentMainFunction(fromIntent)) {
            LocusIntents.handleIntentMainFunction(fromIntent, new LocusIntents.OnIntentMainFunction() {

                public void onLocationReceived(boolean gpsEnabled, Location locGps, Location locMapCenter) {
                    point = new Point("Map center", locMapCenter);
                }

                public void onFailed() {
                }
            });
        }
        new LoadAsyncTask().execute(point);
    }

    private class Pair {

        private String gcCode;
        private Float distance;

        public Pair(Float f, String s) {
            this.distance = f;
            this.gcCode = s;
        }
    }
}
