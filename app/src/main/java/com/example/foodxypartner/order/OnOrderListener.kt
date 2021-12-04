package com.example.foodxypartner.order


import com.example.foodxypartner.data.Order

interface OnOrderListener {


    fun onStartChat(order: Order)
    fun OnStatusChange(order: Order)

}