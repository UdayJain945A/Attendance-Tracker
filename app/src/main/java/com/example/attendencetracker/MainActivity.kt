package com.example.attendencetracker

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendencetracker.adapters.OrganisationsAdapter
import com.example.attendencetracker.databinding.ActivityMainBinding
import com.example.attendencetracker.dialogboxes.EditDialogBox
import com.example.attendencetracker.models.OrganisationsModel
import com.example.attendencetracker.utils.Constants
import com.example.attendencetracker.utils.database.DatabaseHandler
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), EditDialogBox.EditDialogListener, OrganisationsAdapter.OnItemClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var databaseHandler: DatabaseHandler
    private lateinit var organisationsAdapter: OrganisationsAdapter
    private lateinit var organisationList: ArrayList<OrganisationsModel>
    private var focusedOrganisation: OrganisationsModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.empty.visibility = View.GONE
        organisationList = ArrayList()
        databaseHandler = DatabaseHandler(this)
        setOrganisationAdapter()
        getOrganisationList()
        binding.newOrganisationButton.setOnClickListener { addOrganisation() }
        Constants.OPEN_ORG = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_backup_restore -> startActivity(Intent(this, SignInActivity::class.java))

            R.id.menu_share -> {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, Constants.APP_URL)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(shareIntent, "Share via"))
            }

            R.id.menu_rate_us -> {
                val uri = Uri.parse(Constants.APP_URL)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }

            R.id.more_apps -> {
                val uri = Uri.parse(Constants.DEV_URL)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }

            R.id.about -> {
                val uri = Uri.parse(Constants.ABOUT_URL)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }

            R.id.privacy_policy -> {
                val uri = Uri.parse(Constants.PRIVACY_POLICY)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }

            R.id.terms -> {
                val uri = Uri.parse(Constants.TERMS)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }
        return true
    }

    private fun setOrganisationAdapter() {
        organisationsAdapter = OrganisationsAdapter(organisationList, this, this)
        binding.organisationsContainer.apply {
            adapter = organisationsAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun getOrganisationList() {
        val organisationsRef = databaseHandler.readableDatabase
        val getCommand = "SELECT * FROM ${Constants.ORGANISATIONS}"
        val cursor = organisationsRef.rawQuery(getCommand, null)
        if (cursor.moveToFirst()) {
            do {
                val organisation = OrganisationsModel().apply {
                    id = cursor.getInt(0)
                    name = cursor.getString(1)
                    attendance = cursor.getInt(2)
                    target = cursor.getInt(3)
                }
                organisationList.add(organisation)
                organisationsAdapter.notifyItemInserted(organisationsAdapter.itemCount - 1)
            } while (cursor.moveToNext())
        }
        checkForEmpty()
        cursor.close()
        organisationsRef.close()
    }

    private fun openOrganisation() {
        val intent = Intent(this, OrganisationActivity::class.java)
        startActivity(intent)
    }

    private fun editOrganisation() {
        focusedOrganisation?.let {
            val editDialogBox = EditDialogBox(this)
            editDialogBox.show(supportFragmentManager, "edit dialog")
            EditDialogBox.name = it.name
            EditDialogBox.target = it.target
        }
    }

    private fun deleteOrganisation(position: Int) {
        val organisation = organisationList[position]
        AlertDialog.Builder(this)
            .setMessage("Are you sure you want to delete ${organisation.name}?")
            .setPositiveButton("Yes") { _, _ ->
                databaseHandler.deleteOrganisation(organisation.id.toString(), organisation.name)
                Toast.makeText(this, "Deleted ${organisation.name}", Toast.LENGTH_LONG).show()
                organisationList.removeAt(position)
                organisationsAdapter.notifyItemRemoved(position)
                checkForEmpty()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun addOrganisation() {
        val intent = Intent(this, NewOrganisationActivity::class.java)
        startActivity(intent)
    }

    private fun checkForEmpty() {
        binding.empty.visibility = if (organisationList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun submitDetails(newNameText: EditText, newTargetText: EditText) {
        val nameValidity = Constants.getNameValidity(newNameText.text.toString())
        val targetValidity = Constants.getTargetValidity(newTargetText.text.toString())

        if (nameValidity.containsKey(false)) {
            Toast.makeText(this, nameValidity[false], Toast.LENGTH_SHORT).show()
            return
        }
        if (targetValidity.containsKey(false)) {
            Toast.makeText(this, targetValidity[false], Toast.LENGTH_SHORT).show()
            return
        }
        val organisationName: String
        val organisationTarget: Int
        try {
            organisationName = nameValidity[true] ?: return
            organisationTarget = targetValidity[true]?.toInt() ?: return
        } catch (e: NullPointerException) {
            return
        }
        val values = ContentValues().apply {
            put(Constants.NAME, organisationName)
            put(Constants.TARGET, organisationTarget)
        }
        databaseHandler.updateOrganisation(values, focusedOrganisation?.name ?: "")
        if (organisationName != focusedOrganisation?.name) {
            databaseHandler.renameOrganisationTable(focusedOrganisation?.name ?: "", organisationName)
        }
        focusedOrganisation?.apply {
            name = organisationName
            target = organisationTarget
        }
        organisationsAdapter.notifyItemChanged(organisationList.indexOf(focusedOrganisation))
        Toast.makeText(this, "Updated Successfully", Toast.LENGTH_LONG).show()
        focusedOrganisation = null
    }

    override fun onItemClick(position: Int) {
        Constants.OPEN_ORG = organisationList[position].id.toString()
        openOrganisation()
    }

    override fun onEditClick(position: Int) {
        focusedOrganisation = organisationList[position]
        editOrganisation()
    }

    override fun onDeleteClick(position: Int) {
        deleteOrganisation(position)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
        exitProcess(0)
    }
}
