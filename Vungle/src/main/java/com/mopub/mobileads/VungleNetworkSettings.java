package com.mopub.mobileads;

import com.vungle.warren.VungleSettings;

/**
 * To apply the Vungle network settings during initialization.
 */
class VungleNetworkSettings {

    private static VungleSettings.Builder sBuilder = new VungleSettings.Builder();

    /**
     * To pass Vungle network setting to SDK, these methods must be called before first loadAd.
     * if called after first loading an ad, settings will not be applied.
     */
    private static VungleSettings sVungleSettings;

    static void setMinSpaceForInit(long spaceForInit) {
        sBuilder.setMinimumSpaceForInit(spaceForInit);
        applySettings();
    }

    static void setMinSpaceForAdLoad(long spaceForAd) {
        sBuilder.setMinimumSpaceForAd(spaceForAd);
        applySettings();
    }

    static void setAndroidIdOptOut(boolean isOptedOut) {
        sBuilder.setAndroidIdOptOut(isOptedOut);
        applySettings();
    }

    static void setPriorityPlacement(String priorityPlacement) {
        sBuilder.setPriorityPlacement(priorityPlacement);
        applySettings();
    }

    static VungleSettings getVungleSettings() {
        return sVungleSettings;
    }


    private static void applySettings() {
        sVungleSettings = sBuilder.disableBannerRefresh().build();
    }
}
