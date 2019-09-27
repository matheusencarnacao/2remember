@file:Suppress("DEPRECATION")

package br.com.tworemember.localizer.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.tworemember.localizer.providers.Preferences
import br.com.tworemember.localizer.providers.ProgressDialogProvider
import br.com.tworemember.localizer.R
import br.com.tworemember.localizer.DAO.UserDAO
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val callbackManager = CallbackManager.Factory.create()
    private lateinit var auth: FirebaseAuth
    private val userDAO = UserDAO()
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 2000
    private var dialog : ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        setContentView(R.layout.activity_main)

        configureGoogleClient()
        sign_in_button.setOnClickListener(this)

        val buttonFacebookLogin = login_button as LoginButton

        buttonFacebookLogin.setPermissions("email", "public_profile")
        buttonFacebookLogin.registerCallback(callbackManager, facebookCallback)
    }

    private fun configureGoogleClient(){
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        // Build a GoogleSignInClient with the options specified by gso.
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    public override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser
        if (currentUser != null){
            iniciarDialog()
            goToHome()
        }
    }

    private fun goToHome(){
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        dialog?.dismiss()
        finish()
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d("MainActiviry", "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("MainActivity", "signInWithCredential:success")
                    val user = auth.currentUser
                    user?.let { saveUser(it) }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("MainActivity", "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "Erro ao autenticar com o Facebook.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Pass the activity result back to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w("MainActivity", "Google sign in failed", e)
                // ...
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d("MainActivity", "firebaseAuthWithGoogle:" + acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("MainActivity", "signInWithCredential:success")
                    val user = auth.currentUser
                    user?.let{ saveUser(it) }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("MainActivity", "signInWithCredential:failure", task.exception)
                    //Snackbar.make(main_layout, "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
                    //updateUI(null)
                }
            }
    }

    private fun iniciarDialog(){
        dialog = ProgressDialogProvider.showProgressDialog(
            this,
            "Realizando login..."
        )
    }

    fun saveUser(currentUser: FirebaseUser){
        iniciarDialog()
        val user = userDAO.updateDb(currentUser)
        if(user == null){
            auth.signOut()
            Toast.makeText(this, "Erro ao cadastrar cliente", Toast.LENGTH_SHORT).show()
            return
        }
        Preferences(this).setUser(user)
        goToHome()
    }

    override fun onClick(v: View?) { signIn() }


    private val facebookCallback = object : FacebookCallback<LoginResult> {
        override fun onSuccess(loginResult: LoginResult) {
            Log.d("MainActivity", "facebook:onSuccess:$loginResult")
            handleFacebookAccessToken(loginResult.accessToken)
        }

        override fun onCancel() {
            Log.d("MainActivity!", "facebook:onCancel")
            Toast.makeText(this@MainActivity, "Cancelado pelo usuário", Toast.LENGTH_SHORT).show()
        }

        override fun onError(error: FacebookException) {
            Log.d("MainActivity: ", "facebook:onError", error.cause)
            Toast.makeText(this@MainActivity, "Ops! Ocorreu um erro ao logar, por favor tente novamente", Toast.LENGTH_SHORT).show()
        }
    }
}
