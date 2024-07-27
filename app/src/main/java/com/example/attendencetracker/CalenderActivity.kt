package com.example.attendencetracker

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.attendencetracker.databinding.ActivityCalenderBinding
import com.example.attendencetracker.dialogboxes.EditAttendanceDialog
import com.example.attendencetracker.models.ClassesModel
import  com.example.attendencetracker.utils.database.DatabaseHandler
import java.util.*


class CalendarActivity : AppCompatActivity(), EditAttendanceDialog.SubmitNewAttendance {

    companion object {
        @JvmStatic
        lateinit var model: ClassesModel
        @JvmStatic
        lateinit var organisationName: String
    }

    private lateinit var binding:ActivityCalenderBinding
    private lateinit var focusedDate: String
    private lateinit var presentCounter: TextView
    private lateinit var absentCounter: TextView
    private var present = 0
    private var absent = 0
    private lateinit var databaseHandler: DatabaseHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCalenderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        focusedDate = currentDate
        supportActionBar?.title = organisationName
        binding.classNameCalendar.text = model.name
        initializeDatabase()
        initializeViews()
        setAttendance()
        binding.editAttendance.setOnClickListener {
            editAttendance()
        }
    }

    private fun initializeDatabase() {
        databaseHandler = DatabaseHandler(this@CalendarActivity)
        val database = databaseHandler.readableDatabase
        val classId = model.id
        val getCommand = "SELECT * FROM \"$organisationName\" WHERE class_sno=$classId"
        val cursor = database.rawQuery(getCommand, null)
        cursor.moveToFirst()
        val history = cursor.getString(4)
        cursor.close()
        database.close()
        model.classHistory = history
    }

    private fun initializeViews() {
        val calendarView = binding.classCalendar
        calendarView.setOnDateChangeListener { _, year: Int, month: Int, dayOfMonth: Int ->
            focusedDate = getFormattedDate(year, month, dayOfMonth)
            setAttendance()
        }
        presentCounter = binding.presentCounter
        absentCounter = binding.absentCounter
    }

    private fun setAttendance() {
        present = model.getPresentCount(focusedDate)
        absent = model.getAbsentCount(focusedDate)
        presentCounter.text = present.toString()
        absentCounter.text = absent.toString()
    }

    private val currentDate: String
        get() {
            val calendar = Calendar.getInstance()
            return getFormattedDate(
                calendar[Calendar.YEAR],
                calendar[Calendar.MONTH],
                calendar[Calendar.DATE]
            )
        }

    private fun getFormattedDate(year: Int, month: Int, date: Int): String {
        val formattedDate = if (date >= 10) date.toString() else "0$date"
        val formattedMonth = if (month >= 10) month.toString() else "0$month"
        return year.toString() + formattedMonth + formattedDate
    }

    private fun editAttendance() {
        EditAttendanceDialog.presentCount = present
        EditAttendanceDialog.absentCount = absent
        val attendanceDialog = EditAttendanceDialog()
        attendanceDialog.show(supportFragmentManager, "edit attendance")
    }

    override fun updateAttendance(newPresent: Int, newAbsent: Int) {
        val attendance = IntArray(2)
        attendance[0] = newPresent - present
        attendance[1] = newAbsent - absent
        databaseHandler.markAttendance(organisationName, model, focusedDate, attendance)
        databaseHandler.refreshOverAttendance(organisationName)
        setAttendance()
        val toast = Toast.makeText(this, R.string.updated, Toast.LENGTH_LONG)
        toast.setGravity(Gravity.CENTER, 0, 20)
        toast.show()
    }

    //==========================================================================================================//
    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, OrganisationActivity::class.java))
    }
}