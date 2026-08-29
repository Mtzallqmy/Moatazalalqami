from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
repo = ROOT


def rw(path, fn):
    p = repo / path
    s = p.read_text(encoding='utf-8')
    n = fn(s)
    if n != s:
        p.write_text(n, encoding='utf-8')
        print('updated', path)


def patch_app_gradle(s):
    s = s.replace('applicationId = "excp.rikkahub"', 'applicationId = "com.moatazalaqami.agent"')
    s = re.sub(r'versionCode\s*=\s*\d+', 'versionCode = 30000', s, count=1)
    s = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "3.0.0"', s, count=1)
    s = s.replace('abiFilters += listOf("arm64-v8a", "x86_64")', 'abiFilters += listOf("arm64-v8a")')
    s = s.replace('include("arm64-v8a", "x86_64")', 'include("arm64-v8a")')
    s = s.replace('isUniversalApk = true', 'isUniversalApk = false')
    marker = 'tasks.register("buildAll") {'
    if 'prepareEmbeddedLinuxRootfs' not in s:
        block = r'''
// Moataz Alaqami 3.0: embed an aarch64 Alpine Linux minirootfs in every APK.
// The checksum is fetched from Alpine's official release mirror and verified.
val alpineVersion = "3.24.1"
val embeddedLinuxDir = layout.buildDirectory.dir("generated/moatazLinux")
val prepareEmbeddedLinuxRootfs by tasks.registering {
    val outDir = embeddedLinuxDir
    outputs.dir(outDir)
    doLast {
        val dir = outDir.get().asFile.apply { mkdirs() }
        val archiveName = "alpine-minirootfs-$alpineVersion-aarch64.tar.gz"
        val base = "https://dl-cdn.alpinelinux.org/alpine/v3.24/releases/aarch64"
        val archive = File(dir, "linux-rootfs.tar.gz")
        val checksum = File(dir, "$archiveName.sha256")
        if (!archive.exists()) {
            java.net.URI("$base/$archiveName").toURL().openStream().use { input ->
                archive.outputStream().use { input.copyTo(it) }
            }
        }
        java.net.URI("$base/$archiveName.sha256").toURL().openStream().use { input ->
            checksum.outputStream().use { input.copyTo(it) }
        }
        val expected = checksum.readText().trim().substringBefore(' ')
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val actual = archive.inputStream().use { stream ->
            val buf = ByteArray(1024 * 128)
            while (true) {
                val count = stream.read(buf)
                if (count < 0) break
                digest.update(buf, 0, count)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
        check(actual.equals(expected, ignoreCase = true)) {
            "Embedded Linux rootfs checksum mismatch: expected=$expected actual=$actual"
        }
    }
}

android.sourceSets.getByName("main").assets.srcDir(embeddedLinuxDir)
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }
    .configureEach { dependsOn(prepareEmbeddedLinuxRootfs) }

'''
        s = s.replace(marker, block + marker)
    return s
rw('app/build.gradle.kts', patch_app_gradle)

for p in (repo/'app/src/main/res').glob('values*/strings.xml'):
    text = p.read_text(encoding='utf-8')
    text = re.sub(r'(<string\s+name="app_name"[^>]*>).*?(</string>)', r'\1Moataz Alaqami\2', text)
    text = text.replace('RikkaHub Agent', 'Moataz Alaqami')
    text = text.replace('RikkaHub-agent', 'Moataz Alaqami')
    text = text.replace('RikkaHub', 'Moataz Alaqami')
    text = text.replace('https://rikka-ai.com/', 'https://github.com/Mtzallqmy/Moatazalalqami')
    text = text.replace('https://rikka-ai.com', 'https://github.com/Mtzallqmy/Moatazalalqami')
    p.write_text(text, encoding='utf-8')

