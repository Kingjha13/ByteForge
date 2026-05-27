package com.javacompiler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.javacompiler.interpreter.InterpreterResult
import com.javacompiler.interpreter.JavaInterpreter
import com.javacompiler.ui.theme.AccentBlue
import com.javacompiler.ui.theme.AccentGreen
import com.javacompiler.ui.theme.AccentRed
import com.javacompiler.ui.theme.EditorBackground
import com.javacompiler.ui.theme.EditorBorder
import com.javacompiler.ui.theme.EditorComment
import com.javacompiler.ui.theme.EditorSurface
import com.javacompiler.ui.theme.EditorText
import com.javacompiler.ui.theme.JavaCompilerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JavaCompilerTheme {
                JavaCompilerScreen()
            }
        }
    }
}

@Composable
fun JavaCompilerScreen() {
    val starterCode = """public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
        System.out.println("Welcome to my Java Compiler!");
        System.out.println(42);
    }
}"""

    var sourceCode by remember { mutableStateOf(starterCode) }
    var result     by remember { mutableStateOf<InterpreterResult?>(null) }
    val interpreter = remember { JavaInterpreter() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopBar(onRun = { result = interpreter.run(sourceCode) })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CodeEditor(
                code = sourceCode,
                onCodeChange = { sourceCode = it },
                modifier = Modifier.weight(0.6f)
            )

            OutputPanel(
                result = result,
                modifier = Modifier.weight(0.4f)
            )
        }
    }
}

@Composable
fun TopBar(onRun: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EditorSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Java Compiler",
                color = AccentBlue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Phase 1 — Print Statements",
                color = EditorComment,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Button(
            onClick = onRun,
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "▶  Run",
                color = EditorBackground,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
    }

    Divider(color = EditorBorder, thickness = 1.dp)
}

@Composable
fun CodeEditor(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lines = code.split("\n")

    Column(modifier = modifier) {
        Text(
            text = "  Main.java",
            color = EditorComment,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .background(EditorSurface)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(EditorBackground)
                .border(1.dp, EditorBorder, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
        ) {
            Column(
                modifier = Modifier
                    .background(EditorSurface)
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                lines.forEachIndexed { index, _ ->
                    Text(
                        text = "${index + 1}",
                        color = EditorComment,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }

            val scrollState = rememberScrollState()
            BasicTextField(
                value = code,
                onValueChange = onCodeChange,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(12.dp),
                textStyle = TextStyle(
                    color = EditorText,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(AccentBlue)
            )
        }
    }
}

@Composable
fun OutputPanel(
    result: InterpreterResult?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EditorSurface)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Output",
                color = EditorComment,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            when {
                result == null  -> StatusBadge("READY",  AccentBlue)
                result.hasError()  -> StatusBadge("ERROR",  AccentRed)
                else               -> StatusBadge("OK",     AccentGreen)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(Color(0xFF010409))
                .border(1.dp, EditorBorder, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                result == null -> {
                    Text(
                        text = "// Press ▶ Run to execute your code",
                        color = EditorComment,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
                result.hasError() -> {
                    Text(
                        text = result.getFormattedError(),
                        color = AccentRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
                else -> {
                    Text(
                        text = result.outputText.ifEmpty { "(no output)" },
                        color = AccentGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}
