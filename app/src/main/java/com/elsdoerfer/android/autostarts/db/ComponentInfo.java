package com.elsdoerfer.android.autostarts.db;

import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;


/**
 * This can be compared to android.content.pm.ComponentInfo,
 * except that we are forced to parse the package manifest files
 * ourselves, so we don't use the classes in android.content.pm.
 */
public class ComponentInfo implements Parcelable {
    // These identify the component
    public PackageInfo packageInfo;
    public String componentName;
    //  The receivers this component uses.
    public IntentFilterInfo[] intentFilters;

    // This is peripheral data
    public String componentLabel;
    public int priority;
    public boolean defaultEnabled;
    public int currentEnabledState;

    public ComponentInfo() {
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s/%s", packageInfo.packageName, componentName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.packageInfo, this.componentName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComponentInfo)) return false;
        ComponentInfo that = (ComponentInfo) o;
        return packageInfo.equals(that.packageInfo) && componentName.equals(that.componentName);
    }

    /**
     * Return a label identifying the component.
     */
    public String getLabel() {
        if (componentLabel != null && !componentLabel.equals(""))
            return componentLabel;
        else
            return packageInfo.getLabel();
    }

    /**
     * Resolve the current and default "enabled" state.
     */
    public boolean isCurrentlyEnabled() {
        switch (currentEnabledState) {
            case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                return true;
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED:
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER:
                return false;
            case PackageManager.COMPONENT_ENABLED_STATE_DEFAULT:
                return defaultEnabled;
            default:
                throw new RuntimeException("Not a valid enabled state: " + currentEnabledState);
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        packageInfo.writeToParcel(dest, flags);
        dest.writeString(componentName);
        dest.writeString(componentLabel);
        dest.writeInt(priority);
        dest.writeInt(defaultEnabled ? 1 : 0);
        dest.writeInt(currentEnabledState);
    }

    public static final Parcelable.Creator<ComponentInfo> CREATOR
            = new Parcelable.Creator<ComponentInfo>() {
        public ComponentInfo createFromParcel(Parcel in) {
            return new ComponentInfo(in);
        }

        public ComponentInfo[] newArray(int size) {
            return new ComponentInfo[size];
        }
    };

    private ComponentInfo(Parcel in) {
        packageInfo = PackageInfo.CREATOR.createFromParcel(in);
        componentName = in.readString();
        componentLabel = in.readString();
        priority = in.readInt();
        defaultEnabled = in.readInt() == 1;
        currentEnabledState = in.readInt();
    }
}

