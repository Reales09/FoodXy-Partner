package com.example.foodxypartner.promo

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
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
import com.example.foodxypartner.data.EventPost
import com.example.foodxypartner.core.Constants
import com.example.foodxypartner.data.Product
import com.example.foodxypartner.databinding.FragmentDialogAddBinding
import com.example.foodxypartner.databinding.FragmentPromoBinding
import com.example.foodxypartner.fcm.NotificationRS
import com.example.foodxypartner.product.MainAux
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class PromoFragment : DialogFragment(), DialogInterface.OnShowListener {

    private var binding: FragmentPromoBinding? = null

    private var positiveButton: Button? = null
    private var negativeButton: Button? = null

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
            binding = FragmentPromoBinding.inflate(LayoutInflater.from(context))

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


        configButtons()

        val dialog = dialog as? AlertDialog
        dialog?.let { it ->
            positiveButton = it.getButton(Dialog.BUTTON_POSITIVE)
            negativeButton = it.getButton(Dialog.BUTTON_NEGATIVE)

            positiveButton?.setOnClickListener {

                binding?.let {

                    enableUI(false)

                    uploadReducedImage()
                    }

                }

            }

            negativeButton?.setOnClickListener {

                dismiss()
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


    private fun uploadReducedImage() {



            photoSelectedUri?.let {uri ->
                binding?.let { binding ->

                    getBitmapFromUri(uri)?.let {bitmap ->
                        binding.progressBar.visibility = View.VISIBLE

                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG,90,baos)

                        val promoRef = FirebaseStorage.getInstance().reference.child("promos").child(binding.etTopic.text.toString().trim())

                        promoRef.putBytes(baos.toByteArray())

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
                                    val notificationRS = NotificationRS()
                                    notificationRS.sendNotificaionByTopic(

                                        binding.etTittle.text.toString().trim(),
                                        binding.etDescription.text.toString().trim(),
                                        binding.etTopic.text.toString().trim(),
                                        downloadUrl.toString()

                                    ){
                                        if (it){
                                            Toast.makeText(activity, "Promoción enviada", Toast.LENGTH_SHORT).show()
                                            dismiss()
                                        }else{
                                            Toast.makeText(activity, "Error, intente más tarde", Toast.LENGTH_SHORT).show()
                                        }
                                        enableUI(true)
                                    }


                                }
                            }
                            .addOnFailureListener{

                                Toast.makeText(activity, "Error al subir imagen", Toast.LENGTH_SHORT).show()

                                enableUI(true)

                            }
                    }
                }
            }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap?{

        activity?.let {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                val source = ImageDecoder.createSource(it.contentResolver,uri)
                ImageDecoder.decodeBitmap(source)

            }else{
                MediaStore.Images.Media.getBitmap(it.contentResolver, uri)
            }
            return getResizedImage(bitmap, 720)
        }
        return null

    }

    private fun getResizedImage(image: Bitmap, maxSize: Int): Bitmap{

        var width = image.width
        var height = image.height

        if (width <= maxSize && height <= maxSize) return image

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1){

            width = maxSize
            height = (width / bitmapRatio).toInt()


        }else{
            height = maxSize
            width = (height / bitmapRatio).toInt()
        }

        return  Bitmap.createScaledBitmap(image,width,height,true)

    }

    private fun enableUI(enable: Boolean){

        positiveButton?.isEnabled = enable
        negativeButton?.isEnabled = enable
        binding?.let {
            with(it){

                etTittle.isEnabled = enable
                etDescription.isEnabled = enable
                etTopic.isEnabled = enable

            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

}