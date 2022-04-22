package com.blockstream.green.ui.overview

import Api
import Cache
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.SimpleAdapter
import com.blockstream.green.R
import com.blockstream.green.adapters.setImageViewResource
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
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.Executors


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
        val assetId = arguments?.getString(ASSET_ID) ?: session.policyAsset
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
        val dircache = "/data/user/0/com.greenaddress.greenbits_android_wallet.dev/cache/images/"
        val FILE_EXTENSION = ".png"
        val file = File("$dircache$assetId$FILE_EXTENSION")
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        val BASE_URL = "https://btender.bcademy.xyz/"
        val json = Api().getJsonString(assetId)
        Thread.sleep(1_000)
        println("json $json")
        println("dentro il try")
        if (file.exists()) {
            val jsonObject = JSONObject(json.take().toString())
            println("jsonObject $jsonObject")
            val nftContract = jsonObject.getJSONObject("nftContract")
            val meta = nftContract.getJSONArray("meta")
            val n = meta.getJSONObject(0)
            val description = n.getString("description")
            asset?.description=description
            println("file già presente nella cache")
            val image = BitmapFactory.decodeFile(file.toString())
            handler.post {
                println(asset)
                println(asset?.description)
                binding.nftcontent.visibility = View.VISIBLE
                binding.nftname.visibility=View.VISIBLE
                binding.nftname.text = asset?.name

                binding.nftcontent.setImageBitmap(image)
                binding.nftname.setOnClickListener{
                    val intent = Intent(context?.applicationContext, NftViewer::class.java)
                    intent.putExtra("uri", file)
                    intent.putExtra("asset",asset)
                    startActivity(intent)
                }
                binding.nftcontent.setOnClickListener {
                    println("click")
                    val intent = Intent(context?.applicationContext, NftViewer::class.java)
                    intent.putExtra("uri", file)
                    intent.putExtra("asset",asset)
                    startActivity(intent)
                }
            }

        } else {
            println("file non esiste")


            if (json.isNotEmpty()) {
                val jsonObject = JSONObject(json.take().toString())
                println("jsonObject $jsonObject")
                val nftContract = jsonObject.getJSONObject("nftContract")
                val media = nftContract.getJSONObject("media")
                val fileUrl = media.getString("fileUrl")


                nftContract.let {
                    val nftUrl = BASE_URL + fileUrl
                    executor.execute {
                        val meta = nftContract.getJSONArray("meta")
                        val n = meta.getJSONObject(0)
                        val description = n.getString("description")
                        asset?.description=description
                        println("description di setting ${asset?.description}")
                        val `in` = java.net.URL(nftUrl as String?).openStream()
                        val image = BitmapFactory.decodeStream(`in`)
                        val uri = Cache().saveToCacheAndGetUri(image, assetId)
                        println("questo è uri $uri")
                        handler.post {
                            println("Sto per settare immagine")
                            binding.nftcontent.setImageBitmap(image)
                            binding.nftcontent.visibility = View.VISIBLE
                            binding.nftname.visibility=View.VISIBLE
                            binding.nftname.text = asset?.name
                            binding.nftname.setOnClickListener{
                                val intent = Intent(context?.applicationContext, NftViewer::class.java)
                                intent.putExtra("uri", file)
                                intent.putExtra("asset",asset)
                                startActivity(intent)
                            }
                            binding.nftcontent.setOnClickListener {
                                println("click")
                                val intent =Intent(context?.applicationContext, NftViewer::class.java)
                                intent.putExtra("uri", file)
                                intent.putExtra("asset", asset)
                                startActivity(intent)
                            }
                        }

                    }
                }
            } else {
                val itemAdapter = FastItemAdapter<GenericItem>()
                itemAdapter.add(list)

                binding.recycler.apply {
                    adapter = FastAdapter.with(itemAdapter)
                }
            }
        }
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