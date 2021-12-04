package com.example.foodxypartner.product

import com.example.foodxypartner.data.Product

interface OnProductListener {

    fun onClick(product: Product)
    fun onLongClick(product: Product)
}