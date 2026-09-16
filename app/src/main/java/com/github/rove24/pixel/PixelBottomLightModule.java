package com.github.rove24.pixel;

import android.util.Log;
import androidx.annotation.NonNull;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class PixelBottomLightModule extends XposedModule {

    private static final String TAG = "PixelBottomLight";
    private static final String TARGET_PKG = "com.android.systemui";
    private static final String TARGET_VIEW_CLASS = "com.android.systemui.assist.ui.InvocationLightsView";

    private static final int[] GOOGLE_COLORS = new int[]{
            0xFF4285F4, // Blue
            0xFFEA4335, // Red
            0xFFFBBC05, // Yellow
            0xFF34A853  // Green
    };

    // Cached reflection references (resolve once, use forever)
    private static volatile boolean sFieldsResolved = false;
    private static Field sUseNavBarColorField;
    private static Field sLightsField;
    private static Method sSetColorMethod;
    private static Field sColorField;

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        log(Log.INFO, TAG, "PixelBottomLight v0.5 loaded in: " + param.getProcessName());
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        if (!TARGET_PKG.equals(param.getPackageName())) {
            return;
        }

        try {
            ClassLoader classLoader = param.getClassLoader();
            Class<?> lightsViewClass = Class.forName(TARGET_VIEW_CLASS, true, classLoader);
            log(Log.INFO, TAG, "Found target class: " + lightsViewClass.getName());

            // Hook 1: onFinishInflate - set Google colors after view is inflated
            try {
                Method onFinishInflateMethod = lightsViewClass.getDeclaredMethod("onFinishInflate");
                hook(onFinishInflateMethod).intercept(chain -> {
                    Object result = chain.proceed();
                    applyGoogleColors(chain.getThisObject());
                    return result;
                });
                log(Log.INFO, TAG, "Hooked onFinishInflate");
            } catch (NoSuchMethodException e) {
                log(Log.WARN, TAG, "onFinishInflate not found: " + e.getMessage());
            }

            // Hook 2: updateDarkness - block it to prevent single-color override
            try {
                Method updateDarknessMethod = lightsViewClass.getDeclaredMethod("updateDarkness", float.class);
                hook(updateDarknessMethod).intercept(chain -> {
                    // Block entirely - prevents navbar color from overwriting our 4 colors
                    return null;
                });
                log(Log.INFO, TAG, "Hooked updateDarkness");
            } catch (NoSuchMethodException e) {
                log(Log.WARN, TAG, "updateDarkness not found: " + e.getMessage());
            }

            // Hook 3: setColors - intercept and apply our 4 colors instead of single color
            try {
                Method setColorsMethod = lightsViewClass.getDeclaredMethod("setColors", Integer.class);
                hook(setColorsMethod).intercept(chain -> {
                    Object viewObj = chain.getThisObject();
                    // When setColors(null) is called, it sets mUseNavBarColor=true and registers listener.
                    // When setColors(color) is called, it sets all 4 segments to same color.
                    // In both cases, we override with our Google colors instead.
                    applyGoogleColors(viewObj);
                    return null;
                });
                log(Log.INFO, TAG, "Hooked setColors");
            } catch (NoSuchMethodException e) {
                log(Log.WARN, TAG, "setColors not found: " + e.getMessage());
            }

        } catch (ClassNotFoundException e) {
            log(Log.INFO, TAG, "InvocationLightsView not found, skipping.");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Error during init", t);
        }
    }

    /**
     * Walk up the class hierarchy to find a declared field.
     * getDeclaredField only searches the exact class, not parents.
     */
    private static Field findFieldInHierarchy(Class<?> clazz, String fieldName)
            throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName + " not found in hierarchy of " + clazz.getName());
    }

    /**
     * Resolve and cache all reflection references on first call.
     */
    private void resolveFields(Object viewObj) throws NoSuchFieldException {
        if (sFieldsResolved) return;

        synchronized (PixelBottomLightModule.class) {
            if (sFieldsResolved) return;

            Class<?> clazz = viewObj.getClass();

            // Find mUseNavBarColor (boolean) - may not exist on all ROMs
            try {
                sUseNavBarColorField = findFieldInHierarchy(clazz, "mUseNavBarColor");
                sUseNavBarColorField.setAccessible(true);
                log(Log.INFO, TAG, "Resolved mUseNavBarColor on " + sUseNavBarColorField.getDeclaringClass().getSimpleName());
            } catch (NoSuchFieldException e) {
                log(Log.WARN, TAG, "mUseNavBarColor not found in hierarchy");
            }

            // Find mAssistInvocationLights (ArrayList<EdgeLight>) - required
            sLightsField = findFieldInHierarchy(clazz, "mAssistInvocationLights");
            sLightsField.setAccessible(true);
            log(Log.INFO, TAG, "Resolved mAssistInvocationLights on " + sLightsField.getDeclaringClass().getSimpleName());

            sFieldsResolved = true;
        }
    }

    /**
     * Resolve EdgeLight.setColor(int) or fallback to mColor field on first call.
     */
    private void resolveEdgeLightMethods(Object edgeLight) {
        if (sSetColorMethod != null || sColorField != null) return;

        Class<?> elClass = edgeLight.getClass();
        try {
            sSetColorMethod = elClass.getDeclaredMethod("setColor", int.class);
            sSetColorMethod.setAccessible(true);
            log(Log.INFO, TAG, "Resolved EdgeLight.setColor()");
        } catch (NoSuchMethodException e) {
            try {
                sColorField = findFieldInHierarchy(elClass, "mColor");
                sColorField.setAccessible(true);
                log(Log.INFO, TAG, "Resolved EdgeLight.mColor field");
            } catch (NoSuchFieldException ex) {
                log(Log.ERROR, TAG, "Cannot find setColor or mColor on EdgeLight");
            }
        }
    }

    private void applyGoogleColors(Object viewObj) {
        if (viewObj == null) return;

        try {
            resolveFields(viewObj);

            // Set mUseNavBarColor = false to enable per-segment color rendering with Cap.ROUND
            if (sUseNavBarColorField != null) {
                sUseNavBarColorField.setBoolean(viewObj, false);
            }

            // Get the 4 EdgeLight objects and set Google colors
            Object lightsObj = sLightsField.get(viewObj);
            if (lightsObj instanceof List<?>) {
                List<?> lightsList = (List<?>) lightsObj;
                int count = Math.min(lightsList.size(), GOOGLE_COLORS.length);
                for (int i = 0; i < count; i++) {
                    Object edgeLight = lightsList.get(i);
                    if (edgeLight != null) {
                        resolveEdgeLightMethods(edgeLight);
                        setEdgeLightColor(edgeLight, GOOGLE_COLORS[i]);
                    }
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to apply Google colors: " + t.getMessage());
        }
    }

    private void setEdgeLightColor(Object edgeLight, int color) throws Throwable {
        if (sSetColorMethod != null) {
            sSetColorMethod.invoke(edgeLight, color);
        } else if (sColorField != null) {
            sColorField.setInt(edgeLight, color);
        }
    }
}