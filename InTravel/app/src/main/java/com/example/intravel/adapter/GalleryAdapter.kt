package com.example.intravel.adapter

import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.onActive
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.intravel.PhotoFullActivity
import com.example.intravel.R
import com.example.intravel.client.Client
import com.example.intravel.data.PhotoData
import com.example.intravel.databinding.CustomPhotoDeleteBinding
import com.example.intravel.databinding.ItemGalleryBinding
import okhttp3.internal.notify
import retrofit2.Call
import retrofit2.Response
import kotlin.coroutines.coroutineContext

class GalleryAdapter(var photoList: MutableList<PhotoData>): RecyclerView.Adapter<GalleryAdapter.Holder>() {

//  private val REQUEST_DELETE_PHOTO = 100
  var position1 = 0
  var url: String = ""
  var fileName: String = ""

  fun removeData(position: Int){
    photoList.removeAt(position)
    notifyDataSetChanged()
  }


  inner class Holder(val binding: ItemGalleryBinding): RecyclerView.ViewHolder(binding.root) {
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryAdapter.Holder {
    return Holder(ItemGalleryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
  }

  override fun onBindViewHolder(holder: GalleryAdapter.Holder, position: Int) {
    val photo = photoList[position]

    url = "http://43.202.52.211:8811/photos/"
    fileName = photo.fileName

    Glide.with(holder.binding.root.context)
      .load(url + photo.fileName)
      .into(holder.binding.galleryIv)

    holder.binding.galleryIv.setOnLongClickListener {
      AlertDialog.Builder(it.context).run{
        setTitle("서버에 저장된 사진을 정말 삭제하시겠습니까?")
        setPositiveButton("삭제",object: DialogInterface.OnClickListener {
          override fun onClick(p0: DialogInterface?, p1: Int) {
            Client.photoRetrofit.deletePhoto(photo.photoId).enqueue(object:retrofit2.Callback<Void>{
              override fun onResponse(call: Call<Void>, response: Response<Void>) {
                removeData(holder.adapterPosition)
              }
              override fun onFailure(call: Call<Void>, t: Throwable) {
                TODO("Not yet implemented")
              }
            })
          }
        })
        setNegativeButton("취소",null)
        show()
      }
      true
    }
    holder.binding.galleryIv.setOnClickListener {
      val intent = Intent(holder.binding.root.context, PhotoFullActivity::class.java)
      intent.putExtra("url", url)
      intent.putExtra("fileName", photo.fileName)
      intent.putExtra("photoId", photo.photoId)
      intent.putExtra("position", position)
      position1 = position
      (holder.binding.root.context as Activity).startActivity(intent)
    }
  }

  override fun getItemCount(): Int {
    return photoList.size
  }
}