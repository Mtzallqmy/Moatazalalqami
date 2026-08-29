package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Code
import me.rerere.hugeicons.stroke.File02
import me.rerere.hugeicons.stroke.Github
import me.rerere.hugeicons.stroke.SmartPhone01
import me.rerere.rikkahub.BuildConfig
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.openUrl
import me.rerere.rikkahub.utils.plus

@Composable
fun SettingAboutPage() {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.about_page_title)) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AsyncImage(
                        model = R.mipmap.ic_launcher,
                        contentDescription = "Moataz Alaqami",
                        modifier = Modifier.size(112.dp).clip(RoundedCornerShape(28.dp)),
                    )
                    Text("Moataz Alaqami", style = MaterialTheme.typography.headlineLarge)
                    Text("Android AI Agent", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
            item {
                CardGroup {
                    item(
                        leadingContent = { Icon(HugeIcons.Code, null) },
                        supportingContent = { Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") },
                        headlineContent = { Text(stringResource(R.string.about_page_version)) },
                    )
                    item(
                        leadingContent = { Icon(HugeIcons.SmartPhone01, null) },
                        supportingContent = { Text("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.RELEASE} · SDK ${android.os.Build.VERSION.SDK_INT}") },
                        headlineContent = { Text(stringResource(R.string.about_page_system)) },
                    )
                }
            }
            item {
                CardGroup(title = { Text("Project") }) {
                    item(
                        onClick = { context.openUrl("https://github.com/Mtzallqmy/Moatazalalqami") },
                        leadingContent = { Icon(HugeIcons.Github, null) },
                        supportingContent = { Text("github.com/Mtzallqmy/Moatazalalqami") },
                        headlineContent = { Text("Source code") },
                    )
                    item(
                        onClick = { context.openUrl("https://github.com/Mtzallqmy/Moatazalalqami/blob/main/LICENSE") },
                        leadingContent = { Icon(HugeIcons.File02, null) },
                        supportingContent = { Text("GNU AGPL-3.0") },
                        headlineContent = { Text("License") },
                    )
                }
            }
            item {
                CardGroup(title = { Text("Credits & upstream") }) {
                    item(
                        onClick = { context.openUrl("https://github.com/rikkahub/rikkahub") },
                        leadingContent = { Icon(HugeIcons.Github, null) },
                        supportingContent = { Text("Based on RikkaHub and RikkaHub Agent. Original copyrights remain with their respective contributors.") },
                        headlineContent = { Text("RikkaHub contributors") },
                    )
                    item(
                        onClick = { context.openUrl("https://github.com/ExTV/rikkahub-agent") },
                        leadingContent = { Icon(HugeIcons.Github, null) },
                        supportingContent = { Text("Agent fork used as the technical starting point for this modernization.") },
                        headlineContent = { Text("RikkaHub Agent contributors") },
                    )
                }
            }
        }
    }
}
