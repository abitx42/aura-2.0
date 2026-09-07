package com.example.auth

import android.content.Context
import android.content.Intent
import com.example.AuraApplication
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {

    private val auth: FirebaseAuth? by lazy {
        try {
            AuraApplication.ensureFirebaseInitialized(context)
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Throwable) {
            android.util.Log.w("AuthManager", "Firebase Auth unavailable: ${e.message}")
            null
        }
    }

    // Current signed-in user (null if not signed in)
    val currentUser: FirebaseUser? get() = try { auth?.currentUser } catch (e: Throwable) { null }
    val userId: String? get() = try { auth?.currentUser?.uid } catch (e: Throwable) { null }
    val isSignedIn: Boolean get() = try { auth?.currentUser != null } catch (e: Throwable) { false }

    // Build the Google Sign-In intent — launch this from your Activity
    fun getSignInIntent(): Intent {
        val webClientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else "YOUR_WEB_CLIENT_ID_FROM_FIREBASE_CONSOLE"
        } catch (e: Exception) {
            "YOUR_WEB_CLIENT_ID_FROM_FIREBASE_CONSOLE"
        }

        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()

        if (webClientId.isNotBlank() && webClientId != "YOUR_WEB_CLIENT_ID_FROM_FIREBASE_CONSOLE") {
            gsoBuilder.requestIdToken(webClientId)
        }

        return GoogleSignIn.getClient(context, gsoBuilder.build()).signInIntent
    }

    // Called after Google Sign-In returns an idToken
    suspend fun signInWithGoogle(idToken: String): Boolean {
        val currentAuth = auth ?: return false
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            currentAuth.signInWithCredential(credential).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(context, gso).signOut()
        } catch (_: Exception) {}
    }
}

