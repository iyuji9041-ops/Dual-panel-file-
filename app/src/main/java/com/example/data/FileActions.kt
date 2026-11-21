package com.example.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

/**
 * Stateless helpers for sharing / opening files with other apps. Fully offline:
 * only a local FileProvider content:// URI is created, nothing leaves the device by itself.
 */
object FileActions {

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun mimeOf(file: File): String {
        val ext = file.extension.lowercase(Locale.ROOT)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    /** Share one or many FILES (folders are skipped because Android cannot share a directory). */
    fun share(context: Context, paths: Collection<String>) {
        val files = paths.map { File(it) }.filter { it.isFile && it.canRead() }
        if (files.isEmpty()) {
            Toast.makeText(context, "Nothing to share (folders can't be shared)", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uris = ArrayList(files.map { uriFor(context, it) })
            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = mimeOf(files.first())
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    // Mixed types -> */* so every target app can receive the batch.
                    val mimes = files.map { mimeOf(it) }.distinct()
                    type = if (mimes.size == 1) mimes.first() else "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                }
            }
            // ClipData is what actually carries the temporary read grant to the receiver.
            val clip = ClipData.newRawUri(files.first().name, uris.first())
            uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            intent.clipData = clip
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val chooser = Intent.createChooser(intent, "Share")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** "Open with…" -> system app chooser for this file type. */
    fun openWith(context: Context, path: String) {
        val file = File(path)
        if (!file.isFile) return
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uriFor(context, file), mimeOf(file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Open with")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No app can open this file", Toast.LENGTH_SHORT).show()
        }
    }
}

/** Favorites that survive app restarts (plain SharedPreferences, no extra dependency). */
object FavoritesStore {
    private const val PREFS = "dualfiles_favorites"
    private const val KEY = "paths"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(context: Context): Set<String> =
        prefs(context).getStringSet(KEY, emptySet()) ?: emptySet()

    fun isFavorite(context: Context, path: String): Boolean = all(context).contains(path)

    /** @return the NEW state (true = now a favorite). */
    fun toggle(context: Context, path: String): Boolean {
        // getStringSet's result must never be mutated in place, so copy first.
        val set = all(context).toMutableSet()
        val nowFav = if (set.contains(path)) { set.remove(path); false } else { set.add(path); true }
        prefs(context).edit().putStringSet(KEY, set).apply()
        return nowFav
    }
}
