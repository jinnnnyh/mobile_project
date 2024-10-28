package com.example.intravel

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.VectorDrawable
import android.location.Geocoder
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.intravel.client.SubClient
import com.example.intravel.data.Maps
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.example.intravel.databinding.ActivityMapsBinding
import com.example.intravel.databinding.CustomMapBinding
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import retrofit2.Call
import retrofit2.Response
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var mapItemForFunc: Maps

    companion object {
        const val TAG = "MapActivity"
        const val REQUEST_LOCATION_PERMISSION = 1
    }

    lateinit var binding: ActivityMapsBinding
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentMarker: Marker? = null

    private val markersMap = mutableMapOf<Marker, Maps>() // 마커와 데이터 매핑

    private lateinit var searchEdit: EditText
    private lateinit var searchButton: Button
    private lateinit var addressTextView: TextView
    private lateinit var pinNameTextView: TextView
    private lateinit var btnPinAdd: Button
    private lateinit var btnPinUpdate: Button
    private lateinit var btnPinRemove: Button

    private val apiKey = System.getenv("GOOGLE_MAPS_API_KEY")
    private var tId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Google Places API 초기화
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "${apiKey}")
        }

        // PlacesClient 생성
        val placesClient = Places.createClient(this)

        // View 바인딩 초기화
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 검색창 버튼 초기화
        searchEdit = findViewById(R.id.searchEdit)
        searchButton = findViewById(R.id.searchButton)

        // 주소를 표시할 TextView 초기화
        addressTextView = findViewById(R.id.addressTextView)
