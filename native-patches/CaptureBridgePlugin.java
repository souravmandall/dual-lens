package com.duallens.camera;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.OutputStream;

@CapacitorPlugin(name = "CaptureBridge")
public class CaptureBridgePlugin extends Plugin {

    // Tells the web app whether it was launched by another app/website asking for a photo
    // (e.g. a "Capture Image" button on a web form), instead of being opened normally.
    @PluginMethod
    public void isCaptureRequest(PluginCall call) {
        Intent intent = getActivity().getIntent();
        boolean isCapture = intent != null
                && (MediaStore.ACTION_IMAGE_CAPTURE.equals(intent.getAction())
                    || "android.media.action.IMAGE_CAPTURE".equals(intent.getAction()));
        JSObject ret = new JSObject();
        ret.put("value", isCapture);
        call.resolve(ret);
    }

    // Writes the captured JPEG back to the location the calling app asked for, then
    // closes this app and hands control back to the caller.
    @PluginMethod
    public void returnCapture(PluginCall call) {
        String base64Data = call.getString("base64");
        if (base64Data == null) {
            call.reject("base64 data missing");
            return;
        }
        try {
            byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
            Intent intent = getActivity().getIntent();
            Uri outputUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);

            if (outputUri != null) {
                OutputStream out = getActivity().getContentResolver().openOutputStream(outputUri);
                if (out != null) {
                    out.write(bytes);
                    out.flush();
                    out.close();
                }
                getActivity().setResult(android.app.Activity.RESULT_OK);
            } else {
                // Caller didn't give us a place to write to — nothing we can safely return.
                getActivity().setResult(android.app.Activity.RESULT_CANCELED);
            }
            call.resolve();
            getActivity().finish();
        } catch (Exception e) {
            call.reject("Failed to return capture: " + e.getMessage());
        }
    }

    @PluginMethod
    public void cancelCapture(PluginCall call) {
        getActivity().setResult(android.app.Activity.RESULT_CANCELED);
        call.resolve();
        getActivity().finish();
    }
}
