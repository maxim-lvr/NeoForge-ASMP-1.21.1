package net.maximlvr.asmpthings.integration.camera;

public final class CrazyPhoneCaptureState {
    private static boolean takingScreenshot;

    private CrazyPhoneCaptureState() {
    }

    public static boolean isTakingScreenshot() {
        return takingScreenshot;
    }

    public static void setTakingScreenshot(boolean takingScreenshot) {
        CrazyPhoneCaptureState.takingScreenshot = takingScreenshot;
    }
}
