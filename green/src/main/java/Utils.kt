import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL

object Utils {

    fun downloadFile(url: String, path: String): File {
        val file = File(path)
        try {
            file.createNewFile()
            URL(url).openStream().use {  input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (ex: FileNotFoundException) {
            throw FileNotFoundException("Download failed! File $url doesn't exists")
        } catch (ex: IOException) {
            throw IOException("Unable to create ${file.name} file")
        }
        return file
    }

}