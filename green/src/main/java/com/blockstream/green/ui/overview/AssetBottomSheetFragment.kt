package com.blockstream.green.ui.overview

import Api
import android.content.Intent
import android.os.Bundle
import android.view.View
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
import org.json.JSONObject


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

        list += OverlineTextListItem(StringHolder(R.string.id_name), StringHolder(if(isPolicyAsset) look.name else asset?.name ?: getString(R.string.id_no_registered_name_for_this)))

        val balanceListItem = OverlineTextListItem(StringHolder(R.string.id_total_balance), StringHolder("-"))
        val blockHeightListItem = OverlineTextListItem(StringHolder(R.string.id_block_height), StringHolder("-"))

        if(isPolicyAsset){
            list += blockHeightListItem
            list += balanceListItem
        }else{
            list += OverlineTextListItem(StringHolder(R.string.id_asset_id), StringHolder(assetId))
            list += OverlineTextListItem(StringHolder(R.string.id_immaginina), StringHolder(assetId))
            list += balanceListItem
            list += OverlineTextListItem(StringHolder(R.string.id_precision), StringHolder((asset?.precision ?: 0).toString()))
            list += OverlineTextListItem(StringHolder(R.string.id_ticker), StringHolder(asset?.ticker ?: getString(R.string.id_no_registered_ticker_for_this)))
            list += OverlineTextListItem(StringHolder(R.string.id_issuer), StringHolder(asset?.entity?.domain ?: getString(R.string.id_unknown)))
        }
        //DA QUI DEVE ESSERCI IL CONTROLLO
        val callApi= "https://btender.bcademy.xyz/api/v1/assets_contract/$assetId/"
        val id_nft_rosso = "b2489840efd410372c0e3a782c0553e2aedea07f34cb3ae02d2331f9f78845e1"
        val BASE_URL="https://btender.bcademy.xyz/"
        val cicciaassetId=assetId
        if(assetId == "ca733301ae5b406d66f3a6a55f9d61917f24acc54641becaab1be478bba8e826") {
            //CONTROLLO SE L'IMMAGINE DI QUELL'ASSET E' PRESENTE NELLA CACHE
            val cicciaassetId = id_nft_rosso

            val json = Api().getJsonString(cicciaassetId)
            Thread.sleep(1_000)

            println("ENTIRE JSON")
            println("$json")
            println("---------------------------")
            val jsonObject = JSONObject(json.take().toString())

            val nftContract = jsonObject.getJSONObject("nftContract")
            if (nftContract != null) {
                nftContract.let {
                    val media = nftContract.getJSONObject("media")
                    val fileUrl = media.getString("fileUrl")

                    val nftUrl = BASE_URL + fileUrl

                    val intent = Intent(context?.applicationContext, NftViewer::class.java)
                    intent.putExtra("nftUrl", nftUrl)
                    startActivity(intent)
                }
            }
        }

        /*
        val callApi= "https://btender.bcademy.xyz/api/v1/assets_contract/$assetId/"
        val id_nft_rosso="b2489840efd410372c0e3a782c0553e2aedea07f34cb3ae02d2331f9f78845e1"
        val utile="https://btender.bcademy.xyz/"
        //val response= URL(callApi).readText()
        //println(response)
        //val json="""{"state":"completed","assetId":"b2489840efd410372c0e3a782c0553e2aedea07f34cb3ae02d2331f9f78845e1","domain":"bfungible.bcademy.xyz","name":"Rosso","precision":0,"ticker":"NCAAH","issuerPubkey":"032707d05b6aa5e823956b5a0d83e5d1d2acd80e8d8d5a2833f3157f3604b0ef8d","version":0,"icon":null,"nftContract":{"domain":"bfungible.bcademy.xyz","version":0,"media":{"fileUrl":"/media/b2489840efd410372c0e3a782c0553e2aedea07f34cb3ae02d2331f9f78845e1/QmbTAus1x6G6qweygyGeoC1PCjyrHYdxoK2RaJdKhr6XES","contentType":"image/jpeg"},"attachments":[{"isPrivate":false,"fileUrl":"/media/b2489840efd410372c0e3a782c0553e2aedea07f34cb3ae02d2331f9f78845e1/QmPWXvXmqNAF71BhFzb9gZTHRg9R7Xd5rq3WFBtJ5czYdJ","name":"allegato.jpg","contentType":"image/jpeg"}],"meta":[{"language":"","name":"Rosso","description":"un quadrato rosso"}]}}"""
        val json= Api().getJsonString(test)    //.toString()  bisogna rimuovere le stringhe dal risultato
        Thread.sleep(1_000)
        println("ciao ciao$json")
        val ok=json.take().toString()
        val jsonObject=JSONObject(ok)
        val result=jsonObject.getJSONObject("nftContract")
        val result2=result.getJSONObject("media")
        val result3=result2.getString("fileUrl")
        val nfturl=utile+result3
        println("speriamo nel $result3")
        println("media nel $nfturl")
        */

        val itemAdapter = FastItemAdapter<GenericItem>()
        itemAdapter.add(list)

        binding.recycler.apply {
            adapter = FastAdapter.with(itemAdapter)
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        session.getBalancesObservable()
            .subscribeBy(
                onNext = {
                    look.amount = it[assetId] ?: 0
                    balanceListItem.text = StringHolder(look.balance(withUnit = false))
                    binding.recycler.adapter?.notifyItemChanged(list.indexOf(balanceListItem))
                },
                onError = { }
            ).addTo(disposables)

        session.getBlockObservable().subscribeBy(
            onNext = {
                blockHeightListItem.text = StringHolder(it.height.toString())
            },
            onError = { }
        ).addTo(disposables)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}