for base in [repo/'app/src/main/assets/default-skills']:
    for p in base.rglob('*'):
        if p.is_file() and p.suffix.lower() in {'.md', '.txt', '.html', '.js', '.json'}:
            try:
                text = p.read_text(encoding='utf-8')
            except UnicodeDecodeError:
                continue
            text = text.replace('RikkaHub Agent', 'Moataz Alaqami Agent')
            text = text.replace('RikkaHub agent', 'Moataz Alaqami agent')
            text = text.replace('RikkaHub', 'Moataz Alaqami')
            text = text.replace('/sdcard/Documents/Moataz Alaqami/', '/sdcard/Documents/MoatazAlaqami/')
            p.write_text(text, encoding='utf-8')

for p in (repo/'app/src/main/java').rglob('*.kt'):
    text = p.read_text(encoding='utf-8')
    def repl(m):
        body = m.group(1)
        body = body.replace('RikkaHub-agent', 'Moataz Alaqami')
        body = body.replace('RikkaHub Agent', 'Moataz Alaqami')
        body = body.replace('RikkaHub', 'Moataz Alaqami')
        body = body.replace('Pictures/Moataz Alaqami', 'Pictures/MoatazAlaqami')
        body = body.replace('Documents/Moataz Alaqami', 'Documents/MoatazAlaqami')
        body = body.replace('Download/Moataz Alaqami', 'Download/MoatazAlaqami')
        return '"' + body + '"'
    text = re.sub(r'"([^"\n]*)"', repl, text)
    p.write_text(text, encoding='utf-8')

rw('workspace/src/main/java/me/rerere/workspace/RootfsPatcher.kt', lambda s: s.replace('Generated by RikkaHub workspace.', 'Generated by Moataz Alaqami workspace.'))

about = repo/'app/src/main/java/me/rerere/rikkahub/ui/pages/setting/SettingAboutPage.kt'
about.write_text(r'''package me.rerere.rikkahub.ui.pages.setting

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
''', encoding='utf-8')
print('updated about page')

sp = repo/'app/src/main/java/me/rerere/rikkahub/ui/pages/setting/SettingPage.kt'
s = sp.read_text(encoding='utf-8')
s = re.sub(r'''\n\s*item\(\n\s*onClick = \{ navController\.navigate\(Screen\.SettingDonate\) \},\n\s*leadingContent = \{ Icon\(HugeIcons\.InLove, null\) \},\n\s*supportingContent = \{ Text\(stringResource\(R\.string\.setting_page_donate_desc\)\) \},\n\s*headlineContent = \{ Text\(stringResource\(R\.string\.setting_page_donate\)\) \},\n\s*\)''', '', s)
s = re.sub(r'''\n\s*item\(\n\s*onClick = \{\n\s*val docUrl = if \(java\.util\.Locale\.getDefault\(\)\.language == "zh"\) \{.*?\n\s*headlineContent = \{ Text\(stringResource\(R\.string\.setting_page_documentation\)\) \},\n\s*\)''', '', s, flags=re.S)
s = re.sub(r'\n\s*trailingContent = \{\n\s*Row\(.*?\n\s*\},\n\s*headlineContent =', '\n                        headlineContent =', s, count=1, flags=re.S)
sp.write_text(s, encoding='utf-8')
print('cleaned settings upstream promotion')

installer = repo/'workspace/src/main/java/me/rerere/workspace/RootfsInstaller.kt'
s = installer.read_text(encoding='utf-8')
if 'fun installArchive(' not in s:
    needle = '    private fun download(\n'
    method = '''    fun installArchive(
        root: String,
        archive: File,
        onProgress: (RootfsInstallProgress) -> Unit = {},
    ) {
        require(archive.isFile && archive.length() > 0L) { "Rootfs archive is missing" }
        manager.ensureWorkspace(root)
        val tempDir = manager.tempDir(root)
        val stagingDir = File(tempDir, "rootfs-staging")
        val linuxDir = manager.linuxDir(root)
        try {
            stagingDir.deleteRecursively()
            stagingDir.mkdirs()
            extractTar(archive, stagingDir, ArchiveFormat.fromFile(archive), onProgress)
            linuxDir.deleteRecursively()
            require(stagingDir.renameTo(linuxDir)) { "Failed to move rootfs into workspace" }
            patcher.patch(linuxDir)
            onProgress(RootfsInstallProgress(stage = RootfsInstallStage.INSTALLED))
        } finally {
            stagingDir.deleteRecursively()
        }
    }

'''
    s = s.replace(needle, method + needle)
    installer.write_text(s, encoding='utf-8')
    print('added local rootfs archive installer')

