package com.wayside.services.interfaces.api

/**
 * Builds the URLs the app loads place photos from.
 *
 * They point at our own API, not at Google: fetching a photo needs the Maps key, and that key
 * stays on the server. The API answers with a redirect to a signed, expiring URL.
 */
interface PhotoUrlBuilder {
    fun photoUrl(placeId: String, index: Int): String
}
