package ar.com.develup.tateti.actividades

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ar.com.develup.tateti.R
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.android.synthetic.main.actividad_inicial.*

class ActividadInicial : AppCompatActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actividad_inicial)


        firebaseAnalytics = Firebase.analytics
        auth = Firebase.auth

        iniciarSesion.setOnClickListener {
            iniciarSesion()
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM){
                param(FirebaseAnalytics.Param.ITEM_NAME,"click login")
            }
        }
        registrate.setOnClickListener { registrate() }
        olvideMiContrasena.setOnClickListener { olvideMiContrasena() }

        if (usuarioEstaLogueado()) {
            // Si el usuario esta logueado, se redirige a la pantalla
            // de partidas
            verPartidas()
            finish()
        }
        actualizarRemoteConfig()
    }

    private fun usuarioEstaLogueado(): Boolean {
        // Validar que currentUser sea != null
        val currentUser = auth.currentUser
        if(currentUser != null){
            return true
        }
        return false
    }

    private fun verPartidas() {
        val intent = Intent(this, ActividadPartidas::class.java)
        startActivity(intent)
    }

    private fun registrate() {
        val intent = Intent(this, ActividadRegistracion::class.java)
        startActivity(intent)
    }

    private fun actualizarRemoteConfig() {
        configurarDefaultsRemoteConfig()
        configurarOlvideMiContrasena()
    }

    private fun configurarDefaultsRemoteConfig() {
        // Configurar los valores por default para remote config,
        // ya sea por codigo o por XML
        val settings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 60
        }
        Firebase.remoteConfig.setConfigSettingsAsync(settings)

        val defaults = mapOf("olvideContrasena" to false)
        Firebase.remoteConfig.setDefaultsAsync(defaults)

    }

    private fun configurarOlvideMiContrasena() {
        // Obtener el valor de la configuracion para saber si mostrar
        // o no el boton de olvide mi contraseña

        Firebase.remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                var botonOlvideHabilitado : Boolean = false
                if (task.isSuccessful){
                    botonOlvideHabilitado = Firebase.remoteConfig.getBoolean("olvideContrasena")
            }
                if (botonOlvideHabilitado) {
                    olvideMiContrasena.visibility = View.VISIBLE
                } else {
                    olvideMiContrasena.visibility = View.GONE
                }
        }

    }

    private fun olvideMiContrasena() {
        // Obtengo el mail
        val email = email.text.toString()

        // Si no completo el email, muestro mensaje de error
        if (email.isEmpty()) {
            Snackbar.make(rootView!!, "Completa el email", Snackbar.LENGTH_SHORT).show()
        } else {
            auth.sendPasswordResetEmail(email)
              .addOnCompleteListener { task ->
                  if (task.isSuccessful) {
                      Snackbar.make(rootView, "Email enviado", Snackbar.LENGTH_SHORT).show()
                  } else {
                      Snackbar.make(rootView, "Error " + task.exception, Snackbar.LENGTH_SHORT).show()
                  }
              }
        }
    }

    private fun iniciarSesion() {
        val email = email.text.toString()
        val password = password.text.toString()
        try{
            if (email == "" && password == "") {
                throw Exception("Credenciales vacias!")
            }else{
                auth.signInWithEmailAndPassword(email,password)
                    .addOnCompleteListener(this,authenticationListener)
            }

        }catch (e : Exception){
            FirebaseCrashlytics.getInstance().recordException(e)
            Snackbar.make(rootView!!,e.message.toString(),Snackbar.LENGTH_SHORT).show()
        }
    }

        private val authenticationListener: OnCompleteListener<AuthResult?> = OnCompleteListener<AuthResult?> { task ->
            if (task.isSuccessful) {
                if (usuarioVerificoEmail()) {
                    verPartidas()
                } else {
                    desloguearse()
                    Snackbar.make(rootView!!, "Verifica tu email para continuar", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                if (task.exception is FirebaseAuthInvalidUserException) {
                    Snackbar.make(rootView!!, "El usuario no existe", Snackbar.LENGTH_SHORT).show()
                } else if (task.exception is FirebaseAuthInvalidCredentialsException) {
                    Snackbar.make(rootView!!, "Credenciales inválidas", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

    private fun usuarioVerificoEmail(): Boolean {
        // Preguntar al currentUser si verifico email
        return auth.currentUser!!.isEmailVerified
    }

    private fun desloguearse() {
        // Hacer signOut de Firebase
        auth.signOut()
    }
}