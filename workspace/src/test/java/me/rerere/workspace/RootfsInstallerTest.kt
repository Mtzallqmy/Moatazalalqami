package me.rerere.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

class RootfsInstallerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `extract skips OTHER entry data exactly once`() {
        // OTHER 条目 (如 GNU sparse) 带 size>0 数据区, 双重 skip 会让后续 header 错位
        val archive = tmp.newFile("rootfs.tar.gz")
        GZIPOutputStream(archive.outputStream()).use { out ->
            out.writeTarEntry("a.txt", '0', "hello".toByteArray())
            out.writeTarEntry("sparse.bin", 'S', ByteArray(700) { 1 })
            out.writeTarEntry("b.txt", '0', "world".toByteArray())
            out.write(ByteArray(TAR_BLOCK * 2))
        }

        val target = tmp.newFolder("out")
        createInstaller().extractTar(archive, target) {}

        assertEquals("hello", File(target, "a.txt").readText())
        assertEquals("world", File(target, "b.txt").readText())
        assertFalse(File(target, "sparse.bin").exists())
    }

    @Test
    fun `extract handles directories and zero size entries`() {
        val archive = tmp.newFile("rootfs.tar.gz")
        GZIPOutputStream(archive.outputStream()).use { out ->
            out.writeTarEntry("dir/", '5', ByteArray(0))
            out.writeTarEntry("dir/file.txt", '0', "content".toByteArray())
            out.write(ByteArray(TAR_BLOCK * 2))
        }

        val target = tmp.newFolder("out")
        createInstaller().extractTar(archive, target) {}

        assertEquals(true, File(target, "dir").isDirectory)
        assertEquals("content", File(target, "dir/file.txt").readText())
    }

    @Test
    fun `invalid replacement never deletes installed rootfs`() {
        val base = tmp.newFolder("invalid-rollback")
        val manager = WorkspaceManager(base)
        val root = "agent-a"
        val current = manager.linuxDir(root).apply {
            File(this, "bin").mkdirs()
            File(this, "bin/sh").writeText("old-shell")
            File(this, "etc").mkdirs()
        }
        val archive = tmp.newFile("invalid-rootfs.tar.gz")
        GZIPOutputStream(archive.outputStream()).use { out ->
            out.writeTarEntry("etc/", '5', ByteArray(0))
            out.write(ByteArray(TAR_BLOCK * 2))
        }

        try {
            RootfsInstaller(manager).installArchive(root, archive)
            fail("invalid rootfs should be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: validation happens before activation.
        }

        assertEquals("old-shell", File(current, "bin/sh").readText())
        assertFalse(File(manager.workspaceDir(root), RootfsInstaller.BACKUP_DIR).exists())
        assertFalse(File(manager.workspaceDir(root), RootfsInstaller.STAGING_DIR).exists())
    }

    @Test
    fun `patch failure never activates staged rootfs`() {
        val base = tmp.newFolder("patch-rollback")
        val manager = WorkspaceManager(base)
        val root = "agent-b"
        val current = manager.linuxDir(root).apply {
            File(this, "bin").mkdirs()
            File(this, "bin/sh").writeText("known-good")
            File(this, "etc").mkdirs()
        }
        val archive = validRootfsArchive("replacement")
        val failingPatcher = object : RootfsPatcher() {
            override fun patch(linuxDir: File, options: RootfsPatchOptions) {
                error("injected patch failure")
            }
        }

        try {
            RootfsInstaller(manager, failingPatcher).installArchive(root, archive)
            fail("patch failure should abort installation")
        } catch (_: IllegalStateException) {
            // Expected.
        }

        assertEquals("known-good", File(current, "bin/sh").readText())
    }

    @Test
    fun `recovery restores backup when activation was interrupted`() {
        val base = tmp.newFolder("interrupted-recovery")
        val manager = WorkspaceManager(base)
        val root = "agent-c"
        manager.ensureWorkspace(root)
        manager.linuxDir(root).deleteRecursively()
        val backup = File(manager.workspaceDir(root), RootfsInstaller.BACKUP_DIR).apply {
            File(this, "bin").mkdirs()
            File(this, "bin/sh").writeText("recovered")
            File(this, "etc").mkdirs()
        }
        File(manager.workspaceDir(root), RootfsInstaller.STAGING_DIR).mkdirs()
        File(manager.workspaceDir(root), RootfsInstaller.TRANSACTION_MARKER).writeText("activating\n")

        RootfsInstaller(manager).recoverInterruptedInstall(root)

        assertEquals("recovered", File(manager.linuxDir(root), "bin/sh").readText())
        assertFalse(backup.exists())
        assertFalse(File(manager.workspaceDir(root), RootfsInstaller.STAGING_DIR).exists())
        assertFalse(File(manager.workspaceDir(root), RootfsInstaller.TRANSACTION_MARKER).exists())
    }

    @Test
    fun `valid replacement activates only after validation`() {
        val base = tmp.newFolder("valid-activation")
        val manager = WorkspaceManager(base)
        val root = "agent-d"
        manager.ensureWorkspace(root)

        RootfsInstaller(manager).installArchive(root, validRootfsArchive("new-shell"))

        assertEquals("new-shell", File(manager.linuxDir(root), "bin/sh").readText())
        assertTrue(File(manager.linuxDir(root), "etc").isDirectory)
    }

    private fun createInstaller() = RootfsInstaller(WorkspaceManager(tmp.newFolder()))

    private fun validRootfsArchive(shell: String): File {
        val archive = tmp.newFile("rootfs-${System.nanoTime()}.tar.gz")
        GZIPOutputStream(archive.outputStream()).use { out ->
            out.writeTarEntry("bin/", '5', ByteArray(0))
            out.writeTarEntry("bin/sh", '0', shell.toByteArray())
            out.writeTarEntry("etc/", '5', ByteArray(0))
            out.write(ByteArray(TAR_BLOCK * 2))
        }
        return archive
    }

    private fun OutputStream.writeTarEntry(name: String, type: Char, data: ByteArray) {
        val header = ByteArray(TAR_BLOCK)
        name.toByteArray(Charsets.UTF_8).copyInto(header, 0)
        "0000755".toByteArray().copyInto(header, 100)
        data.size.toLong().toOctalField().copyInto(header, 124)
        header[156] = type.code.toByte()
        write(header)
        write(data)
        val padding = (TAR_BLOCK - data.size % TAR_BLOCK) % TAR_BLOCK
        write(ByteArray(padding))
    }

    private fun Long.toOctalField(): ByteArray =
        toString(8).padStart(11, '0').toByteArray(Charsets.UTF_8)

    companion object {
        private const val TAR_BLOCK = 512
    }
}
