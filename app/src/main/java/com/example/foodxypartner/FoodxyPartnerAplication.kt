package com.example.foodxypartner

import android.app.Application
import com.example.foodxypartner.fcm.VolleyHelper

class FoodxyPartnerAplication: Application() {

    companion object{
        lateinit var volleyHelper: VolleyHelper
    }

    override fun onCreate() {
        super.onCreate()

        volleyHelper = VolleyHelper.getInstance(this)
    }

}