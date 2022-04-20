package com.blockstream.green.ui.overview
import Api
import Cache
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import androidx.recyclerview.widget.RecyclerView
import com.blockstream.gdk.data.Asset
import com.blockstream.green.R
import com.blockstream.green.ui.AppActivity
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors



@AndroidEntryPoint
class NftViewer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.nft)

        val nftView = findViewById<ImageView>(R.id.nft)
        val button=findViewById<ImageView>(R.id.buttonClose)
        val viewnftname=findViewById<TextView>(R.id.nftname)
        val viewnftdomain=findViewById<TextView>(R.id.nftdomain)
        val viewnftid=findViewById<TextView>(R.id.nftid)
        val viewnftdescription=findViewById<TextView>(R.id.nftdescription)
        val handler = Handler(Looper.getMainLooper())

        intent?.let {
            val bundle: Bundle? = intent.extras

            val uri = bundle?.get("uri")
            val asset= bundle?.get("asset") as Asset
            println("asset su nftviewer $asset")
            val nftname = asset?.name
            val nftdomain=asset?.entity?.domain
            val nftid=asset?.assetId
            val nftdescription=asset?.description

            //binding.nftname.text = asset?.name
            handler.post {
                val image=BitmapFactory.decodeFile(uri.toString())
                nftView.setImageBitmap(image)
                viewnftname.text=nftname
                viewnftdomain.text=nftdomain
                viewnftid.text=nftid
                viewnftdescription.text=nftdescription
            }
        }

        button.setOnClickListener{
            finish()
        }
    }
}