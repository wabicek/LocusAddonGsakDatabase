package net.kuratkoo.locusaddon.gsakdatabase.util;

import java.io.File;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;

public class Gsak {

    private static final String TAG = "LocusAddonGsakDatabase|GsakUtils";

    public static Boolean isGsakDatabase(File f) {
        if (f.exists() && f.canRead() && f.isFile() && f.getName().endsWith("db3")) {
            return true;
        } else {
            return false;
        }
    }

    public static int convertContainer(String size) {
        if (size.equals("Small")) {
            return PointGeocachingData.CACHE_SIZE_SMALL;
        } else if (size.equals("Large")) {
            return PointGeocachingData.CACHE_SIZE_LARGE;
        } else if (size.equals("Micro")) {
            return PointGeocachingData.CACHE_SIZE_MICRO;
        } else if (size.equals("Not chosen")) {
            return PointGeocachingData.CACHE_SIZE_NOT_CHOSEN;
        } else if (size.equals("Other")) {
            return PointGeocachingData.CACHE_SIZE_OTHER;
        } else if (size.equals("Regular")) {
            return PointGeocachingData.CACHE_SIZE_REGULAR;
        } else {
            return PointGeocachingData.CACHE_SIZE_NOT_CHOSEN;
        }
    }

    public static int convertCacheType(String type) {
        if (type.equals("C")) {
            return PointGeocachingData.CACHE_TYPE_CACHE_IN_TRASH_OUT;
        } else if (type.equals("R")) {
            return PointGeocachingData.CACHE_TYPE_EARTH;
        } else if (type.equals("E")) {
            return PointGeocachingData.CACHE_TYPE_EVENT;
        } else if (type.equals("B")) {
            return PointGeocachingData.CACHE_TYPE_LETTERBOX;
        } else if (type.equals("Z")) {
            return PointGeocachingData.CACHE_TYPE_MEGA_EVENT;
        } else if (type.equals("M")) {
            return PointGeocachingData.CACHE_TYPE_MULTI;
        } else if (type.equals("T")) {
            return PointGeocachingData.CACHE_TYPE_TRADITIONAL;
        } else if (type.equals("U")) {
            return PointGeocachingData.CACHE_TYPE_MYSTERY;
        } else if (type.equals("V")) {
            return PointGeocachingData.CACHE_TYPE_VIRTUAL;
        } else if (type.equals("W")) {
            return PointGeocachingData.CACHE_TYPE_WEBCAM;
        } else if (type.equals("I")) {
            return PointGeocachingData.CACHE_TYPE_WHERIGO;
        } else if (type.equals("A")) {
            return PointGeocachingData.CACHE_TYPE_PROJECT_APE;
        } else if (type.equals("L")) {
            return PointGeocachingData.CACHE_TYPE_LOCATIONLESS;
        } else {
            return PointGeocachingData.CACHE_TYPE_TRADITIONAL;
        }
    }

    public static boolean isAvailable(String status) {
        if (status.equals("A")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isArchived(String status) {
        if (status.equals("X")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isFound(int found) {
        if (found == 1) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isPremium(int premium) {
        if (premium == 1) {
            return true;
        } else {
            return false;
        }
    }

    public static String convertWaypointType(String waypointType) {
        if (waypointType.equals("Final Location")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_FINAL;
        } else if (waypointType.equals("Parking Area")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_PARKING;
        } else if (waypointType.equals("Question to Answer")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_QUESTION;
        } else if (waypointType.equals("Reference Point")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_REFERENCE;
        } else if (waypointType.equals("Stages of a Multicache")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_STAGES;
        } else if (waypointType.equals("Trailhead")) {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_TRAILHEAD;
        } else {
            return PointGeocachingData.CACHE_WAYPOINT_TYPE_REFERENCE;
        }
    }

    public static int convertLogType(String logType) {
        if (logType.equals("Announcement")) {
            return PointGeocachingData.CACHE_LOG_TYPE_ANNOUNCEMENT;
        } else if (logType.equals("Attended")) {
            return PointGeocachingData.CACHE_LOG_TYPE_ATTENDED;
        } else if (logType.equals("Didn't find it")) {
            return PointGeocachingData.CACHE_LOG_TYPE_NOT_FOUNDED;
        } else if (logType.equals("Enable Listing")) {
            return PointGeocachingData.CACHE_LOG_TYPE_ENABLE_LISTING;
        } else if (logType.equals("Found it")) {
            return PointGeocachingData.CACHE_LOG_TYPE_FOUNDED;
        } else if (logType.equals("Needs Archived")) {
            return PointGeocachingData.CACHE_LOG_TYPE_NEEDS_ARCHIVED;
        } else if (logType.equals("Needs Maintenance")) {
            return PointGeocachingData.CACHE_LOG_TYPE_NEEDS_MAINTENANCE;
        } else if (logType.equals("Owner Maintenance")) {
            return PointGeocachingData.CACHE_LOG_TYPE_OWNER_MAINTENANCE;
        } else if (logType.equals("Post Reviewer Note")) {
            return PointGeocachingData.CACHE_LOG_TYPE_POST_REVIEWER_NOTE;
        } else if (logType.equals("Publish Listing")) {
            return PointGeocachingData.CACHE_LOG_TYPE_PUBLISH_LISTING;
        } else if (logType.equals("Temporarily Disable Listing")) {
            return PointGeocachingData.CACHE_LOG_TYPE_TEMPORARILY_DISABLE_LISTING;
        } else if (logType.equals("Update Coordinates")) {
            return PointGeocachingData.CACHE_LOG_TYPE_UPDATE_COORDINATES;
        } else if (logType.equals("Webcam Photo Taken")) {
            return PointGeocachingData.CACHE_LOG_TYPE_WEBCAM_PHOTO_TAKEN;
        } else if (logType.equals("Will Attend")) {
            return PointGeocachingData.CACHE_LOG_TYPE_WILL_ATTEND;
        } else if (logType.equals("Write note")) {
            return PointGeocachingData.CACHE_LOG_TYPE_WRITE_NOTE;
        } else {
            return PointGeocachingData.CACHE_LOG_TYPE_UNKNOWN;
        }
    }

    public static boolean isCorrected(int correction) {
        if (correction == 1) {
            return true;
        } else {
            return false;
        }    }
}
