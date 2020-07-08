package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.mopub.common.logging.MoPubLog;
import com.vungle.warren.AdConfig;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;

/**
 * A custom event for showing Vungle Interstitial.
 */
@Keep
public class VungleInterstitial extends CustomEventInterstitial {

    /*
     * These keys can be used with MoPubInterstitial.setLocalExtras()
     * to pass additional parameters to the SDK.
     */
    @Deprecated
    public static final String SOUND_ENABLED_KEY = "vungleSoundEnabled";
    @Deprecated
    public static final String FLEX_VIEW_CLOSE_TIME_KEY = "vungleFlexViewCloseTimeInSec";
    @Deprecated
    public static final String ORDINAL_VIEW_COUNT_KEY = "vungleOrdinalViewCount";
    @Deprecated
    public static final String AD_ORIENTATION_KEY = "vungleAdOrientation";

    /*
     * APP_ID_KEY is intended for MoPub internal use. Do not modify.
     */
    private static final String APP_ID_KEY = "appId";
    private static final String PLACEMENT_ID_KEY = "pid";
    private static final String PLACEMENT_IDS_KEY = "pids";
    private static final String ADAPTER_NAME = VungleInterstitial.class.getSimpleName();

    private static VungleRouter sVungleRouter;
    private final Handler mHandler;
    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private VungleInterstitialRouterListener mVungleRouterListener;
    @NonNull
    private VungleAdapterConfiguration mVungleAdapterConfiguration;
    private String mAppId;
    private static String mPlacementId;
    private AdConfig mAdConfig;
    private boolean mIsPlaying;


    public VungleInterstitial() {
        mHandler = new Handler(Looper.getMainLooper());
        sVungleRouter = VungleRouter.getInstance();
        mVungleAdapterConfiguration = new VungleAdapterConfiguration();
    }

