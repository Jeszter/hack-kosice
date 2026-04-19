package com.equipay.app

import android.app.Application
import com.equipay.app.auth.AuthRepository
import com.equipay.app.auth.TokenStore
import com.equipay.app.network.ApiClient

class EquiPayApp : Application() {

    lateinit var tokenStore: TokenStore
        private set

    lateinit var authRepo: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        tokenStore = TokenStore(this)
        ApiClient.init(tokenStore)
        authRepo = AuthRepository(tokenStore)
        instance = this
    }

    companion object {
        lateinit var instance: EquiPayApp
            private set
    }
}
