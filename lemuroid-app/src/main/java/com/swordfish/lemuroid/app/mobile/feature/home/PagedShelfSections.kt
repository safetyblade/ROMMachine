package com.swordfish.lemuroid.app.mobile.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfLayout
import kotlinx.coroutines.launch

/** Browse-only: editing still receives the full layout and can move cells between sections. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PagedShelfSections(
    layout: ShelfLayout,
    modifier: Modifier,
    enabled: Boolean,
    content: @Composable (ShelfLayout, Modifier, String) -> Unit,
) {
    val sections = layout.sections
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    if (!enabled || sections.size <= 1) {
        content(layout, modifier, "all")
        return
    }
    val initialPage = sections.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = initialPage, pageCount = { sections.size })
    LaunchedEffect(pager, sections) {
        snapshotFlow { pager.settledPage }.collect { index ->
            selectedId = sections.getOrNull(index)?.id
        }
    }
    val scope = rememberCoroutineScope()
    Column(modifier) {
        if (sections.size > 1) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sections.forEachIndexed { index, section ->
                    FilterChip(
                        selected = pager.currentPage == index,
                        onClick = { scope.launch { pager.animateScrollToPage(index) } },
                        label = { Text(section.title, maxLines = 1) },
                    )
                }
            }
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            key = { sections[it].id },
        ) { index ->
            val section = sections[index]
            content(layout.copy(sections = listOf(section)), Modifier.fillMaxSize(), "section:${section.id}")
        }
    }
}
