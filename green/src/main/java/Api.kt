import kotlinx.serialization.Serializable
import java.io.FileNotFoundException
import java.net.URL
import java.util.concurrent.LinkedBlockingQueue

@Serializable

class Api {

    fun getJsonString(assetId: String): LinkedBlockingQueue<String> {
        val queue = LinkedBlockingQueue<String>()
        Thread {
            try{
                val response =
                    URL("https://btender.bcademy.xyz/api/v1/assets_contract/$assetId/").readText()

                queue.add(response)
                println("Items in Queue are $queue")
            }catch (ffe: FileNotFoundException){

            }

        }.start()
        return queue


    }
}
