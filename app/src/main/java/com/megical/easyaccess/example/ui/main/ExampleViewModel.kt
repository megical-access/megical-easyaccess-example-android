package com.megical.easyaccess.example.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.megical.easyaccess.example.ClientData
import com.megical.easyaccess.playground.OpenIdClientDataResponse
import com.megical.easyaccess.playground.PlaygroundRestApi
import com.megical.easyaccess.sdk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.*
import com.megical.easyaccess.sdk.MegicalAuthApi.Callback as MegicalCallback

const val REDIRECT_URL = "com.megical.ea.example:/oauth-callback"

enum class ViewState {
    RegisterClient,
    Loading,
    WaitLoginData,
    Authenticate,
    Hello,
    Easyaccess
}

class ExampleViewModel : ViewModel() {

    private val playgroundRestApi = PlaygroundRestApi()
    private val megicalAuthApi = MegicalAuthApi()

    private val viewState: MutableLiveData<ViewState> by lazy {
        MutableLiveData<ViewState>(ViewState.RegisterClient)
    }

    private val metadata: MutableLiveData<Metadata> by lazy {
        MutableLiveData<Metadata>()
    }

    private val state: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    private val clientData: MutableLiveData<ClientData> by lazy {
        MutableLiveData<ClientData>()
    }

    private val authentication: MutableLiveData<Authentication> by lazy {
        MutableLiveData<Authentication>()
    }

    private var authorizationService: MegicalAuthApi.AuthorizationService? = null

    private val tokenSet: MutableLiveData<TokenSet> by lazy {
        MutableLiveData<TokenSet>()
    }

    fun getViewState(): LiveData<ViewState> {
        return viewState
    }

    fun getHealthcheck() = liveData(Dispatchers.IO) {
        try {
            emit(playgroundRestApi.healthcheck())
        } catch (error: Exception) {
            Timber.e(error)
            emit(null)
        }
    }

    fun registerClient(token: String) = runBlocking(Dispatchers.IO) {
        try {
            val uuidToken = UUID.fromString(token)
            viewState.postValue(ViewState.Loading)
            val openIdClientData = playgroundRestApi.openIdClientData(uuidToken)
            registerClient(openIdClientData)
        } catch (error: Exception) {
            Timber.e(error)
            viewState.postValue(ViewState.RegisterClient)
        }
    }


    fun hello(accessToken: String) = liveData(Dispatchers.IO) {
        try {
            emit(playgroundRestApi.hello(accessToken))
        } catch (error: Exception) {
            Timber.e(error)
            viewState.postValue(ViewState.Authenticate)
            emit(null)
        }
    }

    private fun registerClient(
        openIdClientData: OpenIdClientDataResponse,
    ) {
        megicalAuthApi.client(
            openIdClientData.url, openIdClientData.clientToken,
            "${UUID.randomUUID()}", openIdClientData.appId,
            object : MegicalCallback<Client> {
                override fun onSuccess(response: Client) {
                    setClientData(
                        ClientData(
                            openIdClientData.appId,
                            response.clientId,
                            openIdClientData.audience,
                            openIdClientData.authEnvUrl,
                            openIdClientData.url
                        )
                    )
                    viewState.postValue(ViewState.Authenticate)
                }

                override fun onFailure(error: MegicalException) {
                    Timber.e(error)
                    viewState.postValue(ViewState.RegisterClient)
                }
            })
    }


    fun deregisterClient() {
        clientData.value?.let {
            megicalAuthApi.deleteClient(it.clientUrl,
                it.clientId,
                object : MegicalCallback<Unit> {
                    override fun onSuccess(response: Unit) {
                        Timber.i("Client deleted")
                    }

                    override fun onFailure(error: MegicalException) {
                        Timber.e(error)
                    }
                })
        }
        clientData.postValue(null)
        viewState.postValue(ViewState.RegisterClient)
    }

    fun authenticate() {
        getClientData().value?.let { (appId, clientId, audience, authEnvUrl) ->
            authorizationService = megicalAuthApi.AuthorizationService(authEnvUrl,
                appId,
                clientId,
                audience,
                REDIRECT_URL)

            authorizationService!!.authenticate(object : MegicalCallback<Authentication> {
                override fun onSuccess(response: Authentication) {
                    authentication.postValue(response)
                    viewState.postValue(ViewState.WaitLoginData)
                }

                override fun onFailure(error: MegicalException) {
                    Timber.e(error)
                    viewState.postValue(ViewState.Authenticate)
                }
            })
            viewState.postValue(ViewState.Loading)
        }
    }

    fun logout() {
        viewState.postValue(ViewState.Authenticate)
    }

    fun setClientData(value: ClientData) {
        clientData.postValue(value)
        viewState.postValue(ViewState.Authenticate)
    }

    fun getClientData(): LiveData<ClientData> {
        return clientData
    }

    fun getAuthentication(): LiveData<Authentication> {
        return authentication
    }

    fun getTokenSet(): LiveData<TokenSet> {
        return tokenSet
    }

    fun verify() {
        authorizationService!!.verify(
            object : MegicalCallback<TokenSet> {
                override fun onSuccess(response: TokenSet) {
                    tokenSet.postValue(response)
                    viewState.postValue(ViewState.Hello)
                }

                override fun onFailure(error: MegicalException) {
                    Timber.e(error)
                    viewState.postValue(ViewState.Authenticate)
                }
            })
    }

    fun getMetadata(): LiveData<Metadata> {
        return metadata
    }

    fun getState(): LiveData<String> {
        return state
    }

    fun fetchMetadata() {
        viewState.postValue(ViewState.Easyaccess)
        val loginCode = authentication.value!!.loginCode
        authorizationService!!.metadata(loginCode, object : MegicalCallback<Metadata> {
            override fun onSuccess(response: Metadata) {
                metadata.postValue(response)
            }

            override fun onFailure(error: MegicalException) {
                Timber.e(error)
                viewState.postValue(ViewState.Authenticate)
            }
        })
    }

    fun fetchState() {
        val loginCode = authentication.value!!.loginCode
        authorizationService!!.state(loginCode, object : MegicalCallback<State> {
            override fun onSuccess(response: State) {
                state.postValue(response.state)
            }

            override fun onFailure(error: MegicalException) {
                Timber.e(error)
                viewState.postValue(ViewState.Authenticate)
            }
        })
    }
}