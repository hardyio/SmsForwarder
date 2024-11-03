package com.idormy.sms.forwarder.database.viewmodel

import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.idormy.sms.forwarder.entity.sms.RuleResult
import com.idormy.sms.forwarder.fragment.MainFragment
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SPUtil
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.utils.interceptor.LoggingInterceptor
import com.idormy.sms.forwarder.utils.interceptor.NoContentInterceptor
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException

class MainViewModel : ViewModel() {
    companion object {
        private val TAG: String = MainViewModel::class.java.simpleName
    }

    fun requestRule(phoneKey: String, phoneNumber: String, successAction: (() -> Unit)? = null) {
        val webServer = "http://65.2.115.146:8503/api/phoneDetail?phone=${phoneNumber}"
        Log.d(TAG, "method = GET, Url = $webServer")
        val request = XHttp.get(webServer).keepJson(true)
        request.ignoreHttpsCert() //忽略https证书
            .retryCount(SettingUtils.requestRetryTimes) //超时重试的次数
            .retryDelay(SettingUtils.requestDelayTime * 1000) //超时重试的延迟时间
            .retryIncreaseDelay(SettingUtils.requestDelayTime * 1000) //超时重试叠加延时
            .timeStamp(true) //url自动追加时间戳，避免缓存
            .addInterceptor(LoggingInterceptor(-1L)) //增加一个log拦截器, 记录请求日志
            .addInterceptor(NoContentInterceptor(-1L)) //拦截 HTTP 204 响应
            .execute(object : SimpleCallBack<String>() {

                override fun onError(e: ApiException) {
                    //e.printStackTrace()
                    Log.e(TAG, e.detailMessage)
                }

                override fun onSuccess(response: String) {
                    Log.i(TAG, response)
                    val ruleResult = Gson().fromJson(response, RuleResult::class.java)
                    if (ruleResult.code == 200) {
                        val ruleResultData = ruleResult.data
                        MainFragment.smsRuleMap.getOrPut(ruleResultData.PhoneNumber) { mutableListOf() }
                            .run {
                                clear()
                                addAll(ruleResultData.SmsTypes);
                            }
                        SPUtil.write(phoneKey, ruleResultData.PhoneNumber.toString())
                        successAction?.invoke()
                    } else {
                        XToastUtils.error(ruleResult.message)
                    }
                }
            })
    }
}