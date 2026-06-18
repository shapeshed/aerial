package com.shapeshed.aerial.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.shapeshed.aerial.R

private val GoogleSansFlexFamily = FontFamily(
    Font(R.font.google_sans_flex_light, weight = FontWeight.Normal),
    Font(R.font.google_sans_flex_regular, weight = FontWeight.Medium),
    Font(R.font.google_sans_flex_medium, weight = FontWeight.SemiBold),
    Font(R.font.google_sans_flex_semibold, weight = FontWeight.Bold),
)

private val base = Typography()

val AerialTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = GoogleSansFlexFamily),
    displayMedium = base.displayMedium.copy(fontFamily = GoogleSansFlexFamily),
    displaySmall = base.displaySmall.copy(fontFamily = GoogleSansFlexFamily),
    headlineLarge = base.headlineLarge.copy(fontFamily = GoogleSansFlexFamily),
    headlineMedium = base.headlineMedium.copy(fontFamily = GoogleSansFlexFamily),
    headlineSmall = base.headlineSmall.copy(fontFamily = GoogleSansFlexFamily),
    titleLarge = base.titleLarge.copy(fontFamily = GoogleSansFlexFamily),
    titleMedium = base.titleMedium.copy(fontFamily = GoogleSansFlexFamily),
    titleSmall = base.titleSmall.copy(fontFamily = GoogleSansFlexFamily),
    bodyLarge = base.bodyLarge.copy(fontFamily = GoogleSansFlexFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = GoogleSansFlexFamily),
    bodySmall = base.bodySmall.copy(fontFamily = GoogleSansFlexFamily),
    labelLarge = base.labelLarge.copy(fontFamily = GoogleSansFlexFamily),
    labelMedium = base.labelMedium.copy(fontFamily = GoogleSansFlexFamily),
    labelSmall = base.labelSmall.copy(fontFamily = GoogleSansFlexFamily),
)
