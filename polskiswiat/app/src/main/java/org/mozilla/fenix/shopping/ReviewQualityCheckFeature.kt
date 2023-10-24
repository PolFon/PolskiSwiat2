/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.shopping

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.LifecycleAwareFeature
import org.mozilla.fenix.components.AppStore

private const val DEBOUNCE_TIMEOUT_MILLIS = 200L

/**
 * Feature implementation that provides review quality check information for supported product
 * pages.
 *
 * @property appStore Reference to the application's [AppStore].
 * @property browserStore Reference to the application's [BrowserStore].
 * @property shoppingExperienceFeature Reference to the [ShoppingExperienceFeature].
 * @property onAvailabilityChange Invoked when availability of this feature changes based on feature
 * flag and when the loaded page is a supported product page.
 * @property onBottomSheetStateChange Invoked when the bottom sheet is collapsed or expanded.
 * @property debounceTimeoutMillis Function that returns the debounce timeout in milliseconds. This
 * make it possible to wait till [ContentState.isProductUrl] is stable before invoking
 * [onAvailabilityChange].
 */
@OptIn(FlowPreview::class)
class ReviewQualityCheckFeature(
    private val appStore: AppStore,
    private val browserStore: BrowserStore,
    private val shoppingExperienceFeature: ShoppingExperienceFeature,
    private val onAvailabilityChange: (isAvailable: Boolean) -> Unit,
    private val onBottomSheetStateChange: (isExpanded: Boolean) -> Unit,
    private val debounceTimeoutMillis: (Boolean) -> Long = { if (it) DEBOUNCE_TIMEOUT_MILLIS else 0 },
) : LifecycleAwareFeature {
    private var scope: CoroutineScope? = null
    private var appStoreScope: CoroutineScope? = null

    override fun start() {
        if (!shoppingExperienceFeature.isEnabled) {
            onAvailabilityChange(false)
            return
        }

        scope = browserStore.flowScoped { flow ->
            flow.mapNotNull { it.selectedTab }
                .map { it.content.isProductUrl && !it.content.loading }
                .distinctUntilChanged()
                .debounce(debounceTimeoutMillis)
                .collect(onAvailabilityChange)
        }

        appStoreScope = appStore.flowScoped { flow ->
            flow.mapNotNull { it.shoppingState.shoppingSheetExpanded }
                .distinctUntilChanged()
                .collect(onBottomSheetStateChange)
        }
    }

    override fun stop() {
        scope?.cancel()
        appStoreScope?.cancel()
    }
}