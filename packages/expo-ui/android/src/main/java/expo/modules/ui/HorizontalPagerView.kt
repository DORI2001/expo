package expo.modules.ui

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.unit.dp
import androidx.core.view.size
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import expo.modules.kotlin.views.ComposeProps
import expo.modules.kotlin.views.FunctionalComposableScope
import expo.modules.kotlin.types.Either
import expo.modules.kotlin.types.OptimizedRecord
import expo.modules.kotlin.views.OptimizedComposeProps

@OptimizedRecord
data class HorizontalPagerPageSelectedEvent(
  @Field val position: Int = 0
) : Record

@OptimizedComposeProps
data class HorizontalPagerProps(
  val currentPage: Int? = null,
  val defaultPage: Int = 0,
  val animatePageChanges: Boolean = true,
  val pageSpacing: Float = 0f,
  val contentPadding: Either<Float, PaddingValuesRecord>? = null,
  val userScrollEnabled: Boolean = true,
  val reverseLayout: Boolean = false,
  val beyondViewportPageCount: Int = 0,
  val modifiers: ModifierList = emptyList()
) : ComposeProps

@Composable
fun FunctionalComposableScope.HorizontalPagerContent(
  props: HorizontalPagerProps,
  onPageSelected: (HorizontalPagerPageSelectedEvent) -> Unit
) {
  val pageCount = view.size
  val initialPage = props.currentPage ?: props.defaultPage
  val pagerState = rememberPagerState(
    initialPage = initialPage
  ) { pageCount }

  // Suppresses onPageSelected during animated programmatic scrolls.
  // Without this, cancelling animateScrollToPage mid-flight causes the
  // pager to settle at an intermediate page, firing onPageSelected with
  // that value, which feeds back into currentPage and starts a new
  // animation — creating an infinite loop.
  val isProgrammaticScroll = remember { mutableStateOf(false) }

  LaunchedEffect(props.currentPage) {
    val target = props.currentPage ?: return@LaunchedEffect
    if (pagerState.currentPage != target) {
      if (props.animatePageChanges) {
        isProgrammaticScroll.value = true
        try {
          pagerState.animateScrollToPage(target)
        } finally {
          isProgrammaticScroll.value = false
        }
      } else {
        pagerState.scrollToPage(target)
      }
    }
  }

  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.settledPage }
      .drop(1)
      .collect { page ->
        if (!isProgrammaticScroll.value) {
          onPageSelected(HorizontalPagerPageSelectedEvent(page))
        }
      }
  }

  val contentPadding = paddingValuesFromEither(props.contentPadding)

  HorizontalPager(
    state = pagerState,
    modifier = ModifierRegistry.applyModifiers(props.modifiers, appContext, composableScope, globalEventDispatcher),
    contentPadding = contentPadding,
    pageSpacing = props.pageSpacing.dp,
    userScrollEnabled = props.userScrollEnabled,
    reverseLayout = props.reverseLayout,
    beyondViewportPageCount = props.beyondViewportPageCount
  ) { pageIndex ->
    Child(UIComposableScope(), pageIndex)
  }
}
