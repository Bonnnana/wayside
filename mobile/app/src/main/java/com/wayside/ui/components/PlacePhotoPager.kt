package com.wayside.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * A place's photos, swiped through like any map app.
 *
 * Each page is a separate request to our API, which redirects to a signed Google URL, so a
 * photo is only ever fetched when it is actually looked at — photos are billed per fetch.
 */
@Composable
fun PlacePhotoPager(
    placeId: String,
    photoCount: Int,
    photoUrl: (String, Int) -> String,
    placeholderColor: Color,
    modifier: Modifier = Modifier,
    /** Varies the drawn placeholder when there's no photo to show instead. */
    placeholderSeed: Int = 0,
) {
    // A place with no photos still needs a header, and the category colour keeps it in family.
    if (photoCount <= 0) {
        PlaceholderImage(color = placeholderColor, seed = placeholderSeed, modifier = modifier)
        return
    }

    val pagerState = rememberPagerState(pageCount = { photoCount })

    Box(modifier) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = photoUrl(placeId, page),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (photoCount > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                PageIndicator(activeIndex = pagerState.currentPage, count = photoCount)
            }
        }
    }
}
