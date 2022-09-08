package com.example.runningmate2.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.runningmate2.databinding.FragmentRecordBinding
import com.example.runningmate2.recyclerView.Adapter
import com.example.runningmate2.recyclerView.Data
import com.example.runningmate2.recyclerView.toData
import com.example.runningmate2.viewModel.MainViewModel

class RecordFragment : Fragment() {

    private val mainViewModel: MainViewModel by activityViewModels()
    val binding: FragmentRecordBinding by lazy {
        FragmentRecordBinding.inflate(layoutInflater)
    }
    private val adapter = Adapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mainViewModel.readDB()

        parentFragmentManager
            .beginTransaction()
            .replace(binding.recordeFrameLayout.id, RecordGraphFragment())
            .commit()

//        binding.myRecyclerView.apply {
//            this.adapter = this@RecordFragment.adapter
//            this.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
//        }

        binding.changeBotton.setOnClickListener{
            if(binding.changeBotton.text == "기록 보기"){
                parentFragmentManager
                    .beginTransaction()
                    .replace(binding.recordeFrameLayout.id, RecordRecyclerFragment())
                    .commit()
                binding.changeBotton.text = "통계 보기"
            }else{
                parentFragmentManager
                    .beginTransaction()
                    .replace(binding.recordeFrameLayout.id, RecordGraphFragment())
                    .commit()
                binding.changeBotton.text = "기록 보기"
            }
        }


        adapter.setItemClickListener(object: Adapter.OnItemClickListener{
            override fun onClick(position: Int) {
                val data = adapter.datalist[position]
                binding.deleteText.text = data.toString()
                Log.e(javaClass.simpleName, "fragment onClick: ${adapter.datalist[position]}")
                Log.e(javaClass.simpleName, "fragment onClick position: $position")
                viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                    mainViewModel.deleteDB(adapter.datalist[position].id)
                    mainViewModel.readDB()
                }
            }
        })

//        binding.deleteButton.setOnClickListener{
//            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
//                mainViewModel.deleteDB()
//                mainViewModel.readDB()
//            }
//        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel.runningData.observe(viewLifecycleOwner) { data ->
            Log.e("TAG", "데이터 넘어온거: $data", )
            adapter.datalist = data.map { it.toData() } as ArrayList<Data>
        }
    }

}