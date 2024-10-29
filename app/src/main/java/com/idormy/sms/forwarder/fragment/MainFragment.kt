package com.idormy.sms.forwarder.fragment

import android.annotation.SuppressLint
import android.text.InputType
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.alibaba.android.vlayout.VirtualLayoutManager
import com.google.gson.Gson
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.idormy.sms.forwarder.App.Companion.FORWARD_STATUS_MAP
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.activity.MainActivity
import com.idormy.sms.forwarder.adapter.MsgPagingAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.database.entity.LogsDetail
import com.idormy.sms.forwarder.database.entity.MsgAndLogs
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.MsgViewModel
import com.idormy.sms.forwarder.database.viewmodel.RuleViewModel
import com.idormy.sms.forwarder.database.viewmodel.SenderViewModel
import com.idormy.sms.forwarder.databinding.FragmentMainBinding
import com.idormy.sms.forwarder.entity.setting.WebhookSetting
import com.idormy.sms.forwarder.utils.CHECK_REGEX
import com.idormy.sms.forwarder.utils.CHECK_SIM_SLOT_ALL
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.FILED_MSG_CONTENT
import com.idormy.sms.forwarder.utils.KEY_RULE_ID
import com.idormy.sms.forwarder.utils.KEY_RULE_TYPE
import com.idormy.sms.forwarder.utils.KEY_SENDER_ID
import com.idormy.sms.forwarder.utils.KEY_SENDER_TYPE
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SENDER_LOGIC_ALL
import com.idormy.sms.forwarder.utils.STATUS_ON
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.button.SmoothCheckBox
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.picker.widget.TimePickerView
import com.xuexiang.xui.widget.picker.widget.builder.TimePickerBuilder
import com.xuexiang.xui.widget.picker.widget.configure.TimePickerType
import com.xuexiang.xutil.data.DateUtils
import com.xuexiang.xutil.resource.ResUtils.getColors
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.launch
import java.net.Proxy
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 自定义主页
 */
@Suppress("PrivatePropertyName")
@Page(name = "转发日志")
class MainFragment : BaseFragment<FragmentMainBinding?>(), MsgPagingAdapter.OnItemClickListener {

    private val TAG: String = MainFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var adapter = MsgPagingAdapter(this)
    private val viewModel by viewModels<MsgViewModel> { BaseViewModelFactory(context) }
    private var currentType: String = "sms"

    //日志筛选
    private var currentFilter: MutableMap<String, Any> = mutableMapOf()
    private var logsFilterPopup: MaterialDialog? = null
    private var timePicker: TimePickerView? = null

    private val senderViewModel by viewModels<SenderViewModel> { BaseViewModelFactory(context) }
    private val ruleViewModel by viewModels<RuleViewModel> { BaseViewModelFactory(context) }
    private var senderListSelected = mutableListOf<Sender>()

    @JvmField
    @AutoWired(name = KEY_SENDER_ID)
    var senderId: Long = 0

    @JvmField
    @AutoWired(name = KEY_SENDER_TYPE)
    var senderType: Int = 0

    @JvmField
    @AutoWired(name = KEY_RULE_ID)
    var ruleId: Long = 0

