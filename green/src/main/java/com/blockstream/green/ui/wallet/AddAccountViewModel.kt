package com.blockstream.green.ui.wallet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.data.AccountType
import com.blockstream.gdk.data.SubAccount
import com.blockstream.gdk.params.SubAccountParams
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.lifecycle.ListenableLiveData
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.nameCleanup
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class AddAccountViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    @Assisted wallet: Wallet,
    @Assisted val accountType: AccountType,
    @Assisted("mnemonic") val mnemonic: String?,
    @Assisted("xpub") val xpub: String?,
) : AbstractWalletViewModel(sessionManager, walletRepository, wallet) {

    val isEnabled = MutableLiveData(false)

    val accountName = ListenableLiveData("") {
        isEnabled.value = it.isNotBlank()
    }

    val accountCreated = MutableLiveData<SubAccount>()

    fun createAccount() {
        session.observable {
            accountName.value.nameCleanup()!!.let { name ->
                it.createSubAccount(SubAccountParams(
                    name = name,
                    type = accountType,
                    recoveryMnemonic = mnemonic,
                    recoveryXpub = xpub
                ))
            }
        }.doOnSubscribe {
            isEnabled.postValue(false)
        }.doOnTerminate {
            isEnabled.postValue(true)
        }.subscribe({
            accountCreated.value = it
        }, {
            onError.value = ConsumableEvent(it)
        })
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            accountType: AccountType,
            @Assisted("mnemonic")
            mnemonic: String?,
            @Assisted("xpub")
            xpub: String?
        ): AddAccountViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            accountType: AccountType,
            mnemonic: String?,
            xpub: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, accountType, mnemonic, xpub) as T
            }
        }
    }
}