//        addButton = findViewById(R.id.addButton)
        addressTextView.visibility = View.GONE
        // pinName을 표시할 TextView 초기화
        pinNameTextView = findViewById(R.id.pinNameTextView)
        pinNameTextView.visibility = View.GONE

        // 추가, 수정, 삭제 버튼 초기화
        btnPinAdd = findViewById(R.id.btnPinAdd)
        btnPinAdd.visibility = View.GONE
        btnPinUpdate = findViewById(R.id.btnPinUpdate)
        btnPinUpdate.visibility = View.GONE
        btnPinRemove = findViewById(R.id.btnPinRemove)
        btnPinRemove.visibility = View.GONE

        // MapView 초기화 및 생성
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)

        // FusedLocationProviderClient 초기화 (현재 위치 정보 제공)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mapView.getMapAsync(this)

        searchButton.setOnClickListener {
            val searchQuery = searchEdit.text.toString()
            if (searchQuery.isNotEmpty()) {
//                searchLocationByAddress(searchQuery)
                searchPlaceByName(searchQuery)
            } else {
                Toast.makeText(this, "주소를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }

        // Intent로부터 tId를 추출
        tId = intent.getLongExtra("tId", 0)

        binding.btnBack.setOnClickListener {
            finish()
        }

        // 핀 위치 지정 후 추가 버튼 클릭 시 이벤트 처리
        binding.btnPinAdd.setOnClickListener {
            val currentLatLng = currentMarker?.position ?: return@setOnClickListener    // 현재 클릭한 위치 가져오기

            // 다이얼로그 생성
            val addDialog = CustomMapBinding.inflate(layoutInflater)

            AlertDialog.Builder(this).run {
                setTitle("해당 위치 저장")
                setView(addDialog.root)
                setPositiveButton("확인", object : DialogInterface.OnClickListener {
                    override fun onClick(p0: DialogInterface?, p1: Int) {
                        val edtPinName = addDialog.edtPinName.text.toString()
                        val latitude = currentLatLng.latitude.toString()
                        val longitude = currentLatLng.longitude.toString()

                        // 사용자가 선택한 색상 가져오기
                        val selectedColor = addDialog.spinnerPinColor.selectedItem.toString()

                        // 핀 정보 데이터베이스 저장
                        val mapItem = Maps(0, tId, latitude, longitude, edtPinName, selectedColor)

                        SubClient.retrofit.insertMap(tId, mapItem).enqueue(object : retrofit2.Callback<Maps> {
                            override fun onResponse(call: Call<Maps>, response: Response<Maps>) {
                                // 지도에 핀 추가
                                val latLng = LatLng(latitude.toDouble(), longitude.toDouble())
                                val markerOptions = MarkerOptions()
                                    .position(latLng)
                                    .icon(getMarkerIconByColor(selectedColor)) // 색상에 따른 아이콘 설정
                                val marker = googleMap.addMarker(markerOptions)

                                markersMap[marker] = mapItem

                                // 핀이 추가된 위치로 카메라 이동
                                googleMap.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        latLng,
                                        15f
                                    )
                                )

                                // 추가 후 바로 수정시 해당 mapId 서버에서 받아야 함
                                val newMapItem = response.body()
                                val mapId = newMapItem?.mapId ?: 0

                                if (mapId != 0.toLong()) {
                                    // 성공적으로 서버에서 mapId를 받음
                                    mapItem.mapId = mapId

                                    // 이제 수정 작업을 진행 가능
                                    updateData()
                                }
                                
                                addressTextView.visibility = View.GONE
                                pinNameTextView.visibility = View.GONE
                                btnPinAdd.visibility = View.GONE
                            }
                            override fun onFailure(call: Call<Maps>, t: Throwable) {
                            }
                        })  //insertMap
                    }
                })
                .setNegativeButton("취소", null)
                .show()
            }   //AlertDialog
        }   //btnPinAdd
    }

    // 수정 버튼 클릭 시 저장된 내용(pinName, pinColor) 수정
    private fun updateData() {
        binding.btnPinUpdate.setOnClickListener {
            val updateDialog = CustomMapBinding.inflate(layoutInflater)
            updateDialog.edtPinName.setText(mapItemForFunc.pinName) // 기존 데이터 표시

            // Spinner에서 기존 색상을 선택하도록 설정
            val pinColors = resources.getStringArray(R.array.pin_colors)
            val colorIndex = pinColors.indexOf(mapItemForFunc.pinColor)
            if (colorIndex != -1) {
                updateDialog.spinnerPinColor.setSelection(colorIndex)
            }

            AlertDialog.Builder(this).run {
                setView(updateDialog.root)
                setPositiveButton("수정", object :DialogInterface.OnClickListener {
                    override fun onClick(p0: DialogInterface?, p1: Int) {
                        val updatePinName = updateDialog.edtPinName.text.toString()
                        val latitude = mapItemForFunc.latitude
                        val longitude = mapItemForFunc.longitude
                        val updatedColor = updateDialog.spinnerPinColor.selectedItem.toString()

                        val updatedMapItem = Maps(
                            mapItemForFunc.mapId,
                            mapItemForFunc.travId,
                            latitude,
                            longitude,
                            updatePinName,
                            updatedColor // 색상 정보 추가
                        )

                        SubClient.retrofit.updateMap(mapItemForFunc.mapId, updatedMapItem).enqueue(object : retrofit2.Callback<Maps> {
                            override fun onResponse(call: Call<Maps>, response: Response<Maps>) {
                                // UI에 수정된 핀 이름 및 색상 반영
                                binding.pinNameTextView.text = updatedMapItem.pinName
                                mapItemForFunc.pinName = updatePinName
                                mapItemForFunc.pinColor = updatedColor

                                // 기존 마커 제거 후, 새로운 색상으로 마커 다시 추가
                                val marker = markersMap.entries.find { it.value.mapId == mapItemForFunc.mapId }?.key
                                marker?.remove()

                                val newMarkerOptions = MarkerOptions()
                                    .position(LatLng(latitude.toDouble(), longitude.toDouble()))
                                    .icon(getMarkerIconByColor(updatedColor)) // 수정된 색상 적용
                                val newMarker = googleMap.addMarker(newMarkerOptions)

                                // 마커와 데이터를 다시 매핑
                                markersMap[newMarker] = updatedMapItem
                                Log.d("updateMapItem", "$updatedMapItem")

                                // 기존 데이터는 제거
                                markersMap.entries.removeIf { it.value.mapId == mapItemForFunc.mapId && it.key != newMarker }
                            }
                            override fun onFailure(call: Call<Maps>, t: Throwable) {}
                        })
                    }
                })
                setNegativeButton("취소", null)
                show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        checkLocationPermission()

        // 지도 클릭 리스너 추가
        googleMap.setOnMapClickListener { latLng ->
            // 기존 핀 제거
            clearMarkers()
            // 새 핀 추가
            currentMarker = setupMarker(LatLngEntity(latLng.latitude, latLng.longitude))

            // 주소 표시
            displayAddress(latLng)

            // 하단에 주소 및 추가버튼 생김
            addressTextView.visibility = View.VISIBLE
            pinNameTextView.visibility = View.GONE
            updateConstraintsForPinName(true)
            btnPinAdd.visibility = View.VISIBLE
            btnPinUpdate.visibility = View.GONE
            btnPinRemove.visibility = View.GONE
        }

        // 모든 마커 불러오기
        SubClient.retrofit.findAllMap(tId).enqueue(object : retrofit2.Callback<List<Maps>> {
            override fun onResponse(call: Call<List<Maps>>, response: Response<List<Maps>>) {
                response.body()?.let { markers ->
                    if (this@MapsActivity::googleMap.isInitialized) {
                        // LatLngBounds.Builder 초기화
                        val builder = LatLngBounds.Builder()
                        Log.d("allData", "${response.body()}")

                        if (markers.isEmpty()) {
                            Log.e("Error", "No markers found.")
                            return // 더 이상 진행하지 않음
                        }

                        // 지도에 모든 마커 추가
                        for (marker in markers) {
                            val latLng =
                                LatLng(marker.latitude.toDouble(), marker.longitude.toDouble())
                            val markerOptions = MarkerOptions()
                                .position(latLng)
                                .icon(getMarkerIconByColor(marker.pinColor))

                            val googleMapMarker = googleMap.addMarker(markerOptions)

                            markersMap[googleMapMarker] = marker

                            // LatLngBounds에 마커 좌표 추가
                            builder.include(latLng)
                        }

                        // LatLngBounds 빌드
                        val bounds = builder.build()
                        // 지도에서 마커들이 모두 보이도록 카메라 조정
                        val cameraUpdate =
                            CameraUpdateFactory.newLatLngBounds(bounds, 100) // 100은 경계에 여유를 주기 위한 값
                        googleMap.moveCamera(cameraUpdate)
                    }
                }
            }
            override fun onFailure(call: Call<List<Maps>>, t: Throwable) {
            }
        })  //findAllMap

        // 저장된 핀 선택 시 저장된 정보 노출
        googleMap.setOnMarkerClickListener { marker ->
            val mapItem = markersMap[marker]

            // mapItem이 null인 경우 처리 (아무 동작하지 않음)
            if (mapItem == null) {
                return@setOnMarkerClickListener true // true를 반환하여 기본 동작을 막음
            }

            mapItemForFunc = mapItem

            binding.addressTextView.text = getAddressFromLatLng(marker.position)
            binding.pinNameTextView.setText(mapItem.pinName)

            // 버튼의 가시성 설정
            if (mapItemForFunc.pinName == null) {
                pinNameTextView.visibility = View.GONE
            }
            else {
                addressTextView.visibility = View.VISIBLE
                pinNameTextView.visibility = View.VISIBLE

                updateConstraintsForPinName(false)
                btnPinAdd.visibility = View.GONE
                btnPinUpdate.visibility = View.VISIBLE
                btnPinRemove.visibility = View.VISIBLE
            }
            true
        }   //setOnMarkerClickListener

        // 수정 버튼 클릭 시 저장된 내용(pinName, pinColor) 수정
        updateData()

        // 삭제 버튼 클릭 시 다이얼로그 창 열림
        binding.btnPinRemove.setOnClickListener {
            AlertDialog.Builder(this).run {
                setTitle("삭제하시겠습니까?")
                setPositiveButton("삭제", object : DialogInterface.OnClickListener {
                    override fun onClick(p0: DialogInterface?, p1: Int) {
                        SubClient.retrofit.deleteByIdMap(mapItemForFunc.mapId).enqueue(object : retrofit2.Callback<Void> {
                                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                    // 화면에서 핀 삭제
                                    val marker = markersMap.entries.find { it.value.mapId == mapItemForFunc.mapId }?.key
                                    marker?.remove()

                                    // 맵에서 마커와 관련된 데이터 제거
                                    markersMap.entries.removeIf { it.value.mapId == mapItemForFunc.mapId }

                                    addressTextView.visibility = View.GONE
                                    pinNameTextView.visibility = View.GONE
                                    btnPinUpdate.visibility = View.GONE
                                    btnPinRemove.visibility = View.GONE
                                }
                                override fun onFailure(call: Call<Void>, t: Throwable) {
                                }
                            })
                    }
                })
                setNegativeButton("취소", null)
                show()
            }   //AlertDialog
        }   //btnPinRemove
    }

    private fun searchPlaceByName(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        val placesClient = Places.createClient(this)

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            val prediction = response.autocompletePredictions.firstOrNull()
            if (prediction != null) {
                val placeId = prediction.placeId

                // 장소 ID를 사용하여 세부 정보를 가져옵니다.
                val placeRequest =
                    FetchPlaceRequest.builder(placeId, listOf(Place.Field.LAT_LNG)).build()
                placesClient.fetchPlace(placeRequest).addOnSuccessListener { placeResponse ->
                    val latLng = placeResponse.place.latLng
                    if (latLng != null) {
                        // 검색된 장소로 카메라 이동
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                        // 마커 추가
                        clearMarkers()
                        currentMarker = setupMarker(LatLngEntity(latLng.latitude, latLng.longitude))
                        currentMarker?.showInfoWindow()

                        // 주소 표시
                        displayAddress(latLng)

                        addressTextView.visibility = View.VISIBLE
                        btnPinAdd.visibility = View.VISIBLE
                    }
                }
            } else {
                Toast.makeText(this, "해당 장소를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "장소 검색 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                Log.d(TAG, "Current Location: $currentLatLng")

                // 카메라를 현재 위치로 이동
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                // 마커 추가
                clearMarkers()
                currentMarker = setupMarker(LatLngEntity(it.latitude, it.longitude))
                currentMarker?.showInfoWindow()

                // 주소 표시
                displayAddress(currentLatLng)
            } ?: run {
                Log.e(TAG, "Location is null")
            }
        }.addOnFailureListener {
            Log.e(TAG, "Failed to get location", it)
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
        } else {
            updateCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateCurrentLocation()
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 마커 아이콘에 사용할 BitmapDescriptor 생성 함수
    private fun getResizedBitmapDescriptor(resId: Int, width: Int, height: Int): BitmapDescriptor {
        // 리소스로부터 Drawable을 가져옴
        val drawable = ContextCompat.getDrawable(this, resId)
        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

        // 원본 크기의 Bitmap을 생성
        val bitmap = Bitmap.createBitmap(drawable!!.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)

        // 원하는 크기로 Bitmap을 조정
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)

        // Bitmap을 BitmapDescriptor로 변환하여 반환
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap)
    }

    private fun getMarkerIconByColor(color: String): BitmapDescriptor {
        return when (color) {
            "Red" -> getResizedBitmapDescriptor(R.drawable.dot_red, 100, 100)
            "Orange" -> getResizedBitmapDescriptor(R.drawable.dot_orange, 100, 100)
            "Yellow" -> getResizedBitmapDescriptor(R.drawable.dot_yellow, 100, 100)
            "Green" -> getResizedBitmapDescriptor(R.drawable.dot_green, 100, 100)
            "Cyan" -> getResizedBitmapDescriptor(R.drawable.dot_cyan, 100, 100)
            "Blue" -> getResizedBitmapDescriptor(R.drawable.dot_blue, 100, 100)
            "Purple" -> getResizedBitmapDescriptor(R.drawable.dot_purple, 100, 100)
            "Magenta" -> getResizedBitmapDescriptor(R.drawable.dot_magenta, 100, 100)
            else -> getResizedBitmapDescriptor(R.drawable.dot_yellow, 100, 100) // 기본 값
        }
    }

    // 마커 위치, 모양
    private fun setupMarker(locationLatLngEntity: LatLngEntity): Marker? {
        val positionLatLng = LatLng(locationLatLngEntity.latitude ?: return null, locationLatLngEntity.longitude ?: return null)

        // 커스텀 마커 아이콘 적용
        val markerOption = MarkerOptions().apply {
            position(positionLatLng)
            // dot_yellow.png 리소스를 마커 아이콘으로 설정
            icon(getResizedBitmapDescriptor(R.drawable.dot_yellow, 100, 100))
        }

        return googleMap.addMarker(markerOption)
    }

    // 위도 경도에 따른 주소 받아오기
    private fun getAddressFromLatLng(latLng: LatLng): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                addresses[0].getAddressLine(0)
            } else {
                "주소를 찾을 수 없습니다."
            }
        } catch (e: Exception) {
            "주소를 가져오는 중 오류가 발생했습니다."
        }
    }

    // 추가, 수정, 삭제 버튼
    fun updateConstraintsForPinName(isAddButtonVisible: Boolean) {
        val constraintLayout = findViewById<ConstraintLayout>(R.id.constraintLayout)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout) // 현재 제약 조건을 복제합니다.

        if (isAddButtonVisible) {
            // 추가 버튼이 보이는 경우, pinNameTextView의 제약 조건을 btnPinAdd 위로 설정
            constraintSet.connect(R.id.pinNameTextView, ConstraintSet.BOTTOM, R.id.btnPinAdd, ConstraintSet.TOP)
        } else {
            // 삭제 및 수정 버튼이 보이는 경우, pinNameTextView의 제약 조건을 btnPinUpdate 위로 설정
            constraintSet.connect(R.id.pinNameTextView, ConstraintSet.BOTTOM, R.id.btnPinUpdate, ConstraintSet.TOP)
        }

        // 변경된 제약 조건 적용
        constraintSet.applyTo(constraintLayout)
    }

    private fun displayAddress(latLng: LatLng) {
        val address = getAddressFromLatLng(latLng)
        addressTextView.text = address
    }

    private fun clearMarkers() {
        currentMarker?.remove()
        currentMarker = null
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    data class LatLngEntity(
        var latitude: Double?,
        var longitude: Double?
    )
}
