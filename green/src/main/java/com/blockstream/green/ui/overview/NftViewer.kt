package com.blockstream.green.ui.overview
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import com.blockstream.green.R
import com.blockstream.green.ui.AppActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.Executors


@AndroidEntryPoint
class NftViewer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.nft)

        val nftView = findViewById<ImageView>(R.id.nft)

        intent?.let {
            val bundle: Bundle? = intent.extras

            val nftUrl = bundle?.get("nftUrl")

            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())

            // Initializing the image
            var image: Bitmap? = null

            executor.execute {

                try {
                    val `in` = java.net.URL(nftUrl as String?).openStream()
                    image = BitmapFactory.decodeStream(`in`)

                    handler.post {
                        nftView.setImageBitmap(image)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }
}