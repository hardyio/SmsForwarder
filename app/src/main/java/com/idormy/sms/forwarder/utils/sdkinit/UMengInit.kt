package com.idormy.sms.forwarder.utils.sdkinit

//import com.meituan.android.walle.WalleChannelReader
import android.app.Application
import android.content.Context
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.utils.SettingUtils.Companion.isAgreePrivacy
import com.xuexiang.xui.XUI

/**
 * UMeng 统计 SDK初始化
 *
 * @author xuexiang
 * @since 2019-06-18 15:49
 */
class UMengInit private constructor() {
    companion object {
        private const val DEFAULT_CHANNEL_ID = "github"
        /**
         * 初始化SDK,合规指南【先进行预初始化，如果用户隐私同意后可以初始化UmengSDK进行信息上报】
         */
        /**
         * 初始化SDK,合规指南【先进行预初始化，如果用户隐私同意后可以初始化UmengSDK进行信息上报】
         */
        @JvmOverloads
        fun init(context: Context = XUI.getContext()) {
            val appContext = context.applicationContext
            if (appContext is Application) {
                initApplication(appContext)
            }
        }

        /**
         * 初始化SDK,合规指南【先进行预初始化，如果用户隐私同意后可以初始化UmengSDK进行信息上报】
         */
        private fun initApplication(application: Application?) {
            // 运营统计数据调试运行时不初始化
            if (App.isDebug) {
                return
            }
            // 用户同意了隐私协议
            if (isAgreePrivacy) {
                realInit(application)
            }
        }

        /**
         * 真实的初始化UmengSDK【进行设备信息的统计上报，必须在获得用户隐私同意后方可调用】
         */
        private fun realInit(application: Application?) {
            // 运营统计数据调试运行时不初始化
            if (App.isDebug) {
                return
            }
        }

        /**
         * 获取渠道信息
         */
        //private fun getChannel(context: Context?): String {
        //    return WalleChannelReader.getChannel(context!!, DEFAULT_CHANNEL_ID)
        //}
    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}