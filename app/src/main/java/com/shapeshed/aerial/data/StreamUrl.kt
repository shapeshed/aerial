package com.shapeshed.aerial.data

private val BAUER_DOMAIN_REGEX = Regex(
    """hellorayo\.co\.uk|sharp-stream\.com|bauerradio\.com|planetradio\.co\.uk|audioxi\.(com|ie)""",
    RegexOption.IGNORE_CASE,
)
private val BAUER_SKEY_REGEX = Regex("""([?&])aw_0_1st\.skey=[^&]*""")

/** Adds Bauer's short-lived stream key where the stream host requires one. */
fun bauerStreamUrl(station: Station): String {
    if (!BAUER_DOMAIN_REGEX.containsMatchIn(station.streamUrl)) return station.streamUrl
    val skey = System.currentTimeMillis() / 1000
    val sanitizedUrl = station.streamUrl
        .replace(BAUER_SKEY_REGEX, "")
        .replace("?&", "?")
        .replace("&&", "&")
        .trimEnd('?', '&')
    val separator = if ('?' in sanitizedUrl) '&' else '?'
    return "${sanitizedUrl}${separator}aw_0_1st.skey=$skey"
}
