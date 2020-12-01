package com.example.fileexplorer.view.fragments

import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fileexplorer.R
import com.example.fileexplorer.bgtask.FileChangeBroadcastReceiver
import com.example.fileexplorer.model.FileModel
import com.example.fileexplorer.utils.getFileModelsFromFiles
import com.example.fileexplorer.utils.getFilesFromPath
import com.example.fileexplorer.view.adapter.FilesRecyclerAdapter
import kotlinx.android.synthetic.main.fragment_files_list.*

class FilesListFragment : Fragment() {

    private lateinit var mFilesAdapter: FilesRecyclerAdapter
    private lateinit var PATH: String
    private lateinit var mCallback: OnItemClickListener
    private lateinit var mFileChangeBroadcastReceiver: FileChangeBroadcastReceiver


    interface OnItemClickListener {
        fun onClick(fileModel: FileModel)
        fun onLongClick(fileModel: FileModel)
    }

    companion object {
        private const val ARG_PATH: String = "com.example.fileexplorer.fileslist.path"
        fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_files_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filePath = arguments?.getString(ARG_PATH)
        if (filePath == null) {
            Toast.makeText(context, "Path should not be null!", Toast.LENGTH_SHORT).show()
            return
        }
        PATH = filePath

        mFileChangeBroadcastReceiver = FileChangeBroadcastReceiver(PATH) {
            updateDate()
        }
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            mCallback = context as OnItemClickListener
        } catch (e: Exception) {
            throw Exception("${context} should implement FilesListFragment.OnItemCLickListener")
        }
    }

    override fun onResume() {
        super.onResume()
        context?.registerReceiver(mFileChangeBroadcastReceiver, IntentFilter(getString(R.string.file_change_broadcast)))
    }

    override fun onPause() {
        super.onPause()
        context?.unregisterReceiver(mFileChangeBroadcastReceiver)
    }

    class Builder {
        var path: String = ""

        fun build(): FilesListFragment {
            val fragment = FilesListFragment()
            val args = Bundle()
            args.putString(ARG_PATH, path)
            fragment.arguments = args;
            return fragment
        }
    }

    private fun initViews() {
        filesRecyclerView.layoutManager = LinearLayoutManager(context)
        mFilesAdapter = FilesRecyclerAdapter()
        filesRecyclerView.adapter = mFilesAdapter

        mFilesAdapter.onItemClickListener = {
            mCallback.onClick(it)
        }

        mFilesAdapter.onItemLongClickListener = {
            mCallback.onLongClick(it)
        }
        updateDate()
    }

    fun updateDate() {
        val files = getFileModelsFromFiles(getFilesFromPath(PATH))

        if (files.isEmpty()) {
            emptyFolderLayout.visibility = View.VISIBLE
        } else {
            emptyFolderLayout.visibility = View.INVISIBLE
        }

        mFilesAdapter.updateData(files)
    }
}