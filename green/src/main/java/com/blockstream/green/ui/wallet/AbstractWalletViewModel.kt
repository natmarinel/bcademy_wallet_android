package com.blockstream.green.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.data.AppEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.devices.DeviceResolver
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.async
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.ConsumableEvent
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import mu.KLogging
import java.util.concurrent.TimeUnit


abstract class AbstractWalletViewModel constructor(
    val sessionManager: SessionManager,
    val walletRepository: WalletRepository,
    var wallet: Wallet,
) : AppViewModel() {

    sealed class WalletEvent: AppEvent {
        object RenameWallet : WalletEvent()
        object DeleteWallet : WalletEvent()
        object RenameAccount : WalletEvent()
        object AckMessage : WalletEvent()

        class Logout(val reason: LogoutReason) : WalletEvent()
    }

    enum class LogoutReason {
        USER_ACTION, DISCONNECTED, TIMEOUT, DEVICE_DISCONNECTED
    }

    val session = sessionManager.getWalletSession(wallet)

    private val walletLiveData: MutableLiveData<Wallet> = MutableLiveData(wallet)
    fun getWalletLiveData(): LiveData<Wallet> = walletLiveData

    private val subAccountLiveData: MutableLiveData<SubAccount> = MutableLiveData()
    fun getSubAccountLiveData(): LiveData<SubAccount> = subAccountLiveData

    // Logout events, can be expanded in the future
    val onReconnectEvent = MutableLiveData<ConsumableEvent<Long>>()

    private var reconnectTimer: Disposable? = null

    init {
        // Listen wallet updates from Database
        walletRepository
            .getWalletObservable(wallet.id)
            .async()
            .subscribe {
                wallet = it
                walletLiveData.value = wallet
                walletUpdated()
            }.addTo(disposables)


        session.device?.deviceState?.observe(viewLifecycleOwner){
            // Device went offline
            if(it == com.blockstream.green.devices.Device.DeviceState.DISCONNECTED){
                logout(LogoutReason.DEVICE_DISCONNECTED)
            }
        }

        // Only on Login Screen
        if (session.isConnected) {

            session.observable {
                it.getSubAccount(session.activeAccount)
            }.subscribe({
                subAccountLiveData.value = it
            }, {
                it.printStackTrace()
            }).addTo(disposables)


            session
                .getNetworkEventObservable()
                .async()
                .subscribeBy(
                    onNext = { event ->
                        // Dispose previous timer
                        reconnectTimer?.dispose()

                        if(event.isConnected){
                            onReconnectEvent.value = ConsumableEvent(-1)
                        } else {
                            reconnectTimer = Observable.interval(1, TimeUnit.SECONDS)
                                .take(event.waitInSeconds+ 1)
                                .map {
                                    event.waitInSeconds - it
                                }
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeBy(
                                    onNext = {
                                        onReconnectEvent.value = ConsumableEvent(it)
                                    }
                                ).addTo(disposables)
                        }
                    }
                )
                .addTo(disposables)
        }
    }

    open fun walletUpdated() {

    }

    open fun selectSubAccount(account: SubAccount) {
        subAccountLiveData.value = account

        wallet.activeAccount = account.pointer

        if(!wallet.isHardwareEmulated) {
            wallet.observable {
                walletRepository.updateWalletSync(wallet)
            }.subscribeBy()
        }

        session.setActiveAccount(account.pointer)
    }

    fun deleteWallet() {
        wallet.observable {
            sessionManager.destroyWalletSession(wallet)
            walletRepository.deleteWallet(wallet)
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onError = {
                onError.postValue(ConsumableEvent(it))
            },
            onSuccess = {
                onEvent.postValue(ConsumableEvent(WalletEvent.DeleteWallet))
            }
        )
    }

    fun renameSubAccount(index: Long, name: String) {
        if (name.isBlank()) return

        session.observable {
            it.renameSubAccount(index, name)
            it.getSubAccount(index)
        }.subscribeBy(
            onError = {
                onError.postValue(ConsumableEvent(it))
            },
            onSuccess = {
                subAccountLiveData.value = it
                onEvent.postValue(ConsumableEvent(WalletEvent.RenameAccount))

                // Update the subaccounts list
                session.updateSubAccountsAndBalances()
            }
        )
    }

    fun renameWallet(name: String) {
        if (name.isBlank()) return

        wallet.observable {
            wallet.name = name.trim()
            walletRepository.updateWalletSync(wallet)
        }.subscribeBy(
            onError = {
                onError.postValue(ConsumableEvent(it))
            },
            onSuccess = {
                onEvent.postValue(ConsumableEvent(WalletEvent.RenameWallet))
            }
        )
    }

    fun ackSystemMessage(message : String){
        session.observable {
            session.ackSystemMessage(message)
                .resolve(hardwareWalletResolver = DeviceResolver(null, session.hwWallet))
            session.updateSystemMessage()
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onSuccess = {
                onEvent.postValue(ConsumableEvent(WalletEvent.AckMessage))
            },
            onError = {
                onError.postValue(ConsumableEvent(it))
            }
        )
    }

    fun logout(reason: LogoutReason) {
        session.disconnectAsync()
        onEvent.postValue(ConsumableEvent(WalletEvent.Logout(reason)))
    }

    companion object : KLogging()
}