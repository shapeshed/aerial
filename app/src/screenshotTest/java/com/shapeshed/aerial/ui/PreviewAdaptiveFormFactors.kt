package com.shapeshed.aerial.ui

import androidx.compose.ui.tooling.preview.Preview

/**
 * Multi-preview annotation covering the compact, medium, and expanded width
 * buckets at short, standard, and tall heights (400/610/900dp x 400/500/1000dp).
 * Applied by the adaptive screenshot previews.
 */
@Preview(name = "400x400", device = "spec:width=400dp,height=400dp,dpi=420")
@Preview(name = "400x500", device = "spec:width=400dp,height=500dp,dpi=420")
@Preview(name = "400x1000", device = "spec:width=400dp,height=1000dp,dpi=420")
@Preview(name = "610x400", device = "spec:width=610dp,height=400dp,dpi=420")
@Preview(name = "610x500", device = "spec:width=610dp,height=500dp,dpi=420")
@Preview(name = "610x1000", device = "spec:width=610dp,height=1000dp,dpi=420")
@Preview(name = "900x400", device = "spec:width=900dp,height=400dp,dpi=420")
@Preview(name = "900x500", device = "spec:width=900dp,height=500dp,dpi=420")
@Preview(name = "900x1000", device = "spec:width=900dp,height=1000dp,dpi=420")
annotation class PreviewAdaptiveFormFactors
