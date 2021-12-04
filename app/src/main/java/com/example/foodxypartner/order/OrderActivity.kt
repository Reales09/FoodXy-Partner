package com.example.foodxypartner.order

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodxypartner.R
import com.example.foodxypartner.chat.ChatFragment
import com.example.foodxypartner.core.Constants
import com.example.foodxypartner.data.Order
import com.example.foodxypartner.databinding.ActivityOrderBinding
import com.example.foodxypartner.fcm.NotificationRS
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firestore.v1.StructuredQuery

class OrderActivity : AppCompatActivity(), OnOrderListener, OrderAux {

    private lateinit var binding: ActivityOrderBinding

    private lateinit var adapter: OrderAdapter

    private lateinit var orderSelected: Order

    private val aValues: Array<String> by lazy {
        resources.getStringArray(R.array.status_value)
    }

    private val aKeys: Array<Int> by lazy {
        resources.getIntArray(R.array.status_key).toTypedArray()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFirestore()


    }

    private fun setupRecyclerView() {
        adapter = OrderAdapter(mutableListOf(),this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@OrderActivity)
            adapter = this@OrderActivity.adapter
        }
    }

    private fun setupFirestore() {

        val db = FirebaseFirestore.getInstance()

        db.collection(Constants.COLL_REQUEST)
            .get()
            .addOnSuccessListener {
                for (document in it){
                    val order = document.toObject(Order::class.java)
                    order.id = document.id
                    adapter.add(order)
                }
            }

            .addOnFailureListener {
                Toast.makeText(this, "Errror al consultar los datos", Toast.LENGTH_SHORT).show()
            }

    }

    private fun notifyClient(order: Order){

        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_USERS)
            .document(order.clientId)
            .collection(Constants.COLL_TOKENS)
            .get()
            .addOnSuccessListener {
                var tokenStr = ""
                for (document in it){

                    val tokenMap = document.data
                    tokenStr += "${tokenMap.getValue(Constants.PROP_TOKEN)},"
                }
                if (tokenStr.length > 0){ tokenStr = tokenStr.dropLast(1)

                var names =""
                order.products.forEach {
                    names +="${it.value.name}, "
                }
                names = names.dropLast(2)

                val index = aKeys.indexOf(order.status)

                val notificationRS = NotificationRS()
                notificationRS.sendNotificaion("Tu pedido ha sido ${aValues[index]}", names,tokenStr)

            }
          }
            .addOnFailureListener {
                Toast.makeText(this, "Errror al consultar los datos", Toast.LENGTH_SHORT).show()
            }
    }

    override fun OnStatusChange(order: Order) {

        val db=FirebaseFirestore.getInstance()

        db.collection(Constants.COLL_REQUEST)
            .document(order.id)
            .update(Constants.PROP_STATUS, order.status)
            .addOnSuccessListener {
                Toast.makeText(this, "Orden actualizada", Toast.LENGTH_SHORT).show()

                notifyClient(order)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar orden.", Toast.LENGTH_SHORT).show()
            }

    }

    override fun onStartChat(order: Order) {

        orderSelected = order

        val fragment = ChatFragment()

        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerMain, fragment)
            .addToBackStack(null)
            .commit()

    }

    override fun getOrderSelected(): Order = orderSelected
}