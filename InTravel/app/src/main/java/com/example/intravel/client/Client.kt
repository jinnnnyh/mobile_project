package com.example.intravel.client

import com.example.intravel.interfaces.PhotoInterface
import com.example.intravel.interfaces.TravelInterface
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Client {
  val retrofit: TravelInterface = Retrofit.Builder()
    .baseUrl("http://10.100.105.205:8811/travel/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(TravelInterface::class.java)

  val photoRetrofit: PhotoInterface = Retrofit.Builder()
    .baseUrl("http://10.100.105.205:8811/photo/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(PhotoInterface::class.java)
}