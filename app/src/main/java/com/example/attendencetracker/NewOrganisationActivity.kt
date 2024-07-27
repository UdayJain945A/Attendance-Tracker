package com.example.attendencetracker

import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.attendencetracker.databinding.ActivityNewOrganisationBinding
import com.example.attendencetracker.models.OrganisationsModel
import com.example.attendencetracker.utils.Constants

import com.example.attendencetracker.utils.database.DatabaseHandler

class NewOrganisationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewOrganisationBinding
    private lateinit var databaseHandler: DatabaseHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNewOrganisationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        databaseHandler = DatabaseHandler(this@NewOrganisationActivity)
        binding.newOrganisationSubmit.setOnClickListener { submitNewOrganisation() }
    }

    private fun submitNewOrganisation() {
        val nameValidity = Constants.getNameValidity(binding.editNewOrganisationName.text.toString())
        val targetValidity = Constants.getTargetValidity(binding.editNewOrganisationTarget.text.toString())

        if (nameValidity.containsKey(false)) {
            binding.editNewOrganisationName.error = nameValidity[false]
            return
        }
        if (targetValidity.containsKey(false)) {
            binding.editNewOrganisationTarget.error = targetValidity[false]
            return
        }
        val organisationName: String
        val organisationTarget = try {
            organisationName = nameValidity[true]!!
            targetValidity[true]?.toInt()!!
        } catch (e: NullPointerException) {
            return
        }
        val newOrganisation = OrganisationsModel(name = organisationName, target = organisationTarget)
        try {
            databaseHandler.createNewOrganisation(newOrganisation)
            insertOrganisation(newOrganisation)
        } catch (e: SQLiteException) {
            binding.editNewOrganisationName.error = "Organisation already exists"
            Toast.makeText(this, "Organisation already exists", Toast.LENGTH_SHORT).show()
        }
    }

    private fun insertOrganisation(newOrganisation: OrganisationsModel) {
        val values = ContentValues()
        values.put(Constants.NAME, newOrganisation.name)
        values.put(Constants.TARGET, newOrganisation.target)
        values.put(Constants.ATTENDANCE, newOrganisation.attendance)
        databaseHandler.insertOrganisation(values)
        Toast.makeText(this, "Added " + newOrganisation.name + " Successfully", Toast.LENGTH_LONG)
            .show()
        val intent = Intent(applicationContext, MainActivity::class.java)
        finish()
        startActivity(intent)
    }
}