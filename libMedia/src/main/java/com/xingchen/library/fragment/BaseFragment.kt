package com.xingchen.library.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view, savedInstanceState)
        initListener(view, savedInstanceState)
    }

    /**
     * 初始化视图
     */
    open fun initView(view: View, savedInstanceState: Bundle?) {}

    /**
     * 初始化监听器
     */
    open fun initListener(view: View, savedInstanceState: Bundle?) {}
}