package com.example.fileexplorer.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fileexplorer.R
import com.example.fileexplorer.backstack.BackStackManager
import com.example.fileexplorer.bgtask.FileChangeBroadcastReceiver
import com.example.fileexplorer.bgtask.FileIntentService
import com.example.fileexplorer.constants.FileType
import com.example.fileexplorer.model.FileModel
import com.example.fileexplorer.utils.createNewFile
import com.example.fileexplorer.utils.createNewFolder
import com.example.fileexplorer.utils.deleteFile_
import com.example.fileexplorer.utils.launchFileIntent
import com.example.fileexplorer.view.adapter.BreadcrumbRecyclerAdapter
import com.example.fileexplorer.view.dialog.FileOptionsDialog
import com.example.fileexplorer.view.fragments.FilesListFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_enter_name.view.*

class MainActivity : AppCompatActivity(), FilesListFragment.OnItemClickListener  {

    private val backStackManager = BackStackManager()
    private lateinit var mBreadcrumbRecyclerAdapter: BreadcrumbRecyclerAdapter
    private var isCopyModeActive: Boolean = false
    private var selectedFileModel: FileModel? = null

    companion object {
        private const val OPTIONS_DIALOG_TAG: String = "com.example.fileexplorer.view.options_dialog"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility.or(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            val filesListFragment = FilesListFragment.build {
                path = Environment.getExternalStorageDirectory().absolutePath
            }

            supportFragmentManager.beginTransaction()
                .add(R.id.container, filesListFragment)
                .addToBackStack(Environment.getExternalStorageDirectory().absolutePath)
                .commit()
        }
        requestPermissions()
        initViews()
        initBackStack()
    }

    private fun initViews() {
        setSupportActionBar(toolbar)

        breadcrumbRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mBreadcrumbRecyclerAdapter = BreadcrumbRecyclerAdapter()
        breadcrumbRecyclerView.adapter = mBreadcrumbRecyclerAdapter
        mBreadcrumbRecyclerAdapter.onItemClickListener = {
            supportFragmentManager.popBackStack(it.path, 2);
            backStackManager.popFromStackTill(it)
        }
    }

    private fun initBackStack() {
        backStackManager.onStackChangeListener = {
            updateAdapterData(it)
        }
        backStackManager.addToStack(fileModel = FileModel(Environment.getExternalStorageDirectory().absolutePath, FileType.FOLDER, "/", 0.0))
    }

    private fun updateAdapterData(files: List<FileModel>) {
        mBreadcrumbRecyclerAdapter.updateData(files)
        if (files.isNotEmpty()) {
            breadcrumbRecyclerView.smoothScrollToPosition(files.size - 1)
        }
    }

    override fun onClick(fileModel: FileModel) {
        if (fileModel.fileType == FileType.FOLDER) {
            addFileFragment(fileModel)
        } else {
            launchFileIntent(fileModel)
        }
    }

    override fun onLongClick(fileModel: FileModel) {
        val optionsDialog = FileOptionsDialog.build {}
        optionsDialog.onDeleteClickListener = {
            deleteFile_(fileModel.path)
            updateContentOfCurrentFragment()
        }
        optionsDialog.onCopyClickListener = {
            isCopyModeActive = true
            selectedFileModel = fileModel
            invalidateOptionsMenu()
        }
        optionsDialog.show(supportFragmentManager, OPTIONS_DIALOG_TAG)
    }

    private fun addFileFragment(fileModel: FileModel) {
        val filesListFragment = FilesListFragment.build {
            path = fileModel.path
        }
        backStackManager.addToStack(fileModel)
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.container, filesListFragment)
        fragmentTransaction.addToBackStack(fileModel.path)
        fragmentTransaction.commit()
    }

    override fun onBackPressed() {

        if (isCopyModeActive) {
            isCopyModeActive = false
            invalidateOptionsMenu()
            return
        }

        super.onBackPressed()
        backStackManager.popFromStack()
        if (supportFragmentManager.backStackEntryCount == 0) {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val subMenu = menu?.findItem(R.id.subMenu)
        val pasteItem = menu?.findItem(R.id.menuPasteFile)
        val cancelItem = menu?.findItem(R.id.menuCancel)

        subMenu?.isVisible = !isCopyModeActive
        pasteItem?.isVisible = isCopyModeActive
        cancelItem?.isVisible = isCopyModeActive

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            R.id.menuNewFile -> createNewFileInCurrentDirectory()
            R.id.menuNewFolder -> createNewFolderInCurrentDirectory()
            R.id.menuCancel -> {
                isCopyModeActive = false
                invalidateOptionsMenu()
            }
            R.id.menuPasteFile -> {
                val intent = Intent(this, FileIntentService::class.java)
                intent.action = FileIntentService.ACTION_COPY
                intent.putExtra(FileIntentService.EXTRA_FILE_SOURCE_PATH, selectedFileModel?.path)
                intent.putExtra(FileIntentService.EXTRA_FILE_DESTINATION_PATH, backStackManager.top.path)
                startService(intent)

                isCopyModeActive = false
                invalidateOptionsMenu()
            }
        }
        return super.onOptionsItemSelected(item)
    }
    private fun createNewFileInCurrentDirectory() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_enter_name, null)
        view.createButton.setOnClickListener {
            val fileName = view.nameEditText.text.toString()
            if (fileName.isNotEmpty()) {
                createNewFile(fileName, backStackManager.top.path) { _, message ->
                    bottomSheetDialog.dismiss()
                    updateContentOfCurrentFragment()
                }
            }
        }
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun createNewFolderInCurrentDirectory() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_enter_name, null)
        view.createButton.setOnClickListener {
            val fileName = view.nameEditText.text.toString()
            if (fileName.isNotEmpty()) {
                createNewFolder(fileName, backStackManager.top.path) { _, message ->
                    bottomSheetDialog.dismiss()
                    updateContentOfCurrentFragment()
                }
            }
        }
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun updateContentOfCurrentFragment() {
        val broadcastIntent = Intent()
        broadcastIntent.action = applicationContext.getString(R.string.file_change_broadcast)
        broadcastIntent.putExtra(FileChangeBroadcastReceiver.EXTRA_PATH, backStackManager.top.path)
        sendBroadcast(broadcastIntent)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
    }
}