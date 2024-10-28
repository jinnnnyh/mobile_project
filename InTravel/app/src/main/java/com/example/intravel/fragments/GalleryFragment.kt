package com.example.intravel.fragments

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.intravel.adapter.GalleryAdapter
import com.example.intravel.client.Client
import com.example.intravel.data.PhotoData
import com.example.intravel.databinding.FragmentGalleryBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.OutputStream

class GalleryFragment : Fragment() {

  lateinit var binding: FragmentGalleryBinding
  lateinit var galleryAdapter: GalleryAdapter
  var tId: Long = 0

  private val requestCameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
    if (success) {
      photoUri?.let {
        photoSaveDialog(it)
      }
    }
  }

  private val requestGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    uri?.let {
      galleryPhotoSaveDialog(it)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    binding = FragmentGalleryBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }

    tId = activity?.intent?.getLongExtra("tId", 0) ?: 0

    val photoList = mutableListOf<PhotoData>()
    galleryAdapter = GalleryAdapter(photoList)
    binding.galleryRV.adapter = galleryAdapter
    binding.galleryRV.layoutManager = GridLayoutManager(requireContext(), 3)

//    사진 로딩
    loadPhotoList()

//     카메라 버튼 클릭
    binding.btnTakePhoto.setOnClickListener {
      takePhotoIntent()
    }

//    갤러리 버튼 클릭
    binding.btnPhoneGallery.setOnClickListener {
      selectPhotoFromGallery()
    }
  } // viewCreate

  private fun selectPhotoFromGallery() {
    requestGalleryLauncher.launch("image/*")
  }

  private fun takePhotoIntent() {
    Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
      photoUri = createPhotoUri()
      photoUri?.let { uri -> requestCameraLauncher.launch(uri) }
    }
  }

  private var photoUri: Uri? = null

  private fun createPhotoUri(): Uri? {
    val contentValues = ContentValues().apply {
      put(MediaStore.Images.Media.DISPLAY_NAME, "photo_${System.currentTimeMillis()}.jpg")
      put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    }
    return requireActivity().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
  }

  private fun galleryPhotoSaveDialog(photoUri: Uri) {
    val builder = AlertDialog.Builder(requireContext())
    builder.setTitle("갤러리 사진 업로드")
    builder.setMessage("선택한 사진을 저장하시겠습니까?")
    builder.setPositiveButton("확인") { dialog, _ ->
      uploadPhoto(photoUri)
      dialog.dismiss()
    }
    builder.setNegativeButton("취소") { dialog, _ ->
      dialog.dismiss()
    }
    builder.create().show()
  }

  private fun photoSaveDialog(photoUri: Uri) {
    val builder = AlertDialog.Builder(requireContext())

    builder.setTitle("촬영한 사진 업로드")
    builder.setMessage("촬영한 사진을 저장하시겠습니까?")
    builder.setPositiveButton("확인") { dialog, _ ->
      uploadPhoto(photoUri)
      dialog.dismiss()
    }
    builder.setNegativeButton("취소") { dialog, _ ->
      dialog.dismiss()
    }
    builder.create().show()
  }

  private fun uploadPhoto(photoUri: Uri) {

    val inputStream = context!!.contentResolver.openInputStream(photoUri)
    val requestFile = inputStream?.readBytes()?.toRequestBody("image/jpeg".toMediaTypeOrNull())

    Log.d("photoUri","${photoUri}")

    if (requestFile != null) {
      val body = MultipartBody.Part.createFormData("photo", "photo.jpg", requestFile)

      val call = Client.photoRetrofit.savePhoto(tId, body)
      call.enqueue(object : Callback<PhotoData> {
        override fun onResponse(call: Call<PhotoData>, response: Response<PhotoData>) {
          if (response.isSuccessful) {
            response.body()?.let { newPhoto ->
              galleryAdapter.photoList.add(newPhoto)
              galleryAdapter.notifyDataSetChanged()
            }
          } else {
            // 실패 처리
          }
        }

        override fun onFailure(call: Call<PhotoData>, t: Throwable) {
          Log.d("uploadPhoto", "${t.message}")
        }
      })
    } else {
      // InputStream을 열 수 없을 때 처리
    }

  }

  private fun loadPhotoList() {
    val call = Client.photoRetrofit.findPhotoList(tId)
    call.enqueue(object : Callback<List<PhotoData>> {
      override fun onResponse(call: Call<List<PhotoData>>, response: Response<List<PhotoData>>) {
        if (response.isSuccessful) {
          response.body()?.let {
            galleryAdapter.photoList.clear()
            galleryAdapter.photoList.addAll(it)
            galleryAdapter.notifyDataSetChanged()
          }
        }
        else {
          // 실패 처리
        }
      }

      override fun onFailure(call: Call<List<PhotoData>>, t: Throwable) {
        // 에러 처리
      }
    })
  }
}