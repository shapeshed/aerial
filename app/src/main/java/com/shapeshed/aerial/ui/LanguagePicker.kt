package com.shapeshed.aerial.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.shapeshed.aerial.R

/** A supported UI language; [autonym] is the language's own name and is intentionally not translated. */
data class AppLanguage(val tag: String, val autonym: String)

// Must stay in sync with res/xml/locales_config.xml and the values-* resource dirs.
val APP_LANGUAGES: List<AppLanguage> = listOf(
    AppLanguage("en", "English (US)"),
    AppLanguage("en-GB", "English (UK)"),
    AppLanguage("es", "Español"),
    AppLanguage("fr", "Français"),
    AppLanguage("de", "Deutsch"),
    AppLanguage("it", "Italiano"),
    AppLanguage("pt", "Português"),
    AppLanguage("nl", "Nederlands"),
    AppLanguage("zh-CN", "简体中文"),
    AppLanguage("ja", "日本語"),
    AppLanguage("ko", "한국어"),
)

private fun setAppLanguage(tag: String) {
    val locales = if (tag.isBlank()) LocaleListCompat.getEmptyLocaleList()
    else LocaleListCompat.forLanguageTags(tag)
    AppCompatDelegate.setApplicationLocales(locales)
}

/**
 * Settings row + dialog for choosing the app language. Only shown on API < 33; on Android 13+
 * the system per-app language picker (Settings → Apps → Aerial → Language) is used instead.
 */
@Composable
fun LanguageSettingRow() {
    var showDialog by remember { mutableStateOf(false) }
    val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val currentLabel = APP_LANGUAGES.firstOrNull { it.tag.equals(currentTag, ignoreCase = true) }?.autonym
        ?: stringResource(R.string.language_system_default)

    ListItem(
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true },
        leadingContent = { Icon(Icons.Rounded.Language, contentDescription = null) },
        supportingContent = { Text(currentLabel) },
    ) {
        Text(stringResource(R.string.language))
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 420.dp)) {
                    LanguageOption(
                        label = stringResource(R.string.language_system_default),
                        selected = currentTag.isBlank(),
                        onClick = { setAppLanguage(""); showDialog = false },
                    )
                    APP_LANGUAGES.forEach { lang ->
                        LanguageOption(
                            label = lang.autonym,
                            selected = currentTag.equals(lang.tag, ignoreCase = true),
                            onClick = { setAppLanguage(lang.tag); showDialog = false },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
