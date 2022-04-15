package com.blockstream.green.ui.overview
import Cache
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
import java.io.File
import java.util.concurrent.Executors


@AndroidEntryPoint
class NftViewer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.nft)

        val nftView = findViewById<ImageView>(R.id.nft)
        val handler = Handler(Looper.getMainLooper())

        intent?.let {
            val bundle: Bundle? = intent.extras

            val uri = bundle?.get("uri")
            handler.post {
                val image=BitmapFactory.decodeFile(uri.toString())
                nftView.setImageBitmap(image)
            }
        }
    }
}