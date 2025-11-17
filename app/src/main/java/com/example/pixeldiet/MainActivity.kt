package com.example.pixeldiet

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels // ⭐️ 1. import 추가
import androidx.appcompat.app.AlertDialog
import com.example.pixeldiet.ui.navigation.AppNavigation
import com.example.pixeldiet.ui.theme.PixelDietTheme
import com.example.pixeldiet.viewmodel.SharedViewModel // ⭐️ 2. import 추가

class MainActivity : ComponentActivity() {

    // ⭐️ 3. ViewModel의 인스턴스를 Activity에서 가져옵니다.
    private val viewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⭐️ 4. 권한 확인을 먼저 실행합니다. (데이터 로드에 필수)
        checkUsageStatsPermission()

        setContent {
            PixelDietTheme {
                AppNavigation()
            }
        }
    }

    // ⭐️ 5. [신규] onResume() 오버라이드
    // 사용자가 이 앱으로 돌아올 때마다 호출됩니다.
    override fun onResume() {
        super.onResume()

        // ⭐️ 6. 데이터를 새로고침하라고 ViewModel에 지시합니다.
        // (권한이 있어야만 실행)
        if (hasUsageStatsPermission()) {
            viewModel.refreshData()
        }
    }

    // --- 권한 확인 로직 (동일) ---
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun checkUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            AlertDialog.Builder(this)
                .setTitle("권한 필요")
                .setMessage("앱 사용 시간 정보를 가져오기 위해 '사용 정보 접근' 권한이 필요합니다.")
                .setPositiveButton("설정으로 이동") { _, _ ->
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                .setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}