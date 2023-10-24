/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.shopping.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import mozilla.components.lib.state.ext.observeAsState
import org.mozilla.fenix.shopping.store.BottomSheetDismissSource
import org.mozilla.fenix.shopping.store.ReviewQualityCheckAction
import org.mozilla.fenix.shopping.store.ReviewQualityCheckState
import org.mozilla.fenix.shopping.store.ReviewQualityCheckState.OptedIn.ProductReviewState.AnalysisPresent
import org.mozilla.fenix.shopping.store.ReviewQualityCheckStore

/**
 * Top-level UI for the Review Quality Check feature.
 *
 * @param store [ReviewQualityCheckStore] that holds the state.
 * @param onRequestDismiss Invoked when a user action requests dismissal of the bottom sheet.
 * @param modifier The modifier to be applied to the Composable.
 */
@Suppress("LongMethod")
@Composable
fun ReviewQualityCheckBottomSheet(
    store: ReviewQualityCheckStore,
    onRequestDismiss: (source: BottomSheetDismissSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reviewQualityCheckState by store.observeAsState(ReviewQualityCheckState.Initial) { it }
    val isOptedIn =
        remember(reviewQualityCheckState) { reviewQualityCheckState is ReviewQualityCheckState.OptedIn }

    ReviewQualityCheckScaffold(
        onRequestDismiss = {
            onRequestDismiss(BottomSheetDismissSource.HANDLE_CLICKED)
        },
        modifier = modifier.animateContentSize(),
    ) {
        when (val state = reviewQualityCheckState) {
            is ReviewQualityCheckState.NotOptedIn -> {
                ReviewQualityCheckContextualOnboarding(
                    productVendors = state.productVendors,
                    onPrimaryButtonClick = {
                        store.dispatch(ReviewQualityCheckAction.OptIn)
                    },
                    onLearnMoreClick = {
                        onRequestDismiss(BottomSheetDismissSource.LINK_OPENED)
                        store.dispatch(ReviewQualityCheckAction.OpenOnboardingLearnMoreLink)
                    },
                    onPrivacyPolicyClick = {
                        onRequestDismiss(BottomSheetDismissSource.LINK_OPENED)
                        store.dispatch(ReviewQualityCheckAction.OpenOnboardingPrivacyPolicyLink)
                    },
                    onTermsOfUseClick = {
                        onRequestDismiss(BottomSheetDismissSource.LINK_OPENED)
                        store.dispatch(ReviewQualityCheckAction.OpenOnboardingTermsLink)
                    },
                    onSecondaryButtonClick = {
                        onRequestDismiss(BottomSheetDismissSource.NOT_NOW)
                        store.dispatch(ReviewQualityCheckAction.NotNowClicked)
                    },
                )
            }

            is ReviewQualityCheckState.OptedIn -> {
                ProductReview(
                    state = state,
                    onOptOutClick = {
                        onRequestDismiss(BottomSheetDismissSource.OPT_OUT)
                        store.dispatch(ReviewQualityCheckAction.OptOut)
                    },
                    onAnalyzeClick = {
                        store.dispatch(ReviewQualityCheckAction.AnalyzeProduct)
                    },
                    onReanalyzeClick = {
                        store.dispatch(ReviewQualityCheckAction.ReanalyzeProduct)
                    },
                    onProductRecommendationsEnabledStateChange = {
                        store.dispatch(ReviewQualityCheckAction.ToggleProductRecommendation)
                    },
                    onReviewGradeLearnMoreClick = {
                        onRequestDismiss(BottomSheetDismissSource.LINK_OPENED)
                        store.dispatch(ReviewQualityCheckAction.OpenExplainerLearnMoreLink)
                    },
                    onFooterLinkClick = {
                        onRequestDismiss(BottomSheetDismissSource.LINK_OPENED)
                        store.dispatch(ReviewQualityCheckAction.OpenPoweredByLink)
                    },
                    onExpandSettings = {
                        store.dispatch(ReviewQualityCheckAction.ExpandSettingsClicked)
                    },
                    onNoAnalysisPresent = {
                        store.dispatch(ReviewQualityCheckAction.NoAnalysisDisplayed)
                    },
                    onShowMoreRecentReviewsClicked = {
                        store.dispatch(ReviewQualityCheckAction.ShowMoreRecentReviewsClicked)
                    },
                )
            }

            is ReviewQualityCheckState.Initial -> {}
        }
    }

    LaunchedEffect(isOptedIn) {
        if (isOptedIn) {
            store.dispatch(ReviewQualityCheckAction.FetchProductAnalysis)
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun ProductReview(
    state: ReviewQualityCheckState.OptedIn,
    onOptOutClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onReanalyzeClick: () -> Unit,
    onProductRecommendationsEnabledStateChange: (Boolean) -> Unit,
    onShowMoreRecentReviewsClicked: () -> Unit,
    onNoAnalysisPresent: () -> Unit,
    onExpandSettings: () -> Unit,
    onReviewGradeLearnMoreClick: () -> Unit,
    onFooterLinkClick: () -> Unit,
) {
    Crossfade(
        targetState = state.productReviewState,
        label = "ProductReview-Crossfade",
    ) { productReviewState ->
        when (productReviewState) {
            is AnalysisPresent -> {
                ProductAnalysis(
                    productRecommendationsEnabled = state.productRecommendationsPreference,
                    productAnalysis = productReviewState,
                    productVendor = state.productVendor,
                    onOptOutClick = onOptOutClick,
                    onReanalyzeClick = onReanalyzeClick,
                    onProductRecommendationsEnabledStateChange = onProductRecommendationsEnabledStateChange,
                    onShowMoreRecentReviewsClicked = onShowMoreRecentReviewsClicked,
                    onExpandSettings = onExpandSettings,
                    onReviewGradeLearnMoreClick = onReviewGradeLearnMoreClick,
                    onFooterLinkClick = onFooterLinkClick,
                )
            }

            is ReviewQualityCheckState.OptedIn.ProductReviewState.Error -> {
                ProductAnalysisError(
                    error = productReviewState,
                    productRecommendationsEnabled = state.productRecommendationsPreference,
                    productVendor = state.productVendor,
                    onReviewGradeLearnMoreClick = onReviewGradeLearnMoreClick,
                    onOptOutClick = onOptOutClick,
                    onProductRecommendationsEnabledStateChange = onProductRecommendationsEnabledStateChange,
                    onFooterLinkClick = onFooterLinkClick,
                    onExpandSettings = onExpandSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is ReviewQualityCheckState.OptedIn.ProductReviewState.Loading -> {
                ProductReviewLoading()
            }

            is ReviewQualityCheckState.OptedIn.ProductReviewState.NoAnalysisPresent -> {
                LaunchedEffect(Unit) {
                    onNoAnalysisPresent()
                }

                NoAnalysis(
                    isAnalyzing = productReviewState.isReanalyzing,
                    productRecommendationsEnabled = state.productRecommendationsPreference,
                    productVendor = state.productVendor,
                    onAnalyzeClick = onAnalyzeClick,
                    onReviewGradeLearnMoreClick = onReviewGradeLearnMoreClick,
                    onOptOutClick = onOptOutClick,
                    onProductRecommendationsEnabledStateChange = onProductRecommendationsEnabledStateChange,
                    onExpandSettings = onExpandSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}