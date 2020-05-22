package com.veinhorn.scrollgalleryview.builder;


import androidx.fragment.app.FragmentManager;

public class GallerySettings {
    private int thumbnailSize;
    private boolean isZoomEnabled;
    private FragmentManager fragmentManager;

    public int getThumbnailSize() {
        return thumbnailSize;
    }

    public void setThumbnailSize(int thumbnailSize) {
        this.thumbnailSize = thumbnailSize;
    }

    public boolean isZoomEnabled() {
        return isZoomEnabled;
    }

    public void setZoomEnabled(boolean zoomEnabled) {
        isZoomEnabled = zoomEnabled;
    }

    public FragmentManager getFragmentManager() {
        return fragmentManager;
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public static GallerySettingsBuilder from(FragmentManager fm) {
        GallerySettingsBuilder builder = new GallerySettingsBuilderImpl();
        builder.withFragmentManager(fm);
        return builder;
    }
}