    @Override
    protected void loadInterstitial(Context context,
                                    CustomEventInterstitialListener customEventInterstitialListener,
                                    Map<String, Object> localExtras,
                                    Map<String, String> serverExtras) {
        mCustomEventInterstitialListener = customEventInterstitialListener;
        mIsPlaying = false;

        setAutomaticImpressionAndClickTracking(false);

        if (context == null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);

                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);
                }
            });

            return;
        }

        if (!validateIdsInServerExtras(serverExtras)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);

                    MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);
                }
            });

            return;
        }

        if (mVungleRouterListener == null) {
            mVungleRouterListener = new VungleInterstitialRouterListener();
        }

        if (!sVungleRouter.isVungleInitialized()) {
            // No longer passing the placement IDs (pids) param per Vungle 6.3.17
            sVungleRouter.initVungle(context, mAppId);
            mVungleAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);
        }

        if (localExtras != null) {
            mAdConfig = new AdConfig();
            VungleMediationConfiguration.adConfigWithLocalExtras(mAdConfig, localExtras);
        }

        sVungleRouter.loadAdForPlacement(mPlacementId, mVungleRouterListener);
        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
    }

    @Override
    protected void showInterstitial() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

        if (sVungleRouter.isAdPlayableForPlacement(mPlacementId)) {

            sVungleRouter.playAdForPlacement(mPlacementId, mAdConfig);
            mIsPlaying = true;
        } else {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "SDK tried to show a Vungle interstitial ad before it " +
                    "finished loading. Please try again.");
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);

                    MoPubLog.log(getAdNetworkId(), SHOW_FAILED, ADAPTER_NAME,
                            MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                            MoPubErrorCode.NETWORK_NO_FILL);
                }
            });
        }
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                "onInvalidate is called for Placement ID:" + mPlacementId);
        sVungleRouter.removeRouterListener(mPlacementId);
        mVungleRouterListener = null;
        mAdConfig = null;
    }

    // private functions
    private boolean validateIdsInServerExtras(Map<String, String> serverExtras) {
        boolean isAllDataValid = true;

        if (serverExtras.containsKey(APP_ID_KEY)) {
            mAppId = serverExtras.get(APP_ID_KEY);
            if (mAppId != null && mAppId.isEmpty()) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "App ID is empty.");
                isAllDataValid = false;
            }
        } else {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "AppID is not in serverExtras.");
            isAllDataValid = false;
        }

        if (serverExtras.containsKey(PLACEMENT_ID_KEY)) {
            mPlacementId = serverExtras.get(PLACEMENT_ID_KEY);
            if (mPlacementId != null && mPlacementId.isEmpty()) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Placement ID for this Ad Unit is empty.");
                isAllDataValid = false;
            }
        } else {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                    "Placement ID for this Ad Unit is not in serverExtras.");
            isAllDataValid = false;
        }

        if (serverExtras.containsKey(PLACEMENT_IDS_KEY)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME + "No need to set placement IDs " +
                    "in MoPub dashboard with Vungle SDK version " +
                    com.vungle.warren.BuildConfig.VERSION_NAME);
        }

        return isAllDataValid;
    }

    private static String getAdNetworkId() {
        return mPlacementId;
    }

    /*
     * VungleRouterListener
     */
    private class VungleInterstitialRouterListener implements VungleRouterListener {
        @Override
        public void onAdEnd(String placementId) {
            if (mPlacementId.equals(placementId)) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdEnd - Placement ID: " + placementId);
                mIsPlaying = false;

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mCustomEventInterstitialListener != null) {
                            mCustomEventInterstitialListener.onInterstitialDismissed();
                        }
                    }
                });
                sVungleRouter.removeRouterListener(mPlacementId);
            }
        }

        @Override
        public void onAdClick(String placementId) {
            if (mPlacementId.equals(placementId)) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onAdClick - Placement ID: " + placementId);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mCustomEventInterstitialListener != null) {
                            mCustomEventInterstitialListener.onInterstitialClicked();
                        }

                        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
                    }
                });
            }
        }

        @Override
        public void onAdRewarded(String placementId) {
            //nothing to do
        }

        @Override
        public void onAdLeftApplication(String placementId) {
            // Only logging this event. No need to call interstitialListener.onLeaveApplication()
            // because it's an alias for interstitialListener.onInterstitialClicked()
            MoPubLog.log(getAdNetworkId(), WILL_LEAVE_APPLICATION, ADAPTER_NAME);
        }

        @Override
        public void onAdStart(@NonNull String placementReferenceId) {
            if (mPlacementId.equals(placementReferenceId)) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                        "onAdStart - Placement ID: " + placementReferenceId);
                mIsPlaying = true;

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mCustomEventInterstitialListener != null) {
                            mCustomEventInterstitialListener.onInterstitialShown();
                            mCustomEventInterstitialListener.onInterstitialImpression();
                        }

                        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
                    }
                });
            }
        }

        @Override
        public void onUnableToPlayAd(@NonNull String placementReferenceId, String reason) {
            if (mPlacementId.equals(placementReferenceId)) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "onUnableToPlayAd - Placement ID: " +
                        placementReferenceId + ", reason: " + reason);
                mIsPlaying = false;

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mCustomEventInterstitialListener != null) {
                            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                        }

                        MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                                MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                                MoPubErrorCode.NETWORK_NO_FILL);
                    }
                });
            }
        }

        @Override
        public void onAdAvailabilityUpdate(@NonNull String placementReferenceId, boolean isAdAvailable) {
            if (mPlacementId.equals(placementReferenceId)) {
                if (!mIsPlaying) {
                    if (isAdAvailable) {
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                                "interstitial ad successfully loaded - Placement ID: " + placementReferenceId);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mCustomEventInterstitialListener != null) {
                                    mCustomEventInterstitialListener.onInterstitialLoaded();
                                }
                                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
                            }
                        });
                    } else {
                        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME,
                                "interstitial ad is not loaded - Placement ID: " + placementReferenceId);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mCustomEventInterstitialListener != null) {
                                    mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                                }

                                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                                        MoPubErrorCode.NETWORK_NO_FILL);
                            }
                        });
                    }
                }
            }
        }
    }
}
