package com.wayside.utils

import com.wayside.models.ui.GeoPoint

/**
 * Decodes Google's encoded polyline — the shape of a route, sent as an ASCII string of deltas
 * rather than a list of coordinates.
 *
 * The API could expand the points before sending them, but the encoded form is what Routes
 * produces and it is an order of magnitude smaller over the wire for a long drive.
 */
object PolylineDecoder {

    fun decode(encoded: String?): List<GeoPoint> {
        if (encoded.isNullOrEmpty()) return emptyList()

        val points = mutableListOf<GeoPoint>()
        var index = 0
        var latitude = 0
        var longitude = 0

        while (index < encoded.length) {
            var shift = 0
            var result = 0
            var chunk: Int

            // Latitude delta: five-bit chunks, low bits first, bit 6 flagging "more to come".
            do {
                chunk = encoded[index++].code - 63
                result = result or ((chunk and 0x1f) shl shift)
                shift += 5
            } while (chunk >= 0x20 && index < encoded.length)
            // Zig-zag encoded, so odd values are negative.
            latitude += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            shift = 0
            result = 0
            do {
                chunk = encoded[index++].code - 63
                result = result or ((chunk and 0x1f) shl shift)
                shift += 5
            } while (chunk >= 0x20 && index < encoded.length)
            longitude += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            points += GeoPoint(latitude / 1e5, longitude / 1e5)
        }

        return points
    }
}
