package com.blockstream.gdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.MutableLiveData
import com.blockstream.crypto.R
import com.blockstream.gdk.data.Asset
import com.blockstream.gdk.data.Assets
import com.blockstream.gdk.params.AssetsParams

interface AssetQATester {
    fun isAssetGdkCacheDisabled(): Boolean
    fun isAssetFetchDisabled(): Boolean
    fun isAssetIconsFetchDisabled(): Boolean
}

interface AssetsProvider {
    fun refreshAssets(params: AssetsParams): Assets
}

enum class CacheStatus {
    Empty, Cached, Latest
}

data class AssetStatus(
    var metadataStatus: CacheStatus = CacheStatus.Empty,
    var iconStatus: CacheStatus = CacheStatus.Empty,
    var onProgress: Boolean = false,
)

/*
 * AssetManager is responsible of updating Assets and handle different caches
 * App Cache: cached data from apk
 * GDK Cache: cached data from a previous successful fetch
 */
class AssetManager(
    private val context: Context,
    val QATester: AssetQATester,
    private val applicationId: String
) {
    private var metadata: Map<String, Asset> = mapOf()
    private var icons: Map<String, Bitmap?> = mapOf()

    // Internal representation of the Status
    private val status = AssetStatus()

    val statusLiveData =  MutableLiveData(status)

    fun setGdkCache(assets: Assets) {
        this.metadata = assets.assets
        this.icons = assets.icons ?: mapOf()

        // Status: Cached

        status.metadataStatus = CacheStatus.Cached
        status.iconStatus = CacheStatus.Cached
    }

    private fun updateMetadata(assets: Assets) {
        this.metadata = assets.assets
        // Status: Metadata Latest
        status.metadataStatus = CacheStatus.Latest
    }

    // Currently unused as the assets are integrated in the build
    fun updateIcons(assets: Assets) {
        this.icons = assets.icons ?: mapOf()
        // Status: Icons Latest
        status.iconStatus = CacheStatus.Latest
    }

    fun getAsset(assetId: String): Asset? {
        // Asset from GDK (cache or up2date)
        return metadata[assetId]
    }

    fun hasAssetIcon(assetId: String): Boolean = getAssetIcon(assetId) != null

    fun getAssetDrawableOrNull(assetId: String): Drawable? {
        when{
            assetId=="ca733301ae5b406d66f3a6a55f9d61917f24acc54641becaab1be478bba8e826" ->{
                return context.getDrawable((R.drawable.nfticon))
            }
            else ->{
                getAssetIcon(assetId)?.let {
                    return BitmapDrawable(context.resources, it)
                }
            }
        }

        // CONTROLLO SE L'ASSET ID CORRISPONDE AL ID DEL NFT ALLORA RETURN CONTEXT. GETDRWABLE(R.DRAWBLE.NOMEIMMAGINE)  SALVARE FILE DENTRO CRYPTO SRC MAIN RES DRAWABLE ANCHE IN PNG NESSUN PROBLEMA
        /*     if (assetId == "77961c27eba8fb63b1f966000b49e0e1561349cb0cf459d111acbf895b5251db") {
            return context.getDrawable(R.drawable.asset_77961c27eba8fb63b1f966000b49e0e1561349cb0cf459d111acbf895b5251db)
        } else {
            getAssetIcon(assetId)?.let {
                return BitmapDrawable(context.resources, it)
            }
        }
        when {
                Network.isMainnet(session.network.id) -> {
                    R.drawable.ic_bitcoin_network_60
                }
                Network.isLiquid(session.network.id) -> {
                    R.drawable.ic_liquid_network_60
                }
                Network.isTestnet(session.network.id) -> {
                    R.drawable.ic_bitcoin_testnet_network_60
                }
                Network.isTestnetLiquid(session.network.id) -> {
                    R.drawable.ic_liquid_testnet_network_60
                }
                else -> {
                    R.drawable.ic_unknown_asset_60
                }
            }
        */
        getAssetIcon(assetId)?.let {
            return BitmapDrawable(context.resources, it)
        }

        return null
    }

    fun getAssetDrawableOrDefault(assetId: String): Drawable {
        return getAssetDrawableOrNull(assetId) ?: context.getDrawable(R.drawable.ic_unknown_asset_60)!!
    }

    fun updateAssetsIfNeeded(provider: AssetsProvider) {
        updateMetadata(provider, false)
    }

    fun updateAssetsAsync(provider: AssetsProvider) {
        // Use ApplicationScope
        //applicationScope.launch {
        //    updateMetadata(provider, true)
        //}
    }

    private fun updateMetadata(provider: AssetsProvider, forceUpdate: Boolean) {
        // Update from Network if needed
        if (forceUpdate || status.metadataStatus != CacheStatus.Latest) {

            try {
                statusLiveData.postValue(status.apply { onProgress = true })

                if (!QATester.isAssetGdkCacheDisabled()) {

                    // First try to update from GDK Cache if needed
                    if (status.metadataStatus == CacheStatus.Empty) {
                        try {
                            // Update from GDK Cache
                            setGdkCache(
                                provider.refreshAssets(
                                    AssetsParams(
                                        assets = true,
                                        icons = true,
                                        refresh = false
                                    )
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // Allow forceUpdate to override QATester settings
                if (QATester.isAssetFetchDisabled() && !forceUpdate) {
                    return
                }

                try {
                    // Try to update the registry - only metadata
                    // Fetch assets without icons as we have better chances to complete the network call
                    updateMetadata(
                        provider.refreshAssets(
                            AssetsParams(
                                assets = true,
                                icons = false,
                                refresh = true
                            )
                        )
                    )

                    // Allow forceUpdate to override QATester settings
                    if (QATester.isAssetIconsFetchDisabled() && !forceUpdate) {
                        return
                    }

                    try {
                        // Try to update the registry - only icons
                        updateIcons(
                            provider.refreshAssets(
                                AssetsParams(
                                    assets = false,
                                    icons = true,
                                    refresh = true
                                )
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } finally {
                statusLiveData.postValue(status.apply { onProgress = false })
            }
        }
    }

    fun getAssetIcon(assetId: String): Bitmap? {
        return icons[assetId]
    }
}