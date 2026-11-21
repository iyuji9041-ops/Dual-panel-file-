package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FileRepository
import com.example.model.DropActionMode
import com.example.model.SortOption
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name from context and verify dual file repository operations`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DualFiles", appName)

        val repo = FileRepository(context)
        val driveAFiles = repo.listFiles(
            dirPath = repo.driveADir.absolutePath,
            sortOption = SortOption.NAME_ASC,
            searchQuery = "",
            showHidden = false
        )
        assertTrue(driveAFiles.isNotEmpty())

        // Create a file in Drive A and move it to Drive B (simulating drag & drop move)
        val created = repo.createFile(
            parentPath = repo.driveADir.absolutePath,
            fileName = "drag_test.txt",
            content = "Drag and drop test payload"
        ).getOrThrow()
        assertTrue(File(created.path).exists())

        val moveRecord = repo.transferItems(
            sourcePaths = listOf(created.path),
            targetDirPath = repo.driveBDir.absolutePath,
            actionMode = DropActionMode.MOVE
        ).getOrThrow()

        assertFalse(File(created.path).exists())
        val movedDestination = File(repo.driveBDir, "drag_test.txt")
        assertTrue(movedDestination.exists())

        // Undo the move and verify it returns to Drive A
        repo.undoTransfer(moveRecord).getOrThrow()
        assertTrue(File(created.path).exists())
        assertFalse(movedDestination.exists())
    }
}
