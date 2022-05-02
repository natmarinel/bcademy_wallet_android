import java.io.File
import java.io.FileNotFoundException
import java.net.URL

object Utils {

     fun downloadFile(url: String, path: String): File {
        val file = File(path)
        file.createNewFile()
        val inputStream = try {
            URL(url)
        } catch (ex: FileNotFoundException) {
            println("Download failed! File $url doesn't exists")
            null
        }
        inputStream?.let {
            it.openStream().use {  input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return file
    }
}