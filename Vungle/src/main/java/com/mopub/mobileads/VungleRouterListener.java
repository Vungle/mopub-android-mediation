package com.mopub.mobileads;

import androidx.annotation.NonNull;

public interface VungleRouterListener {

    void onAdEnd(String id);

    void onAdClick(String id);

    void onAdRewarded(String id);

    void onAdLeftApplication(String id);

    void onAdStart(@NonNull String placementId);

    void onUnableToPlayAd(@NonNull String placementId, String reason);

    void onAdAvailabilityUpdate(@NonNull String placementId, boolean isAdAvailable);

}
