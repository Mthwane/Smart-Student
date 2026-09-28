package com.example.smartstudent.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Wraps Firebase Authentication for email/password (with real verification emails)
 * and Google Sign-In. Google Sign-In requires a SHA-1 fingerprint to be registered
 * with the Firebase project (Project settings > Your apps > Add fingerprint) — until
 * that's done, `default_web_client_id` won't exist as a resource, so we look it up
 * defensively instead of referencing R.string directly, and fail with a clear message
 * rather than crashing the build or the app.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signUpWithEmail(email: String, password: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: error("Sign up succeeded but no user was returned")
        user.sendEmailVerification().await()
        return user
    }

    suspend fun signInWithEmail(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: error("Sign in succeeded but no user was returned")
    }

    suspend fun resendVerificationEmail() {
        auth.currentUser?.sendEmailVerification()?.await()
    }

    /** Re-fetches the user from Firebase and reports whether their email is now verified. */
    suspend fun refreshEmailVerifiedStatus(): Boolean {
        auth.currentUser?.reload()?.await()
        return auth.currentUser?.isEmailVerified ?: false
    }

    suspend fun signInWithGoogleCredential(idToken: String): FirebaseUser {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user ?: error("Google sign-in succeeded but no user was returned")
    }

    fun signOut() {
        auth.signOut()
    }

    /**
     * Builds a GoogleSignInClient, or null if the project isn't configured for
     * Google Sign-In yet (no SHA-1 registered -> no default_web_client_id resource).
     */
    fun buildGoogleSignInClient(context: Context): GoogleSignInClient? {
        val webClientIdRes = context.resources.getIdentifier(
            "default_web_client_id", "string", context.packageName
        )
        if (webClientIdRes == 0) return null

        val webClientId = context.getString(webClientIdRes)
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, options)
    }
}
