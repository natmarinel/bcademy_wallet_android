package com.blockstream.green.ui.overview

import Utils.downloadFile
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import com.blockstream.green.R
import com.blockstream.green.databinding.AssetDetailsBottomSheetBinding
import com.blockstream.green.ui.WalletBottomSheetDialogFragment
import com.blockstream.green.ui.items.OverlineTextListItem
import com.blockstream.green.ui.looks.AssetLook
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.ui.utils.StringHolder
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.net.URL

@AndroidEntryPoint
class AssetBottomSheetFragment : WalletBottomSheetDialogFragment<AssetDetailsBottomSheetBinding, AbstractWalletViewModel>(
        layout = R.layout.asset_details_bottom_sheet
    ) {

    companion object {
        private const val ASSET_ID = "ASSET_ID"

        fun newInstance(message: String): AssetBottomSheetFragment =
            AssetBottomSheetFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putString(ASSET_ID, message)
                }
            }
    }

    private val disposables = CompositeDisposable()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = viewModel.session
        var assetId = arguments?.getString(ASSET_ID) ?: session.policyAsset
        val asset = session.getAsset(assetId)

        val look = AssetLook(assetId, 0, session)

        val isPolicyAsset = session.policyAsset == assetId

        val list = mutableListOf<GenericItem>()

        list += OverlineTextListItem(
            StringHolder(R.string.id_name),
            StringHolder(
                if (isPolicyAsset) look.name else asset?.name
                    ?: getString(R.string.id_no_registered_name_for_this)
            )
        )

        val balanceListItem =
            OverlineTextListItem(StringHolder(R.string.id_total_balance), StringHolder("-"))
        val blockHeightListItem =
            OverlineTextListItem(StringHolder(R.string.id_block_height), StringHolder("-"))

        if (isPolicyAsset) {
            list += blockHeightListItem
            list += balanceListItem
        } else {
            list += OverlineTextListItem(StringHolder(R.string.id_asset_id), StringHolder(assetId))
            list += balanceListItem
            list += OverlineTextListItem(
                StringHolder(R.string.id_precision),
                StringHolder((asset?.precision ?: 0).toString())
            )
            list += OverlineTextListItem(
                StringHolder(R.string.id_ticker),
                StringHolder(asset?.ticker ?: getString(R.string.id_no_registered_ticker_for_this))
            )
            list += OverlineTextListItem(
                StringHolder(R.string.id_issuer),
                StringHolder(asset?.entity?.domain ?: getString(R.string.id_unknown))
            )
        }

        // =========================================================================================
        // ================================ BCADEMY  ===============================================
        // =========================================================================================

        // test these assets containing different media nft
        //assetId = "1668fb5b9cd93544b8e0462a72b642b719f5cdbdcb787bf924e3417dab3a8303" // nft video
        //assetId = "21f9a48d64e76e6173fb8396041c978f39d3dccd78e1279cdbbe195df4f62f66" // nft gif
        //assetId = "582c4561fc34306661496abda13b30a254fcfeda0b3a16b3194f681ee8b17e53" // btender asset but with nftContract: null
        //assetId = "d8f6cbfa18294451ea73b3e1d91c3c77c8f7211b888e92373acd4baca566b302" // with icon and attachments

        CoroutineScope(Dispatchers.IO).launch {
            val json = BtenderApi.getAssetJson(assetId)
            val nftContract: JSONObject? = try {
                json?.getJSONObject("nftContract")
            } catch(ex: JSONException) {
                println("Btender asset without NFT { nftContract: null }")
                null
            }

            if(nftContract != null) {
                val nftDirectory = File(context?.cacheDir, "NFT") // save in cache app directory
                if (!nftDirectory.exists()) {
                    nftDirectory.mkdirs()
                    println("NFT directory created in ${nftDirectory.absolutePath}")
                }

                val assetDirectory = File(nftDirectory, assetId)
                if (!assetDirectory.exists()) {
                    assetDirectory.mkdirs()
                    println("Asset directory created in ${assetDirectory.absolutePath}")
                }

                val media = nftContract.getJSONObject("media")
                val nftUrl = media.getString("fileUrl")
                val contentType = media.getString("contentType")
                val mediaType = contentType.substring(0, contentType.indexOf('/', 0))
                val mediaExtension =
                    contentType.substring(contentType.indexOf('/', 0) + 1, contentType.length)
                        .lowercase()

                val filename = "$assetId.$mediaExtension"
                var nftFile = File(assetDirectory, filename)

                if (!nftFile.exists()) {
                    nftFile = downloadFile("${BtenderApi.BASE_URL}$nftUrl", nftFile.absolutePath)
                    asset?.nft = nftFile.absolutePath
                    println("Download NFT $filename")
                } else {
                    println("Retrieve NFT $filename from cache")
                }

                val meta = nftContract.getJSONArray("meta").getJSONObject(0)
                asset?.description = meta.getString("description")

                val attachments = try {
                    nftContract.getJSONArray("attachments")
                } catch(ex: FileNotFoundException) {
                    println("Asset without attachments")
                    null
                }

                attachments?.let {
                    asset?.attachments = hashMapOf()
                    for (i in 0 until it.length() - 1) {
                        val attachmentName = it.getJSONObject(i).getString("name")
                        val attachmentUrl = it.getJSONObject(i).getString("fileUrl")
                        asset?.attachments?.put(attachmentName, attachmentUrl)
                    }
                }

                withContext(Dispatchers.Main) {
                    when (mediaType) {
                        "image" -> {
                            when (mediaExtension) {
                                "gif" -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        binding.nftImage.apply {
                                            visibility = View.VISIBLE
                                            val decodedBitmap = ImageDecoder.createSource(nftFile)
                                            val decodedDrawable = ImageDecoder.decodeDrawable(decodedBitmap)
                                            setImageDrawable(decodedDrawable)
                                            val animatedDrawable = decodedDrawable as AnimatedImageDrawable
                                            animatedDrawable.start()
                                        }
                                    } else {
                                        binding.nftGif.apply {
                                            visibility = View.VISIBLE
                                            setBackgroundColor(Color.TRANSPARENT)
                                            // Technique: display a gif with WebView in order to avoid the use of external libraries or gif dependencies
                                            val html =
                                                "<html>" +
                                                    "<body style=\"display: flex; background-color: transparent; justify-content: center;\">" +
                                                        "<img style=\"height: 100%\" src=\"${BtenderApi.BASE_URL}$nftUrl\">" +
                                                    "</body>" +
                                                "</html>"
                                            loadData(html, "text/html", "UTF-8")
                                        }
                                    }
                                }
                                else -> {
                                    binding.nftImage.apply {
                                        visibility = View.VISIBLE
                                        val image = BitmapFactory.decodeFile(asset?.nft)
                                        setImageBitmap(image)
                                    }
                                }
                            }
                        }
                        "video" -> {
                            binding.nftVideo.run {
                                visibility = View.VISIBLE
                                setVideoPath(asset?.nft)
                                setOnPreparedListener {
                                    it.apply {
                                        setVolume(0F, 0F)
                                        isLooping = true
                                        setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                    }
                                }
                                start()
                            }

                        }
                        else -> {
                            Toast.makeText(context,"Unsupported NFT mediaType $mediaType to display", Toast.LENGTH_LONG).show()
                        }
                    }

                    binding.nftname.text = asset?.name

                    listOf(binding.nftImage, binding.nftVideo, binding.nftGif).forEach {
                        run {
                            it.setOnClickListener {
                                startActivity(
                                    Intent(context, NftViewer::class.java).apply {
                                        putExtra("mediaExtension", mediaExtension)
                                        putExtra("mediaType", mediaType)
                                        putExtra("nftUrl", nftUrl)
                                        putExtra("asset", asset)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            else {
                Handler(Looper.getMainLooper()).post {
                    val itemAdapter = FastItemAdapter<GenericItem>()
                    itemAdapter.add(list)
                    binding.recycler.apply {
                        adapter = FastAdapter.with(itemAdapter)
                    }
                }
            }
        }
        // =========================================================================================
        // =========================================================================================
        // =========================================================================================

        binding.buttonClose.setOnClickListener{
            dismiss()
        }

        session.getBalancesObservable()
            .subscribeBy(
                onNext =
                {
                    look.amount = it[assetId] ?: 0
                    balanceListItem.text = StringHolder(look.balance(withUnit = false))
                    binding.recycler.adapter?.notifyItemChanged(list.indexOf(balanceListItem))
                },
                onError =
                {}
            ).addTo(disposables)

        session.getBlockObservable().subscribeBy(
            onNext =
            {
                blockHeightListItem.text = StringHolder(it.height.toString())
            },
            onError =
            {}
        ).addTo(disposables)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}