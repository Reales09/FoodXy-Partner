package com.example.foodxypartner.fcm

import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.example.foodxypartner.FoodxyPartnerAplication
import com.example.foodxypartner.core.Constants
import org.json.JSONException
import org.json.JSONObject

class NotificationRS {

    fun sendNotificaion(title: String, message: String, tokens: String){

        val params = JSONObject()
        params.put(Constants.PARAM_METHOD, Constants.SEND_NOTIFICATION)
        params.put(Constants.PARAM_TITLE,title)
        params.put(Constants.PARAM_MESSAGE, message)
        params.put(Constants.PARAM_TOKENS,tokens)

        val jsonObjectRequest: JsonObjectRequest = object : JsonObjectRequest(Method.POST, Constants.FOODXY_PARTNER_RS,params,
            Response.Listener { response ->

                try {
                    val success = response.getInt(Constants.PARAM_SUCCESS)
                    Log.i("Volley success",success.toString())
                    Log.i("Response", response.toString())
                }catch (e:JSONException){
                    e.printStackTrace()
                    Log.e("Volley exception", e.localizedMessage)
                }

        },Response.ErrorListener { error ->
                if (error.localizedMessage != null){

                    Log.e("Volley error",error.localizedMessage)
                }

            }){



            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> {

                val paramsHeaders = HashMap<String, String>()
                paramsHeaders["Content-Type"]="application/json; charset=utf-8"
                return super.getHeaders()
            }
        }
        FoodxyPartnerAplication.volleyHelper.addToRequestQueue(jsonObjectRequest)
    }
}