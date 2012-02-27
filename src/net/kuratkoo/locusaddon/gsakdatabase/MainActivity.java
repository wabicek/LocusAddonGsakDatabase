package net.kuratkoo.locusaddon.gsakdatabase;

import net.kuratkoo.locusaddon.gsakdatabase.util.Gsak;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.Html;
import android.text.Spanned;
import android.widget.Toast;
import java.io.File;
import menion.android.locus.addon.publiclib.LocusUtils;

public class MainActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private static final String TAG = "LocusAddonGsakDatabase|MainActivity";
    private EditTextPreference db;
    private EditTextPreference nick;
    private EditTextPreference logsCount;
    private EditTextPreference radius;
    private EditTextPreference limit;
    private CheckBoxPreference own;
    private Preference donate;

    @Override
    protected void onResume() {
        super.onResume();
        if (!checkLocus()) return;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkLocus()) return;

        addPreferencesFromResource(R.xml.prefs);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        donate = (Preference) getPreferenceScreen().findPreference("donate");
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=kuratkoo%40gmail%2ecom&lc=CZ&item_name=Locus%20-%20addon%20GSAK%20Database&currency_code=USD"));
        donate.setIntent(i);

        own = (CheckBoxPreference) getPreferenceScreen().findPreference("own");

        db = (EditTextPreference) getPreferenceScreen().findPreference("db");
        File fd = new File(db.getText());
        db.setSummary(editFilePreferenceSummary(Gsak.isGsakDatabase(fd), db.getText(), getText(R.string.pref_db_sum)));

        nick = (EditTextPreference) getPreferenceScreen().findPreference("nick");
        nick.setSummary(editPreferenceSummary(nick.getText(), getText(R.string.pref_nick_sum)));
        if (nick.getText().trim().length() == 0) {
            own.setEnabled(false);
        } else {
            own.setEnabled(true);
        }

        radius = (EditTextPreference) getPreferenceScreen().findPreference("radius");
        radius.setSummary(editPreferenceSummary(radius.getText() + " km", getText(R.string.pref_radius_sum)));

        logsCount = (EditTextPreference) getPreferenceScreen().findPreference("logs_count");
        logsCount.setSummary(editPreferenceSummary(logsCount.getText(), getText(R.string.pref_logs_sum)));

        limit = (EditTextPreference) getPreferenceScreen().findPreference("limit");
        if (limit.getText().equals("0")) {
            limit.setSummary(editPreferenceSummary(getString(R.string.pref_limit_nolimit), getText(R.string.pref_limit_sum)));
        } else {
            limit.setSummary(editPreferenceSummary(limit.getText(), getText(R.string.pref_limit_sum)));
        }

        if (!own.isEnabled()) {
            own.setSummary(Html.fromHtml(getString(R.string.pref_own_sum) + " <b>" + getString(R.string.pref_own_fill) + "</b>"));
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("db")) {
            String path = sharedPreferences.getString(key, "");
            File fd = new File(path);
            db.setSummary(editFilePreferenceSummary(Gsak.isGsakDatabase(fd), path, getText(R.string.pref_db_sum)));
        }

        if (key.equals("nick")) {
            nick.setSummary(editPreferenceSummary(sharedPreferences.getString(key, ""), getText(R.string.pref_nick_sum)));
            if (nick.getText().trim().length() == 0) {
                own.setEnabled(false);
                own.setSummary(Html.fromHtml(getString(R.string.pref_own_sum) + " <b>" + getString(R.string.pref_own_fill) + "</b>"));
            } else {
                own.setEnabled(true);
                own.setSummary(getString(R.string.pref_own_sum));
            }
        }

        if (key.equals("logs_count")) {
            String value = sharedPreferences.getString(key, "5");
            if (value.equals("") || !value.matches("[0-9]+")) {
                Toast.makeText(this, getString(R.string.pref_logs_error), Toast.LENGTH_LONG).show();
                value = "5";
                logsCount.setText("5");
            }
            logsCount.setSummary(editPreferenceSummary(value, getText(R.string.pref_logs_sum)));
        }

        if (key.equals("radius")) {
            String value = sharedPreferences.getString(key, "1");
            if (value.equals("") || !value.matches("[0-9]+") || value.equals("0") || value.equals("00")) {
                Toast.makeText(this, getString(R.string.pref_logs_error), Toast.LENGTH_LONG).show();
                value = "1";
                radius.setText("1");
            }
            radius.setSummary(editPreferenceSummary(value + " km", getText(R.string.pref_radius_sum)));
        }

        if (key.equals("limit")) {
            String value = sharedPreferences.getString(key, "0");
            if (value.equals("") || !value.matches("[0-9]+")) {
                Toast.makeText(this, getString(R.string.pref_limit_error), Toast.LENGTH_LONG).show();
                limit.setText("0");
                limit.setSummary(editPreferenceSummary(getString(R.string.pref_limit_nolimit), getText(R.string.pref_logs_sum)));
            } else if (value.equals("0")) {
                limit.setSummary(editPreferenceSummary(getString(R.string.pref_limit_nolimit), getText(R.string.pref_limit_sum)));
            } else {
                limit.setSummary(editPreferenceSummary(value, getText(R.string.pref_limit_sum)));
            }
        }
    }

    private Spanned editPreferenceSummary(String value, CharSequence summary) {
        if (!value.equals("")) {
            return Html.fromHtml("<font color=\"#FF8000\"><b>(" + value + ")</b></font> " + summary);
        } else {
            return Html.fromHtml(summary.toString());
        }
    }

    private Spanned editFilePreferenceSummary(boolean b, String value, CharSequence summary) {
        if (!value.equals("")) {
            if (b) {
                return Html.fromHtml("<font color=\"#00FF00\"><b>(OK)</b></font> <font color=\"#FF8000\"><b>(" + value + ")</b></font> " + summary);
            } else {
                return Html.fromHtml("<font color=\"#FF0000\"><b>(KO)</b></font> <font color=\"#FF8000\"><b>(" + value + ")</b></font> " + summary);
            }
        } else {
            return Html.fromHtml(summary.toString());
        }
    }

    private boolean checkLocus() {
        if (!LocusUtils.isLocusAvailable(this)) {
            AlertDialog.Builder ad = new AlertDialog.Builder(this);
            ad.setIcon(android.R.drawable.ic_dialog_alert);
            ad.setTitle(R.string.error);
            ad.setMessage(R.string.install_it);
            ad.setPositiveButton(android.R.string.ok, new OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + LocusUtils.getLocusDefaultPackageName(MainActivity.this))));
                }
            });
            ad.show();
            return false;
        }

        if ((LocusUtils.getLocusVersionCode(this) < 121 && !LocusUtils.isLocusProInstalled(this)) || (LocusUtils.getLocusVersionCode(this) < 59 && LocusUtils.isLocusProInstalled(this))) {
            AlertDialog.Builder ad = new AlertDialog.Builder(this);
            ad.setIcon(android.R.drawable.ic_dialog_alert);
            ad.setTitle(R.string.error);
            ad.setMessage(getString(R.string.update_it));
            ad.setPositiveButton(android.R.string.ok, new OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + LocusUtils.getLocusDefaultPackageName(MainActivity.this))));
                }
            });
            ad.show();
            return false;
        }

        return true;
    }
}
