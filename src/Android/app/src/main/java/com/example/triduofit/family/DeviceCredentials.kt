package com.example.triduofit.family

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.net.URI

class DeviceCredentials(context: Context) {
    private val debug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    private val master = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs = EncryptedSharedPreferences.create(context, "familyfit_device", master, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    var server: String
        get() = prefs.getString("server", "https://fit.chouteau.info")!!
        set(value) {
            require(token == null || value.trim().trimEnd('/') == server) { "Le serveur ne peut pas changer tant que ce téléphone est associé." }
            val uri = URI(value.trim().trimEnd('/'))
            require(uri.host != null && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null && (uri.path.isNullOrEmpty() || uri.path == "/")) { "Adresse du site invalide." }
            require(uri.scheme == "https" || (debug && uri.scheme == "http" && (uri.host == "localhost" || uri.host == "127.0.0.1" || uri.host == "10.0.2.2"))) { "Le site doit utiliser HTTPS." }
            prefs.edit().putString("server", uri.toString().trimEnd('/')).commit()
        }
    val token: String? get() = prefs.getString("token", null)
    val memberId: String? get() = prefs.getString("member", null)
    val deviceId: String? get() = prefs.getString("device", null)
    fun save(grant: DeviceGrant) { check(prefs.edit().putString("token", grant.token).putString("member", grant.member.id).putString("device", grant.deviceId).commit()) { "Impossible de sécuriser l’autorisation sur ce téléphone." } }
}