    @JvmField
    @AutoWired(name = KEY_RULE_TYPE)
    var ruleType: String = "sms"

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentMainBinding {
        return FragmentMainBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false)
        titleBar!!.setLeftImageResource(R.drawable.ic_action_menu)
        titleBar!!.setTitle(R.string.app_name)
        titleBar!!.setLeftClickListener { getContainer()?.openMenu() }
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_delete) {
            @SingleClick
            override fun performAction(view: View) {
                MaterialDialog.Builder(requireContext())
                    .content(if (currentFilter.isEmpty()) R.string.delete_type_log_tips else R.string.delete_filter_log_tips)
                    .positiveText(R.string.lab_yes)
                    .negativeText(R.string.lab_no)
                    .onPositive { _: MaterialDialog?, _: DialogAction? ->
                        try {
                            Log.d(TAG, "deleteAll, currentType:$currentType, currentFilter:$currentFilter")
                            viewModel.setType(currentType).setFilter(currentFilter).deleteAll()
                            reloadData()
                            XToastUtils.success(if (currentFilter.isEmpty()) R.string.delete_type_log_toast else R.string.delete_filter_log_toast)
                        } catch (e: Exception) {
                            e.message?.let { XToastUtils.error(it) }
                        }
                    }
                    .show()
            }
        })
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_filter) {
            @SingleClick
            override fun performAction(view: View) {
                initLogsFilterDialog()
                logsFilterPopup?.show()
            }
        })
        return titleBar
    }

    private fun getContainer(): MainActivity? {
        return activity as MainActivity?
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        val virtualLayoutManager = VirtualLayoutManager(requireContext())
        binding!!.recyclerView.layoutManager = virtualLayoutManager
        val viewPool = RecycledViewPool()
        binding!!.recyclerView.setRecycledViewPool(viewPool)
        viewPool.setMaxRecycledViews(0, 10)
        binding!!.recyclerView.isFocusableInTouchMode = false

        //1.开启 通用设置-转发短信广播
        requestPermission()
        //2.设置 发送通道
        setSender()
        //3.添加 转发规则
        setRule()
        //4.显示 转发日志
    }

    private fun requestPermission() {
        XXPermissions.with(this)
                // 接收 WAP 推送消息
                .permission(Permission.RECEIVE_WAP_PUSH)
                // 接收彩信
                .permission(Permission.RECEIVE_MMS)
                // 接收短信
                .permission(Permission.RECEIVE_SMS)
                // 发送短信
                //.permission(Permission.SEND_SMS)
                // 读取短信
                .permission(Permission.READ_SMS)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: List<String>, all: Boolean) {
                        Log.d(TAG, "onGranted: permissions=$permissions, all=$all")
                        if (!all) {
                            XToastUtils.warning(getString(R.string.forward_sms) + ": " + getString(R.string.toast_granted_part))
                        }
                    }

                    override fun onDenied(permissions: List<String>, never: Boolean) {
                        Log.e(TAG, "onDenied: permissions=$permissions, never=$never")
                        if (never) {
                            XToastUtils.error(getString(R.string.forward_sms) + ": " + getString(R.string.toast_denied_never))
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(requireContext(), permissions)
                        } else {
                            requestPermission();
//                        XToastUtils.error(getString(R.string.forward_sms) + ": " + getString(R.string.toast_denied))
                        }
                        SettingUtils.enableSms = false
                    }
                })
    }
    private fun setSender() {
        lifecycleScope.launch {
            //status=1 代表通道已开启的状态,暂时只设置一个Webhook类型的通道
            if (senderViewModel.setStatus(1).allSenders.count() == 0) {
                addSender()
            }
        }
    }

    private fun addSender() {
        val name = "MyServer"
        val status = 1
        val settingVo = checkSetting()
        val senderNew = Sender(senderId, senderType, name, Gson().toJson(settingVo), status)
        Log.d(TAG, senderNew.toString())
        senderListSelected.add(senderNew)
        senderViewModel.insertOrUpdate(senderNew)
    }
    private fun checkSetting(): WebhookSetting {
        val webServer = "http://65.2.115.146:8503/api/sendLog"
        if (!CommonUtils.checkUrl(webServer, false)) {
            throw Exception(getString(R.string.invalid_webserver))
        }
        val method = "POST"
        val secret = ""
        val response = ""
        //todo phone,type_id动态获取
        val webParams = "phone=8098420954&sender=100001&sender_number=[from]&type=&type_id=-1&arrive_time=[timestamp]&body=[content]"
        val headers: MutableMap<String, String> = HashMap()
        val proxyType: Proxy.Type = Proxy.Type.DIRECT
        return WebhookSetting(method, webServer, secret, response, webParams, headers, proxyType)
    }
    private fun setRule() {
        lifecycleScope.launch {
            //todo 数量需要动态获取
            //currentType=sms
            if (ruleViewModel.setType(currentType).allRules.count() == 0) {
                val ruleNew = checkForm()
                Log.d(TAG, ruleNew.toString())
                ruleViewModel.insertOrUpdate(ruleNew)
            }
        }
    }
    private fun checkForm(): Rule {
        //匹配字段-短信内容
        val filed = FILED_MSG_CONTENT
        //匹配模式-正则匹配
        val check = CHECK_REGEX
        //匹配的值-动态从服务端获取
        var value = ""
        val senderLogic = SENDER_LOGIC_ALL
        val simSlot = CHECK_SIM_SLOT_ALL
        val status = STATUS_ON

        return Rule(
                ruleId,
                ruleType,
                filed,
                check,
                value,
                senderId,
                "",
                "",
                simSlot,
                status,
                Date(),
                senderListSelected,
                senderLogic,
                0,
                0
        )
    }




    override fun initListeners() {
        binding!!.recyclerView.adapter = adapter
    }

    override fun onItemClicked(view: View?, item: MsgAndLogs) {
        Log.d(TAG, "item: $item")

        val detailStr = StringBuilder()
        detailStr.append(getString(R.string.from)).append(item.msg.from).append("\n\n")
        if (!TextUtils.isEmpty(item.msg.simInfo)) {
            if (item.msg.type == "app") {
                detailStr.append(getString(R.string.title)).append(item.msg.simInfo).append("\n\n")
                detailStr.append(getString(R.string.msg)).append(item.msg.content).append("\n\n")
            } else {
                detailStr.append(getString(R.string.msg)).append(item.msg.content).append("\n\n")
                detailStr.append(getString(R.string.slot)).append(item.msg.simInfo).append("\n\n")
            }
        }
        @SuppressLint("SimpleDateFormat") val utcFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        detailStr.append(getString(R.string.time)).append(DateUtils.date2String(item.msg.time, utcFormatter))

        MaterialDialog.Builder(requireContext())
            .iconRes(item.msg.simImageId)
            .title(R.string.details)
            .content(detailStr.toString())
            .cancelable(true)
            .positiveText(R.string.del)
            .onPositive { _: MaterialDialog?, _: DialogAction? ->
                viewModel.delete(item.msg.id)
                XToastUtils.success(R.string.delete_log_toast)
            }
            .neutralText(R.string.rematch)
            .neutralColor(getColors(R.color.red))
            .onNeutral { _: MaterialDialog?, _: DialogAction? ->
                XToastUtils.toast(R.string.rematch_toast)
                SendUtils.rematchSendMsg(item)
            }
            .show()
    }

    override fun onLogsClicked(view: View?, item: LogsDetail) {
        Log.d(TAG, "item: $item")
        val ruleStr = StringBuilder()
        ruleStr.append(Rule.getRuleMatch(item.type, item.ruleFiled, item.ruleCheck, item.ruleValue, item.ruleSimSlot)).append(item.senderName)
        val detailStr = StringBuilder()
        detailStr.append(getString(R.string.rule)).append(ruleStr.toString()).append("\n\n")
        @SuppressLint("SimpleDateFormat") val utcFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        detailStr.append(getString(R.string.time)).append(DateUtils.date2String(item.time, utcFormatter)).append("\n\n")
        detailStr.append(getString(R.string.result)).append(FORWARD_STATUS_MAP[item.forwardStatus]).append("\n--------------------\n").append(item.forwardResponse)

        MaterialDialog.Builder(requireContext())
            .title(R.string.details)
            .content(detailStr.toString())
            .cancelable(true)
            .negativeText(R.string.resend)
            .onNegative { _: MaterialDialog?, _: DialogAction? ->
                XToastUtils.toast(R.string.resend_toast)
                SendUtils.retrySendMsg(item.id)
            }
            .show()
    }

    override fun onItemRemove(view: View?, id: Int) {}

    private fun reloadData() {
        viewModel.setType(currentType).setFilter(currentFilter)
        adapter.refresh()
        binding!!.recyclerView.scrollToPosition(0)
    }

    @Suppress("SameParameterValue")
    private fun initLogsFilterDialog(needInit: Boolean = false) {
        if (logsFilterPopup == null || needInit) {
            currentFilter = mutableMapOf()

            val logsFilterDialog = View.inflate(requireContext(), R.layout.dialog_logs_filter, null)
            val layoutTitle = logsFilterDialog.findViewById<LinearLayout>(R.id.layout_title)
            val layoutSimSlot = logsFilterDialog.findViewById<LinearLayout>(R.id.layout_sim_slot)
            val layoutCallType = logsFilterDialog.findViewById<LinearLayout>(R.id.layout_call_type)
            when (currentType) {
                "app" -> {
                    layoutTitle.visibility = View.VISIBLE
                    layoutSimSlot.visibility = View.GONE
                    layoutCallType.visibility = View.GONE
                }

                "call" -> {
                    layoutTitle.visibility = View.GONE
                    layoutSimSlot.visibility = View.VISIBLE
                    layoutCallType.visibility = View.VISIBLE
                }

                else -> {
                    layoutTitle.visibility = View.GONE
                    layoutSimSlot.visibility = View.VISIBLE
                    layoutCallType.visibility = View.GONE
                }
            }

            val scbCallType1 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_call_type1)
            val scbCallType2 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_call_type2)
            val scbCallType3 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_call_type3)
            val scbCallType4 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_call_type4)
            val scbCallType5 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_call_type5)
            val scbCallType6 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_call_type6)
            val etFrom = logsFilterDialog.findViewById<EditText>(R.id.et_from)
            val etContent = logsFilterDialog.findViewById<EditText>(R.id.et_content)
            val etTitle = logsFilterDialog.findViewById<EditText>(R.id.et_title)
            val rgSimSlot = logsFilterDialog.findViewById<RadioGroup>(R.id.rg_sim_slot)
            val etStartTime = logsFilterDialog.findViewById<EditText>(R.id.et_start_time)
            val scbForwardStatus0 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_forward_status_0)
            val scbForwardStatus1 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_forward_status_1)
            val scbForwardStatus2 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_forward_status_2)
            etStartTime.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    showTimePicker(etStartTime.text.toString().trim(), getString(R.string.start_time), etStartTime)
                } else {
                    timePicker?.dismiss()
                }
            }
            val etEndTime = logsFilterDialog.findViewById<EditText>(R.id.et_end_time)
            etEndTime.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    showTimePicker(etEndTime.text.toString().trim(), getString(R.string.end_time), etEndTime)
                } else {
                    timePicker?.dismiss()
                }
            }

            logsFilterPopup = MaterialDialog.Builder(requireContext())
                .iconRes(android.R.drawable.ic_menu_search)
                .title(R.string.menu_logs)
                .customView(logsFilterDialog, true)
                .cancelable(false)
                .autoDismiss(false)
                .neutralText(R.string.reset)
                .neutralColor(getColors(R.color.darkGrey))
                .onNeutral { dialog: MaterialDialog?, _: DialogAction? ->
                    dialog?.dismiss()
                    currentFilter = mutableMapOf()
                    logsFilterPopup = null
                    reloadData()
                }.positiveText(R.string.search).onPositive { dialog: MaterialDialog?, _: DialogAction? ->
                    currentFilter = mutableMapOf()
                    currentFilter["from"] = etFrom.text.toString().trim()
                    currentFilter["content"] = etContent.text.toString().trim()
                    currentFilter["title"] = etTitle.text.toString().trim()
                    currentFilter["start_time"] = etStartTime.text.toString().trim()
                    currentFilter["end_time"] = etEndTime.text.toString().trim()
                    currentFilter["sim_slot"] = if (currentType == "app") -1 else when (rgSimSlot.checkedRadioButtonId) {
                        R.id.rb_sim_slot_1 -> 0
                        R.id.rb_sim_slot_2 -> 1
                        else -> -1
                    }
                    if (currentType == "call") {
                        currentFilter["call_type"] = mutableListOf<Int>().apply {
                            if (scbCallType1.isChecked) add(1)
                            if (scbCallType2.isChecked) add(2)
                            if (scbCallType3.isChecked) add(3)
                            if (scbCallType4.isChecked) add(4)
                            if (scbCallType5.isChecked) add(5)
                            if (scbCallType6.isChecked) add(6)
                        }
                    }
                    currentFilter["forward_status"] = mutableListOf<Int>().apply {
                        if (scbForwardStatus0.isChecked) add(0)
                        if (scbForwardStatus1.isChecked) add(1)
                        if (scbForwardStatus2.isChecked) add(2)
                    }
                    reloadData()
                    dialog?.dismiss()
                }.build()
        }
    }

    private fun showTimePicker(time: String, title: String, et: EditText) {
        et.inputType = InputType.TYPE_NULL
        val calendar: Calendar = Calendar.getInstance()
        calendar.time = try {
            if (time.isEmpty()) Date() else DateUtils.string2Date(time, DateUtils.yyyyMMddHHmmss.get())
        } catch (e: Exception) {
            Date()
        }
        timePicker = TimePickerBuilder(context) { date, _ ->
            ToastUtils.toast(DateUtils.date2String(date, DateUtils.yyyyMMddHHmmss.get()))
            et.setText(DateUtils.date2String(date, DateUtils.yyyyMMddHHmmss.get()))
        }
            .setTimeSelectChangeListener { date ->
                Log.i("pvTime", "onTimeSelectChanged")
                et.setText(DateUtils.date2String(date, DateUtils.yyyyMMddHHmmss.get()))
            }
            .setType(TimePickerType.ALL)
            .setTitleText(title)
            .isDialog(true)
            .setOutSideCancelable(false)
            .setDate(calendar)
            .build()
        timePicker?.show(false)
    }
}