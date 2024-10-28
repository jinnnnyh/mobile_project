package com.example.intravel

import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.intravel.adapter.MainAdapter
import com.example.intravel.client.Client
import com.example.intravel.data.TravelData
import com.example.intravel.databinding.ActivityMainBinding
import com.example.intravel.databinding.CustomDdayBinding
import com.example.intravel.databinding.FragmentGalleryBinding
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
//    setContentView(R.layout.activity_main)
    val binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }


    // 오늘 날짜 시스템에서 받아오기
    // 메인에서 오늘 날짜 출력해야해서 이거 필요함!!!!!!!!!!!!
    //val format = "yyyy.MM.dd." // 포맷 설정
    var today = Date(System.currentTimeMillis()) // 현재 시스템 날짜
    var dateFormat = SimpleDateFormat("yyyy. MM. dd ") // 포맷 적용해서 저장

    // 날짜 포맷 맞춰서 받기
    val todayDate :String = dateFormat.format(today)

    // 메인에 오늘날짜 출력
    binding.titleToday.text = todayDate

    // 한글 요일 구하는 함수
    fun doDayOfWeek():String?{
      val cal: Calendar = Calendar.getInstance()
      var strWeek: String? = null
      val nWeek: Int = cal.get(Calendar.DAY_OF_WEEK)

      if (nWeek == 1) {
        strWeek = "일요일"
      } else if (nWeek == 2) {
        strWeek = "월요일"
      } else if (nWeek == 3) {
        strWeek = "화요일"
      } else if (nWeek == 4) {
        strWeek = "수요일"
      } else if (nWeek == 5) {
        strWeek = "목요일"
      } else if (nWeek == 6) {
        strWeek = "금요일"
      } else if (nWeek == 7) {
        strWeek = "토요일"
      }
      return strWeek
    }

    // 한글로 요일 받기
    val korDate:String? = doDayOfWeek()
    binding.titleDate.text = korDate

    // 데이터 생성
    var mainList = mutableListOf<TravelData>()
    var ingList = mutableListOf<TravelData>() // db 연결전 테스트용
    // db 연결 전에는 데이터 분리해서 넣고 사용함, 연결하고 나서는 상관 없을듯 ?

    // db 연결 전 테스트용 데이터 추가
    // + 버튼 누르면 값 디비에 추가 후 리스폰된 값 받아서 화면에 띄움
    // Y 진행중, N 완료됨
//    mainList.add(TravelData(0,"일본 여행",null,"20240914","20240920","동생",'Y'))
//    mainList.add(TravelData(0,"경주 여행",null,"20240903","20240920","친구",'Y'))
//    mainList.add(TravelData(0,"전주 여행",null,"20240925","20240930","동생",'N'))



    // 어댑터 생성
    var mainAdapter = MainAdapter(mainList)
    binding.recyclerViewMain.adapter = mainAdapter
    binding.recyclerViewMain.layoutManager = LinearLayoutManager(this)

    //  *첫화면* 진행중
    Client.retrofit.findComplete('N').enqueue(object:retrofit2.Callback<List<TravelData>>{
      override fun onResponse(call: Call<List<TravelData>>, response: Response<List<TravelData>>) {
        mainAdapter.mainList.clear() // 어댑터에 있는 데이터 지우고 채우기
        mainAdapter.mainList = response?.body() as MutableList<TravelData>
        mainAdapter.notifyDataSetChanged()
      }

      override fun onFailure(call: Call<List<TravelData>>, t: Throwable) {
        Log.d("findComplete","${t.message}")
      }
    })// findComplete


    // 전체보기
    // 데이터 리스트 불러와서 리사이클러뷰에 붙이기
    binding.btnList.setOnClickListener {

      // db 연결버전
      Client.retrofit.findAll().enqueue(object:retrofit2.Callback<List<TravelData>>{
        override fun onResponse(call: Call<List<TravelData>>, response: Response<List<TravelData>>) {
//          mainAdapter.mainList.clear()
          val mainItem = response.body() as MutableList<TravelData>
          val test1 = mainItem.sortedWith(compareBy({it.travComplete == 'Y'}, {it.travId}))

          mainAdapter.mainList = test1.toMutableList()
          mainAdapter.notifyDataSetChanged()
        }

        override fun onFailure(call: Call<List<TravelData>>, t: Throwable) {
          TODO("Not yet implemented")
        }
      })// findAll
    } // btnList


    // 진행중
    // cate 의 값이 N 인 것만 데이터로 보내기
    binding.btnIng.setOnClickListener {

        // db연결 버전
      Client.retrofit.findComplete('N').enqueue(object:retrofit2.Callback<List<TravelData>>{
        override fun onResponse(call: Call<List<TravelData>>, response: Response<List<TravelData>>) {
          mainAdapter.mainList.clear() // 어댑터에 있는 데이터 지우고 채우기
          mainAdapter.mainList = response.body() as MutableList<TravelData>
          mainAdapter.notifyDataSetChanged()
        }

        override fun onFailure(call: Call<List<TravelData>>, t: Throwable) {
          TODO("Not yet implemented")
        }
      })// findComplete

    }// btnIng


    // 완료
    // cate 의 값이 Y인 것만 불러오기
    binding.btnEnd.setOnClickListener {

      Client.retrofit.findComplete('Y').enqueue(object:retrofit2.Callback<List<TravelData>>{
        override fun onResponse(call: Call<List<TravelData>>, response: Response<List<TravelData>>) {
          mainAdapter.mainList.clear() // 어댑터에 있는 데이터 지우고 채우기
          mainAdapter.mainList = response.body() as MutableList<TravelData>
          mainAdapter.notifyDataSetChanged()
        }

        override fun onFailure(call: Call<List<TravelData>>, t: Throwable) {
          TODO("Not yet implemented")
        }
      })// findComplete

    } // btnEnd



    // 카테고리 데이터
    val cateList = listOf("---선택해주세요---","혼자","친구","가족","연인")
