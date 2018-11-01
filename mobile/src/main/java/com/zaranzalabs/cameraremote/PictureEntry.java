package com.zaranzalabs.cameraremote;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.Map;

@IgnoreExtraProperties
public class PictureEntry
{
    Long timestamp;
    String image;
    Map<String, Float> annotations;

    public PictureEntry() {
    }

    public PictureEntry(Long timestamp, String image, Map<String, Float> annotations) {
        this.timestamp = timestamp;
        this.image = image;
        this.annotations = annotations;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getImage() {
        return image;
    }

    public Map<String, Float> getAnnotations() {
        return annotations;
    }
}
