package com.yourteam.deliveryagent.core

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Utilities for opening Google Maps navigation via Intent.
 * Handles fallback to web URL if Maps app is not installed.
 */
object MapsNavigationUtils {

    /**
     * Open Google Maps with navigation to the given coordinates.
     *
     * @param context Android context
     * @param latitude Destination latitude
     * @param longitude Destination longitude
     * @param label Optional label for the destination (shown in Maps)
     */
    fun openMapsNavigation(
        context: Context,
        latitude: Double,
        longitude: Double,
        label: String = "Destination",
    ) {
        // Try to open with Google Maps app (geo: URI scheme)
        val mapsIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("google.navigation:q=$latitude,$longitude&mode=d")
            `package` = "com.google.android.apps.maps"
        }

        try {
            context.startActivity(mapsIntent)
        } catch (e: Exception) {
            // Maps app not installed; fallback to web URL
            openMapsWebUrl(context, latitude, longitude, label)
        }
    }

    /**
     * Open Google Maps in the browser as a fallback.
     */
    private fun openMapsWebUrl(
        context: Context,
        latitude: Double,
        longitude: Double,
        label: String,
    ) {
        val url = "https://www.google.com/maps/search/$latitude,$longitude/@$latitude,$longitude,15z"
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

        try {
            context.startActivity(webIntent)
        } catch (e: Exception) {
            // No maps app and no browser; silently fail
        }
    }

    /**
     * Open the phone dialer with the given phone number pre-filled.
     *
     * @param context Android context
     * @param phoneNumber Phone number to dial (e.g., "+919876543210")
     */
    fun openPhoneDialer(context: Context, phoneNumber: String) {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }

        try {
            context.startActivity(dialIntent)
        } catch (e: Exception) {
            // Dialer app not available; silently fail
        }
    }
}
