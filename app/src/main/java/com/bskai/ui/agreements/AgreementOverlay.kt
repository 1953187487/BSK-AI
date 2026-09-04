package com.bskai.ui.agreements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AgreementOverlay(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var currentTab by remember { mutableStateOf(0) } // 0=开源协议, 1=使用协议

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // Logo
            Text(
                text = "AURA",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 6.sp
                ),
                color = Color(0xFF6C5CE7)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "智能语音助手",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8B6D0)
            )

            Spacer(Modifier.height(32.dp))

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16162A), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("开源协议", "使用协议").forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp)
                            .background(
                                if (currentTab == idx) Color(0xFF6C5CE7) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { currentTab = idx }
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = if (currentTab == idx) Color.White else Color(0xFFB8B6D0),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Content
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16162A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    if (currentTab == 0) {
                        OpenSourceAgreementContent()
                    } else {
                        UsageAgreementContent()
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Accept button
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text("接受并继续", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = onDecline,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6B6B8D))
            ) {
                Text("退出应用", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun OpenSourceAgreementContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LicenseSection(
            title = "MIT License",
            year = "2026",
            holder = "BSK-AI Contributors"
        ) {
            """
            Permission is hereby granted, free of charge, to any person obtaining a copy
            of this software and associated documentation files (the "Software"), to deal
            in the Software without restriction, including without limitation the rights
            to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
            copies of the Software, and to permit persons to whom the Software is
            furnished to do so, subject to the following conditions:

            The above copyright notice and this permission notice shall be included in all
            copies or substantial portions of the Software.

            THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
            IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
            FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
            AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
            LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
            OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
            SOFTWARE.
            """.trimIndent()
        }

        LicenseSection(
            title = "Apache License 2.0 (部分组件)",
            year = "2026",
            holder = "Android Open Source Project"
        ) {
            """
            Licensed under the Apache License, Version 2.0 (the "License");
            you may not use this file except in compliance with the License.
            You may obtain a copy of the License at

                http://www.apache.org/licenses/LICENSE-2.0

            Unless required by applicable law or agreed to in writing, software
            distributed under the License is distributed on an "AS IS" BASIS,
            WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
            See the License for the specific language governing permissions and
            limitations under the License.
            """.trimIndent()
        }

        LicenseSection(
            title = "JetBrains Kotlin License",
            year = "2015-2026",
            holder = "JetBrains s.r.o."
        ) {
            """
            Kotlin is distributed under the Apache License 2.0.
            Kotlin source code is maintained at: https://github.com/JetBrains/kotlin
            """.trimIndent()
        }

        LicenseSection(
            title = "Google Android Licenses",
            year = "2026",
            holder = "Google LLC"
        ) {
            """
            Android is a trademark of Google LLC.
            This project uses Android SDK components under the Android Software Development Kit License Agreement.
            """.trimIndent()
        }
    }
}

@Composable
private fun LicenseSection(title: String, year: String, holder: String, content: () -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF6C5CE7)
            )
            Text(
                text = "$holder · $year",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B6B8D)
            )
        }
        Text(
            text = content(),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB8B6D0),
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun UsageAgreementContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader("一、服务说明")
        BodyText("AURA是一款运行在Android设备上的智能语音助手应用，通过语音识别、自然语言处理等技术，为用户提供语音交互服务。")

        SectionHeader("二、用户权利")
        BodyText("1. 您可以在自己的设备上自由安装和使用本应用。")
        BodyText("2. 您可以查看源代码（如适用），并根据开源协议进行修改和分发。")
        BodyText("3. 您有权通过设置页面配置自定义API服务商。")

        SectionHeader("三、用户义务")
        BodyText("1. 您应当合法使用本应用，不得利用本应用从事任何违法活动。")
        BodyText("2. 您应当妥善保管您的API密钥，不得将其泄露给第三方。")
        BodyText("3. 您在使用语音功能时，应确保所处环境符合录音的法律要求。")

        SectionHeader("四、隐私保护")
        BodyText("1. 本应用仅在本地收集和处理您的语音数据，不会将您的语音上传到第三方服务器（除非您配置了自定义API）。")
        BodyText("2. 对话历史记录仅保存在本地设备上。")
        BodyText("3. 如您配置了自定义API，数据将传输至您指定的服务商，请自行了解其隐私政策。")

        SectionHeader("五、免责声明")
        BodyText("1. 本应用按'原样'提供，不提供任何明示或暗示的担保。")
        BodyText("2. 开发者不对因使用本应用而产生的任何直接或间接损失承担责任。")
        BodyText("3. 用户应自行承担使用本应用的风险。")

        SectionHeader("六、更新与维护")
        BodyText("开发者保留随时更新本应用的权利，包括但不限于功能更新、安全补丁等。开发者没有义务为用户提供持续的技术支持。")

        SectionHeader("七、协议变更")
        BodyText("本使用协议可能会随着应用的发展而更新。更新后的协议将在应用内公布，继续使用本应用即表示您同意新的协议。")
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = Color(0xFFF0EEFF)
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFB8B6D0),
        lineHeight = 20.sp
    )
}