//    val cateData = resources.getStringArray(R.array.cateList) // 리소스파일 안쓰고 걍 배열 생성
    var cateSelected:String?=null

    // 추가하기 다이얼로그
    // 제목, 출발, 마감, 카테고리
    binding.btnAdd.setOnClickListener { 
      val dialogInsert = CustomDdayBinding.inflate(layoutInflater)

      // spinner 드롭다운 어댑터
      val cateAdapter = ArrayAdapter(this,android.R.layout.simple_list_item_1,cateList)
      dialogInsert.cateSpinner.adapter = cateAdapter

      AlertDialog.Builder(this).run{
        setTitle("디데이 추가하기")
        setMessage("날짜를 선택해주세요")
        setView(dialogInsert.root)
        // 날짜 선택해도 오늘 날짜만 활성화 > edt 선택 안하면 날짜 선택 불가
        dialogInsert.calendarView.setOnDateChangeListener{calendarView, year, month, date ->
          dialogInsert.calendarView.setDate(System.currentTimeMillis())
        }

        // 출발 날짜를 눌렀을 때 캘린더 활성화
        dialogInsert.edtStart.setOnClickListener {
          dialogInsert.calendarView.setOnDateChangeListener { calendarView, year, month, date ->
            var e_month = ""
            var e_date = ""

            // 한자리 수는 0붙여서 나오게
            if(month+1 < 10){
              e_month = "0${month+1}"

              if(date < 10){
                e_date = "0${date}"
              }
              else{
                e_date = date.toString()
              }

            }else if(month+1 >= 10){
              e_month = (month+1).toString()

              if(date < 10){
                e_date ="0${date}"
              }
              else{
                e_date =date.toString()
              }
            }

            dialogInsert.edtStart.setText("${year}${e_month}${e_date}")
          }
        }

        // 마감 날짜를 눌렀을 때 캘린더 활성화
        dialogInsert.edtEnd.setOnClickListener {
          dialogInsert.calendarView.setOnDateChangeListener { calendarView, year, month, date ->
            var e_month = ""
            var e_date = ""

            // 한자리 수는 0붙여서 나오게
            if(month+1 < 10){
              e_month = "0${month+1}"

              if(date < 10){
                e_date = "0${date}"
              }
              else{
                e_date = date.toString()
              }

            }else if(month+1 >= 10){
              e_month = (month+1).toString()

              if(date < 10){
                e_date ="0${date}"
              }
              else{
                e_date =date.toString()
              }
            }

            dialogInsert.edtEnd.setText("${year}${e_month}${e_date}")
          }
        }

        dialogInsert.cateSpinner.onItemSelectedListener = object:AdapterView.OnItemSelectedListener{
          override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
            // 선택값을 리스트의 문자열 말고 포지션 값으로 저장
            cateSelected = position.toString()
          }

          override fun onNothingSelected(p0: AdapterView<*>?) {
            TODO("Not yet implemented")
          }
        }

        setPositiveButton("확인",object:DialogInterface.OnClickListener{
          override fun onClick(p0: DialogInterface?, p1: Int) {
            // 디비 들어갈 데이터
            var d = (TravelData(0,
              dialogInsert.ddayName.text.toString(),
              null,
              dialogInsert.edtStart.text.toString(),
              dialogInsert.edtEnd.text.toString(),
              cateSelected,
               'N'))

//            mainAdapter.insertData(d)

            // db 연결 버전
            Client.retrofit.insert(d).enqueue(object:retrofit2.Callback<TravelData>{
              override fun onResponse(call: Call<TravelData>, response: Response<TravelData>) {
                response.body()?.let { it1 -> mainAdapter.insertData(it1) }
              }

              override fun onFailure(call: Call<TravelData>, t: Throwable) {
                TODO("Not yet implemented")
              }
            })//enqueue
          }//onClick

        })//positiveButon

        setNegativeButton("취소",null)
        show()
      }//dialog
    }// btnAdd

    // 인텐트로 서브창 넘어가게 하기 데이터 가지고 갈 필요 X

    mainAdapter.onItemClickListener = object:MainAdapter.OnItemClickListener{
      override fun onItemClick(data: TravelData, dday:String, position: Int) {

        var intent = Intent(this@MainActivity,DetailMainActivity::class.java)

        intent.putExtra("tId",data.travId) // 메모, 투두리스트 필요
        intent.putExtra("tTitle",data.travTitle) // 서브 상단
        intent.putExtra("dday",dday) // 서브 상단
        intent.putExtra("today",todayDate)
        intent.putExtra("tStartDate", data.startDate) // 메모 작성 시 필요(메모 작성 날짜 선택)
        intent.putExtra("tEndDate", data.endDate) // 메모 작성 시 필요(메모 작성 날짜 선택)
        intent.putExtra("travComplete",data.travComplete) // 완료 여부 전달

        startActivity(intent)
        overridePendingTransition(R.anim.rightin_activity,R.anim.not_move_activity)
      }

    }



  }// onCreate

}