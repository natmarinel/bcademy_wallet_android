package com.blockstream.green.ui.overview

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockstream.gdk.data.Asset
import com.blockstream.green.GreenApplication.Companion.context
import com.blockstream.green.R
import com.blockstream.green.ui.items.AttachmentListItem
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.ui.utils.StringHolder
import java.io.File


class NftViewer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.nft)

        val viewNftGif = findViewById<WebView>(R.id.nftGif)
        val viewNftImage = findViewById<ImageView>(R.id.nftImage)
        val viewNftVideo = findViewById<VideoView>(R.id.nftVideo)
        val viewNftName = findViewById<TextView>(R.id.nftname)
        val viewNftDomain = findViewById<TextView>(R.id.nftdomain)
        val viewNftId = findViewById<TextView>(R.id.nftid)
        val viewNftDescription = findViewById<TextView>(R.id.nftdescription)

        intent?.let {
            val bundle = intent.extras

            val asset = bundle?.get("asset") as Asset
            val mediaType = bundle?.get("mediaType") as String
            val mediaExtension = bundle?.get("mediaExtension") as String
            val nftUrl = bundle?.get("nftUrl") as String
            val nftFile = File(asset?.nft)

            when (mediaType) {
                "image" -> {
                    when (mediaExtension) {
                        "gif" -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                viewNftImage.apply {
                                    visibility = View.VISIBLE
                                    val decodedBitmap = ImageDecoder.createSource(nftFile)
                                    val decodedDrawable = ImageDecoder.decodeDrawable(decodedBitmap)
                                    setImageDrawable(decodedDrawable)
                                    val animatedDrawable = decodedDrawable as AnimatedImageDrawable
                                    animatedDrawable.apply {
                                        setOnClickListener {
                                            if(this.isRunning) this.stop()
                                            else this.start()
                                        }
                                        start()
                                    }
                                }
                            } else {
                                viewNftGif.apply {
                                    visibility = View.VISIBLE
                                    setBackgroundColor(Color.TRANSPARENT)
                                    // Technique: display a gif with WebView in order to avoid the use of external libraries or gif dependencies
                                    val html =
                                        "<html>" +
                                                "<body style=\"display: flex; background-color: transparent; justify-content: center;\">" +
                                                "<img style=\"height: 100%\" src=\"${BtenderApi.BASE_URL}${nftUrl}\">" +
                                                "</body>" +
                                                "</html>"
                                    loadData(html, "text/html", "UTF-8")
                                }
                            }
                        }
                        else -> {
                            viewNftImage.apply {
                                visibility = View.VISIBLE
                                val image = BitmapFactory.decodeFile(asset?.nft)
                                setImageBitmap(image)
                            }
                        }
                    }
                }
                "video" -> {
                    viewNftVideo.apply {
                        visibility = View.VISIBLE
                        setVideoPath(asset?.nft)
                        setMediaController(MediaController(context).apply {
                            setAnchorView(this)
                        })
                        setOnPreparedListener {
                            it.apply {
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

            viewNftName.text = asset?.name
            viewNftDomain.text = asset?.entity?.domain
            viewNftId.text = asset?.assetId
            viewNftDescription.text = asset?.description

            val attachmentList = mutableListOf<GenericItem>()

            asset?.attachments?.let {
                findViewById<TextView>(R.id.nftAttachments).visibility = View.VISIBLE

                for((key,value) in it) {
                    attachmentList += AttachmentListItem(StringHolder(key))
                }
                val itemAdapter = FastItemAdapter<GenericItem>()
                itemAdapter.add(attachmentList)

                //TODO: add attachemnt list with download button

                //binding.recycler.apply {
                  //  adapter = FastAdapter.with(itemAdapter)
                //}

                /*
                for (i in 0 until it.length() - 1) {
                    val aName = it.getJSONObject(i).getString("name")
                    val aUrl = it.getJSONObject(i).getString("fileUrl")
                    var aFile = File(assetDirectory, aName)
                    if (!aFile.exists()) {
                        aFile = downloadFile("${BtenderApi.BASE_URL}${aUrl}", aFile.absolutePath)
                        println("Download attachment $aName")
                    }
                    asset?.attachments?.add(aFile.absolutePath)
                }*/
            }

            findViewById<ConstraintLayout>(R.id.showmorewrapper).setOnClickListener {
                val domainUri = Uri.parse( "${BtenderApi.DOMAIN_PAGE}/${asset?.entity?.domain}")
                startActivity(Intent(Intent.ACTION_VIEW, domainUri))
            }
        }

        findViewById<ImageView>(R.id.buttonClose).setOnClickListener {
            finish()
        }
    }
}