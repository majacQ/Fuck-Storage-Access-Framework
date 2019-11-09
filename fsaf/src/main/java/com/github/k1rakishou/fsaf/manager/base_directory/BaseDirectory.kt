package com.github.k1rakishou.fsaf.manager.base_directory

import android.net.Uri
import android.util.Log
import com.github.k1rakishou.fsaf.document_file.CachingDocumentFile
import com.github.k1rakishou.fsaf.extensions.splitIntoSegments
import com.github.k1rakishou.fsaf.file.AbstractFile
import com.github.k1rakishou.fsaf.file.ExternalFile
import com.github.k1rakishou.fsaf.file.RawFile
import java.io.File

/**
 * Base directory is useful when you want to have a file dump directory for your app (like a
 * directory where you will be storing downloaded files or some user selected directory to be used
 * by your app). It may have a SAF directory (like on sd-card) and a regular java-file backed
 * directory (like a directory in the internal app storage or external like the Downloads directory)
 * or even both at the same time. When having both you may set up a mechanism that will
 * automatically switch from the SAF directory to the java backed directory if SAF directory is
 * deleted by the user of the permissions are not granted for that directory anymore.
 *
 * If you want to have a base directory you need to inherit from this class and then add register it
 * in the [DirectoryManager] via the [FileManager]
 * */
abstract class BaseDirectory(
  private val debugMode: Boolean
) {

  fun isBaseDir(dirPath: Uri): Boolean {
    if (debugMode) {
      check(!(getDirUri() == null && getDirFile() == null)) { "Both dirUri and dirFile are nulls!" }
    }

    if (getDirUri() == null) {
      return false
    }

    return getDirUri() == dirPath
  }

  fun isBaseDir(dirPath: File): Boolean {
    if (debugMode) {
      check(!(getDirUri() == null && getDirFile() == null)) { "Both dirUri and dirFile are nulls!" }
    }

    if (getDirFile() == null) {
      return false
    }

    return getDirFile() == dirPath
  }

  fun isBaseDir(dir: AbstractFile): Boolean {
    if (debugMode) {
      check(!(getDirUri() == null && getDirFile() == null)) { "Both dirUri and dirFile are nulls!" }
    }

    if (dir is ExternalFile) {
      if (getDirUri() == null) {
        return false
      }

      return dir.getFileRoot<CachingDocumentFile>().holder.uri() == getDirUri()
    } else if (dir is RawFile) {
      if (getDirFile() == null) {
        return false
      }

      return dir.getFileRoot<File>().holder.absolutePath == getDirFile()?.absolutePath
    }

    throw IllegalStateException("${dir.javaClass.name} is not supported!")
  }

  fun dirPath(): String {
    if (debugMode) {
      check(!(getDirUri() == null && getDirFile() == null)) { "Both dirUri and dirFile are nulls!" }
    }

    if (getDirUri() != null) {
      return getDirUri().toString()
    }

    val dirFile = checkNotNull(getDirFile()) {
      "dirPath() both dirUri and dirFile are not set!"
    }

    return dirFile.absolutePath
  }

  fun areTheSame(file1: AbstractFile, file2: AbstractFile): Boolean {
    val dirUri = getDirUri()
    val dirFile = getDirFile()

    if ((file1 is RawFile || file2 is RawFile) && dirFile == null) {
      // We don't have the java file base directory set up and one of the input files is a RawFile.
      // There is no way for us to know whether they are the same or not without the base directory
      // path
      Log.e(TAG, "areTheSame() one of the input files is a RawFile and dirFile is not set")
      return false
    }

    if ((file1 is ExternalFile || file2 is ExternalFile) && dirUri == null) {
      // We don't have the java file base directory set up and one of the input files is a RawFile.
      // There is no way for us to know whether they are the same or not without the base directory
      // path
      Log.e(TAG, "areTheSame() one of the input files is an ExternalFile and dirUri is not set")
      return false
    }

    val trimmedPath1 = getTrimmedPath(dirFile, dirUri, file1)
      ?: return false

    val trimmerPath2 = getTrimmedPath(dirFile, dirUri, file2)
      ?: return false

    val segments1 = trimmedPath1.splitIntoSegments()
    val segments2 = trimmerPath2.splitIntoSegments()

    if (segments1.size != segments2.size) {
      Log.d(TAG, "areTheSame() segments count does not match")
      return false
    }

    for (segmentIndex in segments1.indices) {
      val segment1 = segments1[segmentIndex]
      val segment2 = segments2[segmentIndex]

      if (segment1 != segment2) {
        Log.d(TAG, "areTheSame() segment1 ($segment1) != segment2 ($segment2)")
        return false
      }
    }

    return true
  }

  private fun getTrimmedPath(
    dirFile: File?,
    dirUri: Uri?,
    file1: AbstractFile
  ): String? {
    return if (
      dirFile != null
      && file1 is RawFile
      && file1.getFullPath().startsWith(dirFile.absolutePath)
    ) {
      file1.getFullPath().removePrefix(dirFile.absolutePath)
    } else if (
      dirUri != null
      && file1 is ExternalFile
      && file1.getFullPath().startsWith(dirUri.toString())
    ) {
      file1.getFullPath().removePrefix(dirUri.toString())
    } else {
      Log.e(TAG, "getTrimmedPath() cannot get trimmed path " +
        "(dirFile == null: ${dirFile == null}, dirUri == null: ${dirUri == null})")
      null
    }
  }

  /**
   * This should return an Uri to the SAF directory.
   *
   * If both [getDirUri] and [getDirFile] return null then methods like
   * [FileManager.newBaseDirectoryFile] will throw an exception!
   * */
  abstract fun getDirUri(): Uri?

  /**
   * This one should return a fallback java file backed directory.
   * */
  abstract fun getDirFile(): File?

  companion object {
    const val TAG = "BaseDirectory"
  }
}
