package com.elsdoerfer.android.autostarts;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ListActivity extends android.app.ListActivity {

	static final String TAG = "Autostarts";

	// The broadcast intents we support. Read about why this needs to be a
	// predefined, static list below in the comments on where we do the
	// actual loading of the installed receivers.
	//
	// This currently is a list of all intents in the android.content.Intent
	// namespace that are marked as "broadcast intents" in their documentation.
	static final Object[][] supportedIntents = {
		{ Intent.ACTION_AIRPLANE_MODE_CHANGED, R.string.act_airplane_mode_changed },
		{ Intent.ACTION_BATTERY_CHANGED, R.string.act_battery_changed },
		{ Intent.ACTION_BATTERY_LOW, R.string.act_battery_low },
		{ Intent.ACTION_BOOT_COMPLETED, R.string.act_boot_completed },
		{ Intent.ACTION_CAMERA_BUTTON, R.string.act_camera_button },
		{ Intent.ACTION_CLOSE_SYSTEM_DIALOGS, R.string.act_close_system_dialogs },
		{ Intent.ACTION_CONFIGURATION_CHANGED, R.string.act_configuration_changed },
		{ Intent.ACTION_DATE_CHANGED,  R.string.act_date_changed },
		{ Intent.ACTION_DEVICE_STORAGE_LOW, R.string.act_device_storage_low },
		{ Intent.ACTION_DEVICE_STORAGE_OK,  R.string.act_device_storage_ok },
		{ Intent.ACTION_GTALK_SERVICE_CONNECTED,  R.string.act_gtalk_service_connected },
		{ Intent.ACTION_GTALK_SERVICE_DISCONNECTED, R.string.act_gtalk_service_disconnected },
		{ Intent.ACTION_HEADSET_PLUG,  R.string.act_headset_plug },
		{ Intent.ACTION_INPUT_METHOD_CHANGED,  R.string.act_input_method_changed },
		{ Intent.ACTION_MANAGE_PACKAGE_STORAGE, R.string.act_manage_package_storage },
		{ Intent.ACTION_MEDIA_BAD_REMOVAL, R.string.act_media_bad_removal },
		{ Intent.ACTION_MEDIA_BUTTON, R.string.act_media_button },
		{ Intent.ACTION_MEDIA_CHECKING, R.string.act_media_checking },
		{ Intent.ACTION_MEDIA_EJECT, R.string.act_media_eject },
		{ Intent.ACTION_MEDIA_MOUNTED, R.string.act_media_mounted },
		{ Intent.ACTION_MEDIA_NOFS, R.string.act_media_nofs },
		{ Intent.ACTION_MEDIA_REMOVED, R.string.act_media_removed },
		{ Intent.ACTION_MEDIA_SCANNER_FINISHED, R.string.act_media_scanner_finished },
		{ Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, R.string.act_media_scanner_scan_file },
		{ Intent.ACTION_MEDIA_SCANNER_STARTED, R.string.act_media_scanner_started },
		{ Intent.ACTION_MEDIA_SHARED, R.string.act_media_shared },
		{ Intent.ACTION_MEDIA_UNMOUNTABLE, R.string.act_media_unmountable },
		{ Intent.ACTION_MEDIA_UNMOUNTED, R.string.act_media_unmounted },
		{ Intent.ACTION_NEW_OUTGOING_CALL, R.string.act_new_outgoing_call },
		{ Intent.ACTION_PACKAGE_ADDED, R.string.act_package_added },
		{ Intent.ACTION_PACKAGE_CHANGED, R.string.act_package_changed },
		{ Intent.ACTION_PACKAGE_DATA_CLEARED, R.string.act_package_data_cleared },
		{ Intent.ACTION_PACKAGE_INSTALL, R.string.act_package_install },
		{ Intent.ACTION_PACKAGE_REMOVED, R.string.act_package_removed },
		{ Intent.ACTION_PACKAGE_REPLACED, R.string.act_package_replaced },
		{ Intent.ACTION_PACKAGE_RESTARTED, R.string.act_package_restarted },
		{ Intent.ACTION_PROVIDER_CHANGED, R.string.act_provider_changed },
		{ Intent.ACTION_REBOOT, R.string.act_reboot },
		{ Intent.ACTION_SCREEN_OFF, R.string.act_screen_off },
		{ Intent.ACTION_SCREEN_ON, R.string.act_screen_on },
		{ Intent.ACTION_TIMEZONE_CHANGED, R.string.act_timezone_changed },
		{ Intent.ACTION_TIME_CHANGED, R.string.act_time_changed },
		{ Intent.ACTION_TIME_TICK, R.string.act_time_tick },           // not through manifest components?
		{ Intent.ACTION_UID_REMOVED, R.string.act_uid_removed },
		{ Intent.ACTION_UMS_CONNECTED, R.string.act_ums_connected },
		{ Intent.ACTION_UMS_DISCONNECTED, R.string.act_ums_disconnected },
		{ Intent.ACTION_USER_PRESENT, R.string.act_user_present },
		{ Intent.ACTION_WALLPAPER_CHANGED, R.string.act_wallpaper_changed }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);


        // TODO: move this to an ASyncTask
        load();
    }

    /**
     * Load the broadcast receivers installed by applications. Unfortunately, this
     * is a lot more difficult than it sounds.
     *
     * There are multiple approaches one might consider taking, all flawed:
     *
     * 1) Loop through the list of installed packages and collect all receivers.
     *    Unfortunately, Android currently does not allow us to query the intents
     *    a receiver has registered for. See the unimplemented
     *    PackageManager.GET_INTENT_FILTERS, and the following URLs:
     *       http://groups.google.com/group/android-developers/browse_thread/thread/4502827143ea9b20
     *       http://groups.google.com/group/android-developers/browse_thread/thread/ef0e4b390552f2c/
     *
     * 2) Use PackageManager.queryBroadcastReceivers() to find all installed
     *    receivers. In the following thread, hackbod initially suggests that
     *    this could be done using a single call: By using an empty intent filter
     *    with no action, all receivers would be returned. Note however that this
     *    is retracted in a later post:
     *       http://groups.google.com/group/android-developers/browse_thread/thread/3ba4f419f0bec3aa/
     *    Even if that worked, it's still be an open question if we could retrieve
     *    the associated intents, or if we'd again run into the problem from 1).
     *
     *    As a result, using this method we would need a list of builtin, supported
     *    broadcast actions, for each of which we query installed receivers
     *    individually.
     *
     *    Unfortunately, this method has another huge downside: Disabled components
     *    are never returned. As a result, if we want to give the user the option
     *    to toggle receivers on and off, once one has been disabled, we need to
     *    employ various hackery to remember this new status, because a requery
     *    will no longer list the receiver. Considering that applications may be
     *    removed/updated, this is a slightly complex proposition.
     *
     * 3) We could parse the apps inside /data/app manually, potentially based on
     *    the OS Android Code (see for example PackageParser.java). However, this
     *    would be complex to write in any case.
     *
     * For now, we are going with 2).
     */
    public void load() {
        final PackageManager pm = getPackageManager();

        ArrayList<Object[]> receiverList = new ArrayList<Object[]>();

        for (Object[] intent : supportedIntents) {
            Intent query = new Intent();
            query.setAction((String)(intent[0]));

	        List<ResolveInfo> receivers = pm.queryBroadcastReceivers(query, PackageManager.GET_INTENT_FILTERS);
	        for (int i=receivers.size()-1; i>=0; i--) {
	        	ResolveInfo r = receivers.get(i);
	        	Log.d(TAG, "Found receiver: "+r.toString());

	        	if (r.activityInfo == null) {
	        		Log.d(TAG, "activityInfo is null?!");
	        		continue;
	        	}

	        	Object[] dataset = new Object[] { intent, r };
				receiverList.add(dataset);
	        }
        }

		// TODO: give experts more information through a dialog or something. For
		// example, show the full broadcast receiver namespace name, from
		//		r.activityInfo.name

        ArrayAdapter<Object[]> a = new ArrayAdapter<Object[]>(
        		this, R.layout.row, R.id.title, receiverList) {
					@Override
					public View getView(int position, View convertView,
							ViewGroup parent) {
						View view = super.getView(position, convertView, parent);
						Object[] data = this.getItem(position);
						((ImageView)view.findViewById(R.id.icon)).setImageDrawable(
							((ResolveInfo)data[1]).loadIcon(pm));
						((TextView)view.findViewById(R.id.title)).setText(
							((ResolveInfo)data[1]).loadLabel(pm));
						((TextView)view.findViewById(R.id.subtitle)).setText(
								(Integer)((Object[])data[0])[1]);
						return view;
					}
        };
        setListAdapter(a);

    }
}