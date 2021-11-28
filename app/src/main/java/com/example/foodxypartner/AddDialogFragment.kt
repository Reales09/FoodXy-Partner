package com.example.foodxypartner

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.foodxypartner.databinding.FragmentDialogAddBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

class AddDialogFragment : DialogFragment(), DialogInterface.OnShowListener {

    private var binding: FragmentDialogAddBinding? = null

    private var positiveButton: Button? = null
    private var negativeButton: Button? = null

    private var product: Product?=null

    private var photoSelectedUri: Uri? = null

    private val resultLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == Activity.RESULT_OK){
            photoSelectedUri = it.data?.data

            //binding?.imgProductPreview?.setImageURI(photoSelectedUri)
            binding?.let {

                Glide.with(this)
                    .load(photoSelectedUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(it.imgProductPreview)

            }

        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let {activity ->
            binding = FragmentDialogAddBinding.inflate(LayoutInflater.from(context))

            binding?.let {
                val builder = AlertDialog.Builder(activity)
                    .setTitle("Agregar producto")
                    .setPositiveButton("Agregar", null)
                    .setNegativeButton("Cancelar",null)
                    .setView(it.root)

                val dialog = builder.create()
                dialog.setOnShowListener(this)

                return dialog

            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onShow(dialogInterfac: DialogInterface?) {

        initProduct()
        configButtons()

        val dialog = dialog as? AlertDialog
        dialog?.let { it ->
            positiveButton = it.getButton(Dialog.BUTTON_POSITIVE)
            negativeButton = it.getButton(Dialog.BUTTON_NEGATIVE)

            positiveButton?.setOnClickListener {

                binding?.let {

                    enableUI(false)

                    uploadImage(product?.id){eventPost ->

                        if (eventPost.isSuccess){


                            if (product == null){

                                val product = Product(name = it.etName.text.toString().trim(),
                                    description = it.etDescription.text.toString().trim(),
                                    imgUrl = eventPost.photoUrl,
                                    quantity = it.etQuatity.text.toString().toInt(),
                                    price = it.etPrice.text.toString().toDouble())

                                eventPost.documentId?.let { it1 -> save(product, it1) }



                            }else{
                                    product?.apply {
                                    name = it.etName.text.toString().trim()
                                    description = it.etDescription.text.toString().trim()
                                    imgUrl = eventPost.photoUrl
                                    quantity = it.etQuatity.text.toString().toInt()
                                    price = it.etPrice.text.toString().toDouble()

                                    update(this)
                                }
                            }

                        }

                    }

                }

            }

            negativeButton?.setOnClickListener {

                dismiss()
            }

        }

    }


    private fun initProduct() {
        product = (activity as? MainAux)?.getProductSelected()
        product?.let { product ->
            binding?.let {
                it.etName.setText(product.name)
                it.etDescription.setText(product.description)
                it.etQuatity.setText(product.quantity.toString())
                it.etPrice.setText(product.price.toString())

                Glide.with(this)
                    .load(product.imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(it.imgProductPreview)


            }
        }
    }

    private fun configButtons() {
        binding?.let {
            it.ibProduct.setOnClickListener {
                openGallery()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    private fun uploadImage(productId:String?, callback: (EventPost)->Unit) {
        val eventPost = EventPost()
        eventPost.documentId = productId ?: FirebaseFirestore.getInstance().collection(Constants.COLL_PRODUCTS).document().id

        val storageRef = FirebaseStorage.getInstance().reference.child(Constants.PATH_PRODUCT_IMAGES)

        photoSelectedUri?.let {uri ->
            binding?.let { binding ->
                binding.progressBar.visibility = View.VISIBLE

                val photoRef = storageRef.child(eventPost.documentId!!)

                photoRef.putFile(uri)

                    .addOnProgressListener {
                        val progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                        it.run {
                            binding.progressBar.progress = progress
                            binding.tvProgress.text = String.format("%s%%", progress)

                        }
                    }
                    .addOnSuccessListener {

                        it.storage.downloadUrl.addOnSuccessListener {downloadUrl ->
                            Log.i("URL", downloadUrl.toString())
                            eventPost.isSuccess=true
                            eventPost.photoUrl = downloadUrl.toString()
                            callback(eventPost)
                        }
                    }
                    .addOnFailureListener{

                        Toast.makeText(activity, "Error al subir imagen", Toast.LENGTH_SHORT).show()

                        enableUI(true)
                        eventPost.isSuccess=false
                        callback(eventPost)
                    }

            }
        }
    }

    private fun save (product: Product, documentId: String){

        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_PRODUCTS)
            .document(documentId)
            .set(product)
            //.add(product)
            .addOnSuccessListener {
                Toast.makeText(activity, "Producto añadido", Toast.LENGTH_SHORT).show()
            }

            .addOnFailureListener {
                Toast.makeText(activity, "Error al insertar", Toast.LENGTH_SHORT).show()
            }

            .addOnCompleteListener {
                enableUI(true)
                binding?.progressBar?.visibility = View.INVISIBLE
                dismiss()
            }
    }

    private fun update (product: Product){

        val db = FirebaseFirestore.getInstance()

        product.id?.let {id ->

            db.collection(Constants.COLL_PRODUCTS)
                .document(id)
                .set(product)
                .addOnSuccessListener {
                    Toast.makeText(activity, "Producto actualizado", Toast.LENGTH_SHORT).show()
                }

                .addOnFailureListener {
                    Toast.makeText(activity, "Error al actualizar", Toast.LENGTH_SHORT).show()
                }

                .addOnCompleteListener {
                    enableUI(true)
                    binding?.progressBar?.visibility = View.INVISIBLE
                    dismiss()
                }

        }


    }
    private fun enableUI(enable: Boolean){

        positiveButton?.isEnabled = enable
        negativeButton?.isEnabled = enable
        binding?.let {
            with(it){

                etName.isEnabled = enable
                etDescription.isEnabled = enable
                etQuatity.isEnabled = enable
                etPrice.isEnabled = enable

            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

}