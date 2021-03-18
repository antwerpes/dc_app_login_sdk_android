package com.doccheck.apploginsdk

import android.util.Log
import androidx.annotation.WorkerThread
import java.net.HttpURLConnection
import java.net.URL


class HTTPRequestManager {


    @WorkerThread
    fun get(requestUrl: String, success: (String) -> Unit, failure: (String, Int) -> Unit) {
        val url = URL(requestUrl)
        val connection = url.openConnection().apply {
            readTimeout = 30000
            connectTimeout = 60000
        }

        if (connection !is HttpURLConnection) {
            throw Exception("cant get an HttpURLConnection")
        }

        connection.requestMethod = "GET"
        try {
            connection.connect()
            val responseCode = connection.responseCode

            if(responseCode != HttpURLConnection.HTTP_OK) {
                if (responseCode != HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    failure("", responseCode)
                    return
                }

                val reader = connection.inputStream.bufferedReader()
                val response = reader.readText()
                reader.close()
                failure(response, responseCode)
                return
            }

            val reader = connection.inputStream.bufferedReader()
            val response = reader.readText()
            reader.close()
            success(response)

        } catch (e: Exception) {
            Log.e("HTTPRequestManager", "failure during doNetworkRequest", e)
        }
    }
}