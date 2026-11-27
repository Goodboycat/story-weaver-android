package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ai.AiProvider
import com.example.data.AppStrings
import com.example.data.Localization
import com.example.data.TextScalePreset
import com.example.data.ThemePreset
import com.example.data.UserSettings
import com.example.ui.components.AiModelSelectorBar
import com.example.ui.theme.AccentSwatches

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    strings: AppStrings,
    settings: UserSettings,
    userEmail: String?,
    onSelectTheme: (ThemePreset) -> Unit,
    onSelectAccent: (Long?) -> Unit,
    onSelectTextScale: (TextScalePreset) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onSelectAiProvider: (AiProvider) -> Unit,
    onSelectAiModel: (String) -> Unit,
    onSelectAiEndpointUrl: (String) -> Unit,
    onSelectAiApiKey: (String) -> Unit,
    onSelectAiImageEndpointUrl: (String) -> Unit,
    onSelectAiImageModel: (String) -> Unit,
    onSelectAiImageApiKey: (String) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.settings, style = MaterialTheme.typography.headlineSmall)
                if (!userEmail.isNullOrBlank()) {
                    Text(
                        userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Theme Presets
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(strings.themeLabel, style = MaterialTheme.typography.titleMedium)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = settings.themePreset == preset,
                        onClick = { onSelectTheme(preset) },
                        label = { Text(preset.label) },
                        modifier = Modifier.testTag("theme_preset_${preset.key}")
                    )
                }
            }

            // Custom Accent Swatches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.accentColorLabel, style = MaterialTheme.typography.titleMedium)
                if (settings.customAccentArgb != null) {
                    TextButton(onClick = { onSelectAccent(null) }) {
                        Text("Reset")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccentSwatches.forEach { (hex, label) ->
                    val argb = 0xFF000000L or hex
                    val selected = settings.customAccentArgb == argb
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(argb.toInt()))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onSelectAccent(argb) }
                            .semantics { contentDescription = label }
                            .testTag("accent_swatch_${label.lowercase().replace(" ", "_")}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Text Size
            Text(strings.textSizeLabel, style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextScalePreset.entries.forEach { scale ->
                    FilterChip(
                        selected = settings.textScale == scale,
                        onClick = { onSelectTextScale(scale) },
                        label = { Text(scale.label) },
                        modifier = Modifier.testTag("text_scale_${scale.key}")
                    )
                }
            }

            // 12-Language Picker
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Localization.supportedLanguages.forEach { lang ->
                    FilterChip(
                        selected = settings.languageCode == lang.code,
                        onClick = { onSelectLanguage(lang.code) },
                        label = { Text(lang.nativeName) },
                        modifier = Modifier.testTag("language_${lang.code}")
                    )
                }
            }

            HorizontalDivider()

            AiModelSelectorBar(
                selectedProvider = settings.aiProvider,
                selectedModel = settings.aiModel,
                onSelectProvider = onSelectAiProvider,
                onSelectModel = onSelectAiModel,
                endpointUrl = settings.aiEndpointUrl,
                apiKey = settings.aiApiKey,
                onSelectEndpointUrl = onSelectAiEndpointUrl,
                onSelectApiKey = onSelectAiApiKey,
                imageEndpointUrl = settings.aiImageEndpointUrl,
                imageModel = settings.aiImageModel,
                imageApiKey = settings.aiImageApiKey,
                onSelectImageEndpointUrl = onSelectAiImageEndpointUrl,
                onSelectImageModel = onSelectAiImageModel,
                onSelectImageApiKey = onSelectAiImageApiKey,
                showAdvancedApiConfig = true,
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Story Weaver Mobile", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Native Android Jetpack Compose + Cloud Firestore + Firebase AI Logic & Unorouter API.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.signOut)
            }
        }
    }
}
