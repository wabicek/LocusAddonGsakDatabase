package net.kuratkoo.locusaddon.gsakdatabase;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import menion.android.locus.addon.publiclib.LocusConst;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;
import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingAttributes;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingDataLog;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingDataWaypoint;

public class DetailActivity extends Activity {

    private static final String TAG = "LocusAddonGsakDatabase|DetailActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        File fd = new File(PreferenceManager.getDefaultSharedPreferences(this).getString("db", ""));
        if (!GsakUtils.isGsakDatabase(fd)) {
            Toast.makeText(this, R.string.no_db_file, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (intent.hasExtra("cacheId")) {
            String value = intent.getStringExtra("cacheId");
            try {

                byte[] buff = new byte[100000];

                SQLiteDatabase database = SQLiteDatabase.openDatabase(PreferenceManager.getDefaultSharedPreferences(this).getString("db", ""), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                Cursor c = database.rawQuery("SELECT * FROM CachesAll WHERE Code = ?", new String[]{value});
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
                gcData.hidden = hidden.substring(0, 4) + "-" + hidden.substring(5, 7) + "-" + hidden.substring(7, 9) + "T";

                // More!
                gcData.notes = c.getString(c.getColumnIndex("UserNote"));
                gcData.encodedHints = c.getString(c.getColumnIndex("Hints"));
                gcData.shortDescription = c.getString(c.getColumnIndex("ShortDescription"));
                gcData.longDescription = c.getString(c.getColumnIndex("LongDescription"));

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

                Cursor cc = database.rawQuery("SELECT * FROM corrected WHERE kCode = ?", new String[]{gcData.cacheID});
                while (cc.moveToNext()) {
                    PointGeocachingDataWaypoint pgdw = new PointGeocachingDataWaypoint();
                    pgdw.lat = cc.getDouble(cc.getColumnIndex("kAfterLat"));
                    pgdw.lon = cc.getDouble(cc.getColumnIndex("kAfterLon"));
                    pgdw.name = PointGeocachingData.CACHE_WAYPOINT_TYPE_FINAL;;
                    pgdw.type = PointGeocachingData.CACHE_WAYPOINT_TYPE_FINAL;
                    pgdw.code = "FI";
                    pgdws.add(pgdw);
                }
                cc.close();

                gcData.waypoints = pgdws;

                /** Add logs to Geocache **/
                String limit = PreferenceManager.getDefaultSharedPreferences(this).getString("logs_count", "5");
                Cursor logs = database.rawQuery("SELECT * FROM LogsAll WHERE lParent = ? ORDER BY lDate DESC LIMIT ?", new String[]{gcData.cacheID, limit});
                ArrayList<PointGeocachingDataLog> pgdls = new ArrayList<PointGeocachingDataLog>();

                while (logs.moveToNext()) {
                    PointGeocachingDataLog pgdl = new PointGeocachingDataLog();
                    String found = logs.getString(logs.getColumnIndex("lDate"));
                    pgdl.date = found.substring(0, 4) + "-" + found.substring(5, 7) + "-" + found.substring(7, 9) + "T00:00:00Z";
                    pgdl.finder = logs.getString(logs.getColumnIndex("lBy"));
                    pgdl.logText = logs.getString(logs.getColumnIndex("lText"));
                    pgdl.type = GsakUtils.convertLogType(logs.getString(logs.getColumnIndex("lType")));
                    pgdls.add(pgdl);
                }
                logs.close();
                gcData.logs = pgdls;

                /** Add attributes to Geocache **/
                Cursor at = database.rawQuery("SELECT * FROM Attributes WHERE aCode = ?", new String[]{gcData.cacheID});
                ArrayList<PointGeocachingAttributes> pgas = new ArrayList<PointGeocachingAttributes>();

                while (at.moveToNext()) {
                    Boolean isPositive = false;
                    if (at.getInt(at.getColumnIndex("aInc")) == 1) {
                        isPositive = true;
                    } else {
                        isPositive = false;
                    }
                    PointGeocachingAttributes pga = new PointGeocachingAttributes(at.getInt(at.getColumnIndex("aId")), isPositive);
                    pgas.add(pga);
                }
                at.close();
                gcData.attributes = pgas;

                p.setGeocachingData(gcData);
                database.close();

                Intent retIntent = new Intent();
                retIntent.putExtra(LocusConst.EXTRA_POINT, p);
                setResult(RESULT_OK, retIntent);

            } catch (Exception e) {
                Toast.makeText(this, getText(R.string.unable_to_load_detail) + " " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();

            } finally {
                finish();
            }
        }
    }
}
