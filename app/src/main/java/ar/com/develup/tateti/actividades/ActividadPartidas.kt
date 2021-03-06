package ar.com.develup.tateti.actividades

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ar.com.develup.tateti.R
import ar.com.develup.tateti.adaptadores.AdaptadorPartidas
import ar.com.develup.tateti.modelo.Constantes
import ar.com.develup.tateti.modelo.Partida
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.android.synthetic.main.actividad_partidas.*

class ActividadPartidas : AppCompatActivity() {

    companion object {
        private const val TAG = "ActividadPartidas"
    }

    private lateinit var adaptadorPartidas: AdaptadorPartidas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actividad_partidas)
        adaptadorPartidas = AdaptadorPartidas(this)
        partidas.layoutManager = LinearLayoutManager(this)
        partidas.adapter = adaptadorPartidas
        nuevaPartida.setOnClickListener { nuevaPartida() }

        Firebase.messaging.token.addOnCompleteListener {
            if (it.isSuccessful) {
                // En este momento conocemos el valor del token
                Log.d("Notificaciones", it.result!!)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Obtener una referencia a la base de datos, suscribirse a los cambios en Constantes.TABLA_PARTIDAS
        // y agregar como ChildEventListener el listenerTablaPartidas definido mas abajo
        val rootRef = FirebaseDatabase.getInstance().reference
        val partidaRef = rootRef.child(Constantes.TABLA_PARTIDAS.toString())
        partidaRef.addChildEventListener(listenerTablaPartidas)
    }

    fun nuevaPartida() {
        val intent = Intent(this, ActividadPartida::class.java)
        startActivity(intent)
    }

    private val listenerTablaPartidas: ChildEventListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.i(TAG, "onChildAdded: $snapshot")
                val partida = snapshot.getValue(Partida::class.java) // Obtener el valor del dataSnapshot
                if (partida != null) {
                    partida.id = snapshot.key
                } // Asignar el valor del campo "key" del dataSnapshot
                if (partida != null) {
                    adaptadorPartidas.agregarPartida(partida)
                }
        }


        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            Log.i(TAG, "onChildChanged: $s")
            val partida = dataSnapshot.getValue(Partida::class.java) // Obtener el valor del dataSnapshot
            partida!!.id = dataSnapshot.key // Asignar el valor del campo "key" del dataSnapshot
            adaptadorPartidas.partidaCambio(partida)
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            Log.i(TAG, "onChildRemoved: ")
            val partida = dataSnapshot.getValue(Partida::class.java)  // Obtener el valor del dataSnapshot
            partida!!.id = dataSnapshot.key // Asignar el valor del campo "key" del dataSnapshot
            adaptadorPartidas.remover(partida)
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {
            Log.i(TAG, "onChildMoved: $s")
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Log.i(TAG, "onCancelled: ")
        }
    }


}