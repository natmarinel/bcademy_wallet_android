import org.json.JSONException
import org.json.JSONObject
import java.io.FileNotFoundException
import java.net.URL

object BtenderApi {
    const val BASE_URL = "https://btender.bcademy.xyz"
    const val ASSETS_CONTRACT = "$BASE_URL/api/v1/assets_contract"
    const val DOMAIN_PAGE = "$BASE_URL/explorer/domain"

    fun getAssetString(assetId: String): String? {
        return try {
            URL("$ASSETS_CONTRACT/$assetId/").readText()
        } catch (ex: FileNotFoundException) {
            null
        }
    }

    fun getAssetJson(assetId: String): JSONObject? {
        return try {
            JSONObject(getAssetString(assetId))
        } catch(ex: JSONException) {
            null
        } catch(ex: NullPointerException) {
            null
        }
    }
}
