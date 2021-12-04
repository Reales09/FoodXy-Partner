package com.example.foodxypartner.chat


import com.example.foodxypartner.data.Message

interface OnChatListener {

    fun deleteMessage(message: Message)

}