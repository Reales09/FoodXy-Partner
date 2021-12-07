package com.example.foodxypartner.product

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import com.example.foodxypartner.add.AddDialogFragment
import com.example.foodxypartner.core.Constants
import com.example.foodxypartner.data.Product
import com.example.foodxypartner.R
import com.example.foodxypartner.databinding.ActivityMainBinding
import com.example.foodxypartner.order.OrderActivity
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import java.security.MessageDigest
import java.util.*

class MainActivity : AppCompatActivity(), OnProductListener, MainAux {


    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var adapter: ProductAdapter

    private lateinit var  firestoreListener: ListenerRegistration

    private var productSelected: Product? = null

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val response = IdpResponse.fromResultIntent(it.data)

        if (it.resultCode == RESULT_OK) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN){
                    param(FirebaseAnalytics.Param.SUCCESS,100)//100 = login successfully
                    param(FirebaseAnalytics.Param.METHOD, "login")
                }
            }
        } else{
            if (response == null){
                Toast.makeText(this, "Hasta pronto", Toast.LENGTH_SHORT).show()
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN){
                    param(FirebaseAnalytics.Param.SUCCESS,200)//200 = login cancel
                    param(FirebaseAnalytics.Param.METHOD, "login")
                }
                finish()
            }else{
                response.error?.let {
                    if (it.errorCode == ErrorCodes.NO_NETWORK){
                        Toast.makeText(this, "Sin Red", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(this, "Codigod de error: ${it.errorCode}", Toast.LENGTH_SHORT).show()
                    }
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN){
                        param(FirebaseAnalytics.Param.SUCCESS,it.errorCode.toLong())
                        param(FirebaseAnalytics.Param.METHOD, "login")
                    }

                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        configAuth()
        configRecyclerView()
        //configFirestore()
        //configFirestoreRealtime()
        configButtons()
        configAnalytics()


    }


    private fun configAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { auth ->

            if (auth.currentUser != null) {
                supportActionBar?.title = auth.currentUser?.displayName
                binding.llprogress.visibility = View.GONE
                binding.nsvProducts.visibility = View.VISIBLE
                binding.efab.show()

            } else {
                val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build(),
                AuthUI.IdpConfig.FacebookBuilder().build(),
                AuthUI.IdpConfig.PhoneBuilder().build()

                    )

                val loginView = AuthMethodPickerLayout
                    .Builder(R.layout.view_login)
                    .setEmailButtonId(R.id.btnEmail)
                    .setGoogleButtonId(R.id.btnGoogle)
                    .setFacebookButtonId(R.id.btnFacebook)
                    .setPhoneButtonId(R.id.btnFacebook)
                    .setTosAndPrivacyPolicyId(R.id.tvPolicy)
                    .build()


                resultLauncher.launch(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setTosAndPrivacyPolicyUrls("","")
                        .setAvailableProviders(providers)
                        .setAuthMethodPickerLayout(loginView)
                        .build()
                )
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val info = getPackageManager().getPackageInfo(
                        "com.example.foodxypartner",
                        PackageManager.GET_SIGNING_CERTIFICATES)
                    for (signature in info.signingInfo.apkContentsSigners) {
                        val md = MessageDigest.getInstance("SHA")
                        md.update(signature.toByteArray())

                        //Log.d("API >= 28 KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT))
                    }
                } else {
                    val info = getPackageManager().getPackageInfo(
                        "com.example.foodxypartner",
                        PackageManager.GET_SIGNATURES);
                    for (signature in info.signatures) {
                        val md = MessageDigest.getInstance("SHA")
                        md.update(signature.toByteArray())
                     //   Log.d("API < 28 KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    }





    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authStateListener)
        configFirestoreRealtime()
    }

    override fun onPause() {
        super.onPause()
        firebaseAuth.removeAuthStateListener(authStateListener)
        firestoreListener.remove()
    }

    private fun configRecyclerView() {
        adapter = ProductAdapter(mutableListOf(), this)
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity,3,
                GridLayoutManager.HORIZONTAL, false)
            adapter=this@MainActivity.adapter
        }
       /* (1..20).forEach {
            val product = Product(it.toString(), "Producto $it", "Este producto es el $it", "", it, it * 1.1 )
            adapter.add(product)
        }

        */
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.action_sign_out -> {
                AuthUI.getInstance().signOut(this)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Sesión teminada", Toast.LENGTH_SHORT).show()

                        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN){
                            param(FirebaseAnalytics.Param.SUCCESS,100)//100 = sign out successfully
                            param(FirebaseAnalytics.Param.METHOD, "sign_out")
                        }
                    }
                    .addOnCompleteListener {
                        if (it.isSuccessful){
                            binding.nsvProducts.visibility = View.GONE
                            binding.llprogress.visibility = View.VISIBLE
                            binding.efab.hide()
                        }else{
                            Toast.makeText(this, "No se pudo cerrar la sesión", Toast.LENGTH_SHORT).show()

                            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN){
                                param(FirebaseAnalytics.Param.SUCCESS,201)//201 = error sign out
                                param(FirebaseAnalytics.Param.METHOD, "sign_out")
                            }
                        }
                    }
            }
            R.id.action_order_history -> startActivity(Intent(this, OrderActivity::class.java))

        }

        return super.onOptionsItemSelected(item)
    }

    private fun configFirestore() {
        val  db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_PRODUCTS)
            .get()
            .addOnSuccessListener { snapshots ->
                for (document in snapshots){
                    val product = document.toObject(Product::class.java)
                        product.id = document.id
                        adapter.add(product)
                }

            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al consultar datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configFirestoreRealtime() {

        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLL_PRODUCTS)

        firestoreListener = productRef.addSnapshotListener { snapshots, error ->

            if (error != null){

                Toast.makeText(this, "Error al consultar datos", Toast.LENGTH_SHORT).show()

                return@addSnapshotListener
            }
            for (snapshot in snapshots!!.documentChanges){
                val product = snapshot.document.toObject(Product::class.java)
                product.id = snapshot.document.id

                when(snapshot.type){
                    DocumentChange.Type.ADDED -> adapter.add(product)
                    DocumentChange.Type.MODIFIED -> adapter.update(product)
                    DocumentChange.Type.REMOVED -> adapter.delete(product)

                }
            }
        }

    }


    private fun configButtons() {
        binding.efab.setOnClickListener {
            productSelected = null
            AddDialogFragment().show(supportFragmentManager, AddDialogFragment::class.java.simpleName)
        }
    }

    private fun configAnalytics() {
        firebaseAnalytics = Firebase.analytics
    }


    override fun onClick(product: Product) {

        productSelected = product
        AddDialogFragment().show(supportFragmentManager, AddDialogFragment::class.java.simpleName)

    }

    override fun onLongClick(product: Product) {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLL_PRODUCTS)

        product.id?.let { id ->
            productRef.document(id)
                .delete()
                .addOnFailureListener {
                    Toast.makeText(this, "Error al consultar datos", Toast.LENGTH_SHORT).show()
                }
        }


    }

    override fun getProductSelected(): Product? = productSelected
}