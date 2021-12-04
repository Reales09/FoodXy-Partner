package com.example.foodxypartner.product

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.foodxypartner.data.Product
import com.example.foodxypartner.R
import com.example.foodxypartner.databinding.ItemProductBinding

class ProductAdapter (val productList: MutableList<Product>,
                      private val listener: OnProductListener
) :
    RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_product,parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]

        holder.setListener(product)

        holder.binding.tvName.text = product.name
        holder.binding.tvPrice.text = product.price.toString()
        holder.binding.tvQuantity.text = product.quantity.toString()

        Glide.with(context)
            .load(product.imgUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_access_time)
            .error(R.drawable.ic_broken_image)
            .centerCrop()
            .into(holder.binding.imgProduct)

    }

    override fun getItemCount(): Int = productList.size

    fun add(product: Product){
        if (!productList.contains(product)){
            productList.add(product)
            notifyItemInserted(productList.size - 1)
        }else{

            update(product)
        }
    }

    fun update(product: Product){
        val index = productList.indexOf(product)
        if (index != -1){
            productList.set(index, product)
            notifyItemChanged(index)
        }
    }

    fun delete(product: Product){
        val index = productList.indexOf(product)
        if (index != -1){
            productList.removeAt(index)
            notifyItemRemoved(index)
        }
    }



    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view){

        val  binding = ItemProductBinding.bind(view)

        fun setListener(product: Product){
            binding.root.setOnClickListener{
                listener.onClick(product)
            }
            binding.root.setOnLongClickListener {
                listener.onLongClick(product)
                true
            }

        }

    }

}