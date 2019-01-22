package com.effective.android.wxrp.view

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import com.effective.android.wxrp.Constants
import com.effective.android.wxrp.R
import com.effective.android.wxrp.store.Config
import com.effective.android.wxrp.utils.ToolUtil
import kotlinx.android.synthetic.main.activity_main.*
import android.content.ComponentName
import com.effective.android.wxrp.MainViewModel
import com.effective.android.wxrp.store.db.PacketRecordDataBase
import com.effective.android.wxrp.store.db.PacketRepository


class MainActivity : AppCompatActivity() {

    private var mainViewModel: MainViewModel? = null

    companion object {
        private const val TAG = "MainActivity"
    }

    private var settingFragment: SettingFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initData()
        initListener()
    }

    private fun initData() {
        val database = PacketRecordDataBase.getInstance(this)
        val packetRepository = PacketRepository(database.packetRecordDao())
        mainViewModel = ViewModelProviders.of(this, MainViewModel.FACTORY(packetRepository)).get(MainViewModel::class.java)
    }

    private fun initNecessaryState() {
        val hasOpenAccessibility = ToolUtil.isServiceRunning(this, Constants.PACKAGE_SELF_APPLICATION + "." + Constants.CLASS_ACCESSBILITY)
        if (hasOpenAccessibility) {
            accessibility_tip.text = getString(R.string.setting_open_accessibility_tip)
            open_accessibility.visibility = View.GONE
        } else {
            accessibility_tip.text = getString(R.string.setting_close_accessibility_tip)
            open_accessibility.visibility = View.VISIBLE
        }

        val userName = Config.getUserWxName()
        if (!TextUtils.isEmpty(userName)) {
            user_name_tip.text = "微信昵称 $userName"
            get_user_name.text = getString(R.string.setting_get_again_action)
        } else {
            user_name_tip.text = getString(R.string.setting_lost_user_name_tip)
            get_user_name.text = getString(R.string.setting_get_action)
        }

        val openSetting = hasOpenAccessibility && !TextUtils.isEmpty(userName)
        setting.visibility = if (openSetting) {
            View.VISIBLE
        } else {
            View.GONE
        }
        more_setting_tip.visibility = setting.visibility
        packet_list.visibility = setting.visibility
    }

    private fun initListener() {
        setting.setOnClickListener {
            showSettingFragment()
        }
        open_accessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        get_user_name.setOnClickListener {
            val hasOpenAccessibility = ToolUtil.isServiceRunning(this, Constants.PACKAGE_SELF_APPLICATION + "." + Constants.CLASS_ACCESSBILITY)
            if (!hasOpenAccessibility) {
                ToolUtil.toast(this, "请先开启自动模拟点击服务")
                return@setOnClickListener
            }
            if (ToolUtil.isWeixinAvilible(this)) {
                val intent = Intent()
                val cmp = ComponentName(Constants.PACKAFEGE_WECHAT, Constants.CLASS_LAUNCHER)
                intent.action = Intent.ACTION_MAIN
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.component = cmp
                startActivity(intent)
            } else {
                ToolUtil.toast(this, "当前手机未安装微信，请下载微信7.0！")
            }
        }

        user_name_question.setOnClickListener {
            ToolUtil.toast(this, getString(R.string.setting_get_user_name_question), 5000)
        }

        //监听数据源变化
        mainViewModel!!._add_data.observe(this, Observer { value ->
            value?.let {
                packet_list.addPacket(it)
            }
        })

        mainViewModel!!._all_data.observe(this, Observer { value ->
            value?.let {
                packet_list.addPackets(it)
            }
        })
    }

    private fun showSettingFragment() {
        if (settingFragment == null) {
            settingFragment = SettingFragment()
            supportFragmentManager.beginTransaction().replace(R.id.setting_container, settingFragment!!).commit()
        } else {
            supportFragmentManager.beginTransaction().show(settingFragment!!).commit()
        }
    }

    private fun hideSettingFragment() {
        supportFragmentManager.beginTransaction().hide(settingFragment!!).commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        Config.onSave()
    }


    override fun onBackPressed() {
        if (settingFragment != null && settingFragment!!.isVisible) {
            hideSettingFragment()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        initNecessaryState()
    }
}