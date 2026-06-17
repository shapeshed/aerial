package com.shapeshed.aerial.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.shapeshed.aerial.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val googleSans = GoogleFont("Google Sans Flex")

private val GoogleSansFamily = FontFamily(
    Font(googleFont = googleSans, fontProvider = provider, weight = FontWeight.Thin),
    Font(googleFont = googleSans, fontProvider = provider, weight = FontWeight.ExtraLight),
    Font(googleFont = googleSans, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = googleSans, fontProvider = provider, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(googleFont = googleSans, fontProvider = provider, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(googleFont = googleSans, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = googleSans, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = googleSans, fontProvider = provider, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(googleFont = googleSans, fontProvider = provider, weight = FontWeight.Bold, style = FontStyle.Italic),
    Font(googleFont = googleSans, fontProvider = provider, weight = FontWeight.ExtraBold),
    Font(googleFont = googleSans, fontProvider = provider, weight = FontWeight.Black),
)

private val base = Typography()

val AerialTypography = Typography(
    displayLarge  = base.displayLarge.copy(fontFamily  = GoogleSansFamily),
    displayMedium = base.displayMedium.copy(fontFamily = GoogleSansFamily),
    displaySmall  = base.displaySmall.copy(fontFamily  = GoogleSansFamily),
    headlineLarge  = base.headlineLarge.copy(fontFamily  = GoogleSansFamily),
    headlineMedium = base.headlineMedium.copy(fontFamily = GoogleSansFamily),
    headlineSmall  = base.headlineSmall.copy(fontFamily  = GoogleSansFamily),
    titleLarge  = base.titleLarge.copy(fontFamily  = GoogleSansFamily),
    titleMedium = base.titleMedium.copy(fontFamily = GoogleSansFamily),
    titleSmall  = base.titleSmall.copy(fontFamily  = GoogleSansFamily),
    bodyLarge  = base.bodyLarge.copy(fontFamily  = GoogleSansFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = GoogleSansFamily),
    bodySmall  = base.bodySmall.copy(fontFamily  = GoogleSansFamily),
    labelLarge  = base.labelLarge.copy(fontFamily  = GoogleSansFamily),
    labelMedium = base.labelMedium.copy(fontFamily = GoogleSansFamily),
    labelSmall  = base.labelSmall.copy(fontFamily  = GoogleSansFamily),
)