repo_file = repo/'app/src/main/java/me/rerere/rikkahub/data/repository/WorkspaceRepository.kt'
s = repo_file.read_text(encoding='utf-8')
if 'private val context: android.content.Context' not in s:
    s = s.replace(
        '    private val settingsStore: SettingsStore,\n)',
        '    private val settingsStore: SettingsStore,\n    private val context: android.content.Context,\n)'
    )
if 'suspend fun installEmbeddedRootfs(' not in s:
    needle = '    suspend fun installRootfs(\n'
    method = '''    suspend fun installEmbeddedRootfs(
        id: String,
        onProgress: (RootfsInstallProgress) -> Unit = {},
    ): Boolean {
        val workspace = dao.getById(id) ?: return false
        updateShellState(workspace, WorkspaceShellStatus.INSTALLING.name)
        val archive = File(manager.tempDir(workspace.root), "embedded-linux-rootfs.tar.gz")
        return try {
            withContext(Dispatchers.IO) {
                context.assets.open("linux-rootfs.tar.gz").use { input ->
                    archive.outputStream().use { output -> input.copyTo(output) }
                }
            }
            runInterruptible(Dispatchers.IO) {
                rootfsInstaller.installArchive(workspace.root, archive, onProgress)
            }
            updateShellState(workspace, WorkspaceShellStatus.READY.name)
            true
        } catch (e: CancellationException) {
            withContext(NonCancellable) { restoreShellState(workspace) }
            throw e
        } catch (e: InterruptedException) {
            withContext(NonCancellable) { restoreShellState(workspace) }
            throw CancellationException("Embedded rootfs install cancelled").also { it.initCause(e) }
        } catch (e: Throwable) {
            Log.e(TAG, "installEmbeddedRootfs failed: workspace=${workspace.id}", e)
            updateShellState(workspace, WorkspaceShellStatus.BROKEN.name)
            throw e
        } finally {
            archive.delete()
        }
    }

'''
    s = s.replace(needle, method + needle)
old = '        dao.upsert(workspace)\n        return workspace\n'
new = '''        dao.upsert(workspace)
        runCatching { installEmbeddedRootfs(workspace.id) }
            .onFailure { Log.e(TAG, "Embedded Linux auto-provision failed for ${workspace.id}", it) }
        return dao.getById(workspace.id) ?: workspace
'''
if old in s:
    s = s.replace(old, new, 1)
repo_file.write_text(s, encoding='utf-8')
print('wired embedded rootfs repository flow')

di = repo/'app/src/main/java/me/rerere/rikkahub/di/RepositoryModule.kt'
s = di.read_text(encoding='utf-8')
s = s.replace('WorkspaceRepository(get(), get(), get(), get())', 'WorkspaceRepository(get(), get(), get(), get(), get())')
di.write_text(s, encoding='utf-8')

vm = repo/'app/src/main/java/me/rerere/rikkahub/ui/pages/extensions/workspace/WorkspaceDetailVM.kt'
s = vm.read_text(encoding='utf-8')
if 'fun installEmbeddedRootfs()' not in s:
    needle = '    fun installRootfs(url: String) {\n'
    method = '''    fun installEmbeddedRootfs() {
        viewModelScope.launch {
            _installError.value = null
            val workspace = state.value.workspace ?: return@launch
            _installProgress.value = RootfsInstallProgress(stage = RootfsInstallStage.EXTRACTING)
            try {
                repository.installEmbeddedRootfs(workspace.id) { progress ->
                    _installProgress.value = progress
                }
                loadWorkspace()
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (error: Throwable) {
                _installError.value = error.message ?: "Embedded Linux setup failed"
            } finally {
                _installProgress.value = null
            }
        }
    }

'''
    s = s.replace(needle, method + needle)
    vm.write_text(s, encoding='utf-8')

print('modernization patch complete')
