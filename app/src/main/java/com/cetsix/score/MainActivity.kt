package com.cetsix.score

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cetsix.core.ui.theme.CetSixTheme
import com.cetsix.score.ui.navigation.AppNavHost
import com.cetsix.score.ui.navigation.rememberRepository

/**
 * 全应用唯一的 Activity。
 *
 * 完全离线：不声明 INTERNET 权限，不做任何网络调用。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CetSixTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // 先 Composable 取出仓库单例，再以普通 lambda 传给 NavHost
                    val repository = rememberRepository()
                    AppNavHost(repositoryProvider = { repository })
                }
            }
        }
    }
}
