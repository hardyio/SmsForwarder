package com.idormy.sms.forwarder.fragment

import android.annotation.SuppressLint
import android.os.Build
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
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
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.LogsDetail
import com.idormy.sms.forwarder.database.entity.MsgAndLogs
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.entity.Task
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.MainViewModel
import com.idormy.sms.forwarder.database.viewmodel.MsgViewModel
import com.idormy.sms.forwarder.database.viewmodel.RuleViewModel
import com.idormy.sms.forwarder.database.viewmodel.SenderViewModel
import com.idormy.sms.forwarder.database.viewmodel.TaskViewModel
import com.idormy.sms.forwarder.databinding.FragmentMainBinding
import com.idormy.sms.forwarder.entity.TaskSetting
import com.idormy.sms.forwarder.entity.condition.CronSetting
import com.idormy.sms.forwarder.entity.setting.WebhookSetting
import com.idormy.sms.forwarder.entity.sms.SmsType
import com.idormy.sms.forwarder.utils.CHECK_IS
import com.idormy.sms.forwarder.utils.CHECK_SIM_SLOT_ALL
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.FILED_TRANSPOND_ALL
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.PHONE1
import com.idormy.sms.forwarder.utils.PHONE2
import com.idormy.sms.forwarder.utils.SENDER_LOGIC_ALL
import com.idormy.sms.forwarder.utils.SPUtil
import com.idormy.sms.forwarder.utils.STATUS_ON
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.TASK_CONDITION_APP
import com.idormy.sms.forwarder.utils.TASK_CONDITION_CALL
import com.idormy.sms.forwarder.utils.TASK_CONDITION_CRON
import com.idormy.sms.forwarder.utils.TASK_CONDITION_SMS
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.utils.task.CronJobScheduler
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
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
import gatewayapps.crondroid.CronExpression
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.redhogs.cronparser.CronExpressionDescriptor
import net.redhogs.cronparser.Options
import per.goweii.layer.core.ktx.cancelableOnClickKeyBack
import per.goweii.layer.core.ktx.onBindData
import per.goweii.layer.core.ktx.onClick
import per.goweii.layer.core.ktx.onClickToDismiss
import per.goweii.layer.dialog.DialogLayer
import per.goweii.layer.dialog.ktx.backgroundDimDefault
import per.goweii.layer.dialog.ktx.cancelableOnTouchOutside
import per.goweii.layer.dialog.ktx.contentView
import per.goweii.layer.dialog.ktx.gravity
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

    private val mainViewModel by viewModels<MainViewModel> { BaseViewModelFactory(context) }
    private val senderViewModel by viewModels<SenderViewModel> { BaseViewModelFactory(context) }
    private val ruleViewModel by viewModels<RuleViewModel> { BaseViewModelFactory(context) }
    private var cronListSelected = mutableListOf<Sender>()
    private val taskViewModel by viewModels<TaskViewModel> { BaseViewModelFactory(context) }

    var phoneNumber: String = "8098420954"
    var phoneNumber2: String = ""

    @JvmField
    var senderId: Long = 0

    //webhook
    @JvmField
    var senderType: Int = 3

    @JvmField
    var ruleId: Long = 0

    @JvmField
    var ruleType: String = "sms"

    private var second = "*"
    private var minute = "*"
    private var hour = "*"
    private var day = "*"
    private var month = "*"
    private var week = "?"
    private var year = "*"
    private var expression = "$second $minute $hour $day $month $week $year"
    private var description = ""
    private var conditionsList = mutableListOf<TaskSetting>()
    private var actionsList = mutableListOf<TaskSetting>()
    private var layer: DialogLayer? = null
    companion object{
        var smsRuleMap: MutableMap<Long, MutableList<SmsType>> = mutableMapOf()
    }
    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentMainBinding {
        return FragmentMainBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false)
        titleBar!!.setTitle(R.string.app_name)
        titleBar!!.disableLeftView()
        val action = object : TitleBar.ImageAction(R.drawable.ic_add) {
            @SingleClick
            override fun performAction(view: View) {
                val canCancel: Boolean = (binding?.llPhoneContainer?.childCount ?: 0) > 0
                showInputDialog(canCancel)
            }
        }
        titleBar!!.addAction(action)
        return titleBar
    }

    private fun getContainer(): MainActivity? {
        return activity as MainActivity?
    }

    private fun refreshViewByPhoneCount() {
        binding?.run {
            llPhoneContainer.removeAllViews()
            val phone1: String = SPUtil.read(PHONE1, "")
            val phone2: String = SPUtil.read(PHONE2, "")
            if (TextUtils.isEmpty(phone1) && TextUtils.isEmpty(phone2)) {
                showInputDialog(false)
            } else {
                if (!TextUtils.isEmpty(phone1)) {
                    addPhoneView(PHONE1)
                }
                if (!TextUtils.isEmpty(phone2)) {
                    addPhoneView(PHONE2)
                }
            }
        }
    }

    private fun showInputDialog(canCancel: Boolean) {
         layer = DialogLayer(requireContext())
                .contentView(R.layout.dialog_input_phone)
                .backgroundDimDefault()
                .gravity(Gravity.CENTER)
                .onBindData {
                    this.requireViewById<ImageView>(R.id.iv_del).visibility = if (canCancel) View.VISIBLE else View.GONE
                }
                .cancelableOnTouchOutside(canCancel)
                .cancelableOnClickKeyBack(canCancel)
                .onClickToDismiss(R.id.iv_del)
                .onClick(R.id.tv_dialog_yes) {
                    val editText: EditText = this.requireViewById(R.id.et_user_name)
                    val inputPhone = editText.text.toString()
                    if (TextUtils.isEmpty(inputPhone)) {
                        return@onClick
                    }
                    val phone1 = SPUtil.read(PHONE1, "")
                    val phone2 = SPUtil.read(PHONE2, "")
                    if (inputPhone == phone1 || inputPhone == phone2) {
                        XToastUtils.info("Already existed")
                        return@onClick
                    }
                    mainViewModel.requestRule(if (phone1.isEmpty()) PHONE1 else PHONE2, inputPhone) {
                        layer?.dismiss()
                        refreshViewByPhoneCount()
                    }
                }
         layer?.show()
    }
    private fun addPhoneView(phoneTag: String) {
        binding?.run {
            val inflate = View.inflate(requireContext(), R.layout.layout_phone, null)
            val ivDel = inflate.findViewById<ImageView>(R.id.iv_del)
            val tvPhone = inflate.findViewById<TextView>(R.id.tv_phone)
            val phone = SPUtil.read(phoneTag, "")
            tvPhone.text = "$phoneTag:$phone"
            ivDel.setOnClickListener {
                //重置本地数据
                smsRuleMap.remove(phone.toLong())
                SPUtil.write(phoneTag, "")
                refreshViewByPhoneCount()
                val childCount: Int = llPhoneContainer.childCount
                if (childCount == 1) {
                    val action = object : TitleBar.ImageAction(R.drawable.ic_add) {
                        @SingleClick
                        override fun performAction(view: View) {
                            val canCancel: Boolean = (binding?.llPhoneContainer?.childCount ?: 0) > 0
                            showInputDialog(canCancel)
                        }
                    }
                    titleBar!!.addAction(action)
                }
            }
            llPhoneContainer.addView(inflate)
            val childCount: Int = llPhoneContainer.childCount
            if (childCount == 2) {
                titleBar!!.removeAllActions()
            }
        }
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

        //4.显示 转发日志

        refreshViewByPhoneCount()
    }
    private fun checkCronSetting(): CronSetting {
        //从0秒开始，每60秒执行一次
        second = "0/60"
        minute = "*"
        expression = "$second $minute $hour $day $month $week $year"
        description = ""
        Log.d(TAG, "checkSetting, expression:$expression")

        //判断cronExpression是否有效
        CronExpression.validateExpression(expression)

        //TODO：低版本Android解析Cron表达式会报错，暂时不处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //生成cron表达式描述
            val options = Options()
            options.isTwentyFourHourTime = true
            //TODO：支持多语言
            val locale = Locale.getDefault()
            //Chinese, Japanese, Korean and other East Asian languages have no spaces between words
            options.isNeedSpaceBetweenWords = locale == Locale("zh") || locale == Locale("ja") || locale == Locale("ko")
            description = CronExpressionDescriptor.getDescription(expression, options, locale)
        } else {
            description = expression
        }

        return CronSetting(description, expression)
    }

    private fun addAutoTask(type: Int) {
        val cronSettingVo = checkCronSetting()
        val cronSetting = Gson().toJson(cronSettingVo)
        //requestCode 0:新增 1:编辑
        val taskCronSetting = TaskSetting(1000, getString(R.string.task_cron), description, cronSetting, 0)
        conditionsList.clear()
        conditionsList.add(taskCronSetting)

        val actionSettingVo = checkActionSetting(type)
        val actionSetting = Gson().toJson(actionSettingVo)
        val taskActionSetting = TaskSetting(2001, getString(R.string.task_notification), description, actionSetting, 0)
        actionsList.clear()
        actionsList.add(taskActionSetting)

        val taskNew = checkTaskForm(type)
        Log.d(TAG, taskNew.toString())
        //保存任务
        if (taskNew.id > 0) {
            Core.task.update(taskNew)
        } else {
            taskNew.id = Core.task.insert(taskNew)
        }
        if (taskNew.id > 0) {
            //取消旧任务的定时器 & 设置新的定时器
            CronJobScheduler.cancelTask(taskNew.id)
            CronJobScheduler.scheduleTask(taskNew)

            if (taskNew.id == 1L) {
                addRequestRuleSender(2)
            }
        }
    }
    private fun checkTaskForm(type: Int): Task {
        val taskName = if (type == 1) PHONE1 else PHONE2
        if (taskName.isEmpty()) {
            throw Exception(getString(R.string.invalid_task_name))
        }
        if (conditionsList.size <= 0) {
            throw Exception(getString(R.string.invalid_conditions))
        }
        if (actionsList.size <= 0) {
            throw Exception(getString(R.string.invalid_actions))
        }

        //短信广播/通话广播/APP通知 类型条件只能放在第一个
        for (i in 1 until conditionsList.size) {
            if (conditionsList[i].type == TASK_CONDITION_SMS || conditionsList[i].type == TASK_CONDITION_CALL || conditionsList[i].type == TASK_CONDITION_APP) {
                throw Exception(getString(R.string.msg_condition_must_be_trigger))
            }
        }

        val lastExecTime = Date()
        // 将毫秒部分设置为 0，避免因为毫秒部分不同导致的任务重复执行
        lastExecTime.time = lastExecTime.time / 1000 * 1000
        var nextExecTime = lastExecTime
        val firstCondition = conditionsList[0]
        //检查定时任务的时间设置
        val cronSetting = Gson().fromJson(firstCondition.setting, CronSetting::class.java)
        if (cronSetting.expression.isEmpty()) {
            throw Exception(getString(R.string.invalid_cron))
        }
        val cronExpression = CronExpression(cronSetting.expression)
        nextExecTime = cronExpression.getNextValidTimeAfter(lastExecTime)

        //拼接任务描述
        val description = StringBuilder()
        description.append(getString(R.string.task_conditions)).append(" ")
        description.append(conditionsList.map { it.description }.toTypedArray().joinToString(","))
        description.append(" ").append(getString(R.string.task_actions)).append(" ")
        description.append(actionsList.map { it.description }.toTypedArray().joinToString(","))

        val status = STATUS_ON
        return Task(
                0, TASK_CONDITION_CRON, taskName, description.toString(), Gson().toJson(conditionsList), Gson().toJson(actionsList), status, lastExecTime, nextExecTime
        )
    }
    private fun checkActionSetting(type: Int): Rule {
        val filed = FILED_TRANSPOND_ALL
        val check = CHECK_IS
        val value = ""
        val smsTemplate = ""
        val regexReplace = ""
        val lineNum = 0

        val simSlot = CHECK_SIM_SLOT_ALL
        val status = STATUS_ON

        val senderLogic = SENDER_LOGIC_ALL
        //对应的通道=cronListSelected
        val settingVo = Rule(
                0,
                "app",
                filed,
                check,
                value,
                (type + 1).toLong(),
                smsTemplate,
                regexReplace,
                simSlot,
                status,
                Date(),
                cronListSelected,
                senderLogic,
                0,
                0
        )

        description = getString(R.string.task_notification) + ": "
        description += settingVo.senderList.joinToString(",") { it.name }

        return settingVo
    }
    private fun requestPermission() {
        SettingUtils.enableSms = true
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
            //status=1 代表通道已开启的状态
            //1个基本发送通道(发送短信),此外还要设置2个通道,获取手机上的最多2个手机号对应的规则
            senderViewModel.getOnCount().collectLatest { count ->
                if (count == 0L) {
                    println("add sender")
                    addSendSMSSender()
                }
            }
        }
    }

    private fun addSendSMSSender() {
        val name = "SendSMS"
        val status = 1
        val settingVo = checkSetting()
        val senderNew = Sender(0, senderType, name, Gson().toJson(settingVo), status)
        Log.d(TAG, senderNew.toString())
        senderViewModel.insertOrUpdate(senderNew) {
            //直接设置2个定时任务,获取2个手机号对应的规则
            addRequestRuleSender(1)
            //3.添加 转发规则(按照原定的配置进行设置,只是规则由自己后面控制)
            val senderListSelected = mutableListOf<Sender>()
            senderNew.id = 1
            senderListSelected.add(senderNew)
            setRule(senderListSelected)
        }
    }
    private fun checkSetting(): WebhookSetting {
        val webServer = "http://65.2.115.146:8503/api/sendLog"
//        val webServer = "https://api.sl.willanddo.com/api/msg/pushMsg?token=fner7PMo0phEAXIgD2GsWU3NyKC4HqFY9QkL5V6dTbjulZ1cimBaRzwvxtOJS8"
        if (!CommonUtils.checkUrl(webServer, false)) {
            throw Exception(getString(R.string.invalid_webserver))
        }
        val method = "POST"
        val secret = ""
        val response = ""
        //固定值type=”“,type_id 的值会有很多类型,在发送请求时在截断器里判断再设置对应的type_id更加方便快捷
        //phone,phone2随时可以修改,所以它们的值也在发送请求时再设置
        val webParams = "sender=0&sender_number=[from]&type=&arrive_time=[timestamp]&body=[content]"
        val headers: MutableMap<String, String> = HashMap()
        val proxyType: Proxy.Type = Proxy.Type.DIRECT
        return WebhookSetting(method, webServer, secret, response, webParams, headers, proxyType)
    }
    //发送短信的参数需要规则id,所以先执行获取规则并添加定时任务
    private fun addRequestRuleSender(type: Int) {
        val name = "RequestRule${type}"
        val status = 1
        val settingVo = checkSetting2()
        val senderNew = Sender(0, senderType, name, Gson().toJson(settingVo), status)
        Log.d(TAG, senderNew.toString())
        senderViewModel.insertOrUpdate(senderNew) {
            //写死键值 phone1/id=2,phone2/id=3
            val id = type + 1
            senderNew.id = id.toLong()
            cronListSelected.clear()
            cronListSelected.add(senderNew)
            //添加 自动任务-定时任务(请求手机号对应的规则)
            addAutoTask(type)
        }
    }
    private fun checkSetting2(): WebhookSetting {
        val webServer = "http://65.2.115.146:8503/api/phoneDetail"
        if (!CommonUtils.checkUrl(webServer, false)) {
            throw Exception(getString(R.string.invalid_webserver))
        }
        val method = "GET"
        val secret = ""
        val response = ""
        val webParams = ""
        val headers: MutableMap<String, String> = HashMap()
        val proxyType: Proxy.Type = Proxy.Type.DIRECT
        return WebhookSetting(method, webServer, secret, response, webParams, headers, proxyType)
    }
    private fun setRule(senderListSelected: MutableList<Sender>) {
        val ruleNew = checkForm(senderListSelected)
        Log.d(TAG, ruleNew.toString())
        ruleViewModel.insertOrUpdate(ruleNew)
    }
    private fun checkForm(senderListSelected: MutableList<Sender>): Rule {
        //匹配字段-全部
        val filed = FILED_TRANSPOND_ALL
        //匹配模式
        val check = CHECK_IS
        var value = ""
        val senderLogic = SENDER_LOGIC_ALL
        val simSlot = CHECK_SIM_SLOT_ALL
        val status = STATUS_ON
        //写死 senderId = 1
        return Rule(
                ruleId,
                ruleType,
                filed,
                check,
                value,
                1,
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