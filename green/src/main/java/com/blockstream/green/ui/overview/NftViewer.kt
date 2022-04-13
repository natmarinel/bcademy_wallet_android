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

        intent?.let {
            val bundle: Bundle? = intent.extras

            val nftUrl = bundle?.get("nftUrl")
            val nftId = bundle?.get("nftId") as String
            val dircache="/data/user/0/com.greenaddress.greenbits_android_wallet.dev/cache/images/"
            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())
            val file= File("$dircache$nftId.png")
            println(file)
            if(file.exists())
            {
                println("file già presente nella cache")
                val image=BitmapFactory.decodeFile(file.toString())
                handler.post {
                    nftView.setImageBitmap(image)
                }
            }else{
                println("file non esiste")
                executor.execute{
                    try {
                        val `in` = java.net.URL(nftUrl as String?).openStream()
                        val image = BitmapFactory.decodeStream(`in`)
                        println("image $image")
                        println("NftID $nftId")
                        val uri=Cache().saveToCacheAndGetUri(image,nftId)
                        println("questo uri della cache $uri")

                        handler.post {
                            nftView.setImageBitmap(image)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()

                    }
                }

            }
            //CONTROLLO SE L'IMMAGINE DI QUELL'ASSET E' PRESENTE NELLA CACHE

            //SE NON C'E' ESEGUI IL CODICE QUA SOTTO

        }
    }
}