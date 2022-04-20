import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import kotlin.jvm.JvmOverloads
import androidx.core.content.FileProvider
import com.blockstream.green.GreenApplication
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Save a file in the App cache and get Uri for it
 */
class Cache {


    /**
     * Save image to the App cache
     * @param bitmap to save to the cache
     * @param name file name in the cache.
     * If name is null file will be named by default [.TEMP_FILE_NAME]
     * @return file dir when file was saved
     */
    fun saveImgToCache(bitmap: Bitmap, name: String?): File? {
        println("siamo entrare in saveimgtocache")
        var cachePath: File? = null
        var fileName: String? = TEMP_FILE_NAME
        if (!TextUtils.isEmpty(name)) {
            fileName = name
        }
        try {
            cachePath = File(context.cacheDir, CHILD_DIR)
            cachePath.mkdirs()
            val stream = FileOutputStream("$cachePath/$fileName$FILE_EXTENSION")
            bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESS_QUALITY, stream)
            stream.close()

        } catch (e: IOException) {
            Log.e(TAG, "saveImgToCache error: $bitmap", e)
        }
        println("questo dovrebbe essere il cachePath $cachePath")
        return cachePath
    }
    /**
     * Save an image to the App cache dir and return it [Uri]
     * @param bitmap to save to the cache
     * @param name file name in the cache.
     * If name is null file will be named by default [.TEMP_FILE_NAME]
     */
    /**
     * Save an image to the App cache dir and return it [Uri]
     * @param bitmap to save to the cache
     */
    @JvmOverloads
    fun saveToCacheAndGetUri(bitmap: Bitmap, name: String? = null): Uri {
        val file = saveImgToCache(bitmap, name)
        return getImageUri(file, name)
    }

    /**
     * Get a file [Uri]
     * @param name of the file
     * @return file Uri in the App cache or null if file wasn't found
     */
    fun getUriByFileName(name: String): Uri? {
        val context: Context = context
        val fileName: String = if (!TextUtils.isEmpty(name)) {
            name
        } else {
            return null
        }
        val imagePath = File(context.cacheDir, CHILD_DIR)
        val newFile = File(imagePath, fileName + FILE_EXTENSION)
        return FileProvider.getUriForFile(context, context.packageName + ".provider", newFile)
    }

    // Get an image Uri by name without extension from a file dir
    private fun getImageUri(fileDir: File?, name: String?): Uri {
        val context: Context = context
        var fileName: String? = TEMP_FILE_NAME
        if (!TextUtils.isEmpty(name)) {
            fileName = name
        }
        val newFile = File(fileDir, fileName + FILE_EXTENSION)
        return FileProvider.getUriForFile(context, context.packageName + ".provider", newFile)
    }

    /**
     * Get Uri type by [Uri]
     */
    fun getContentType(uri: Uri?): String? {
        return context.contentResolver.getType(uri!!)
    }

    companion object {
        val TAG = Cache::class.java.simpleName
        private const val CHILD_DIR = "images"
        private const val TEMP_FILE_NAME = "img"
        private const val FILE_EXTENSION = ".png"
        private const val COMPRESS_QUALITY = 100
        private val context: Context = GreenApplication.context
    }

}