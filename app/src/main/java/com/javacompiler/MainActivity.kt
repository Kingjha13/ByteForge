package com.javacompiler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.javacompiler.interpreter.InterpreterResult
import com.javacompiler.interpreter.JavaInterpreter
import com.javacompiler.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JavaCompilerTheme { CompilerApp() } }
    }
}

private enum class Tab { EDITOR, OUTPUT }


private val ColKeyword   = Color(0xFF569CD6)
private val ColType      = Color(0xFF4EC9B0)
private val ColString    = Color(0xFFCE9178)
private val ColNumber    = Color(0xFFB5CEA8)
private val ColComment   = Color(0xFF6A9955)
private val ColBracket   = Color(0xFFFFD700)
private val ColOperator  = Color(0xFFD4D4D4)
private val ColDefault   = Color(0xFFD4D4D4)
private val ColAnnotation= Color(0xFF9CDCFE)

private val KEYWORDS = setOf(
    "abstract","assert","break","case","catch","class","const","continue",
    "default","do","else","enum","extends","final","finally","for","goto",
    "if","implements","import","instanceof","interface","native","new",
    "package","private","protected","public","return","static","strictfp",
    "super","switch","synchronized","this","throw","throws","transient","try",
    "volatile","while","null","true","false","void","var"
)
private val TYPES = setOf(
    "int","long","double","float","boolean","char","byte","short",
    "String","Integer","Long","Double","Float","Boolean","Character",
    "Byte","Short","Object","System","Math","Arrays","ArrayList","List",
    "Map","HashMap","Set","HashSet","StringBuilder","Scanner"
)

fun buildHighlightedText(code: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < code.length) {
        if (i + 1 < code.length && code[i] == '/' && code[i + 1] == '/') {
            val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
            withStyle(SpanStyle(color = ColComment)) { append(code.substring(i, end)) }
            i = end
            continue
        }
        if (i + 1 < code.length && code[i] == '/' && code[i + 1] == '*') {
            val end = code.indexOf("*/", i + 2).let { if (it == -1) code.length else it + 2 }
            withStyle(SpanStyle(color = ColComment)) { append(code.substring(i, end)) }
            i = end
            continue
        }
        if (code[i] == '"') {
            var j = i + 1
            while (j < code.length && !(code[j] == '"' && code[j - 1] != '\\')) j++
            j = minOf(j + 1, code.length)
            withStyle(SpanStyle(color = ColString)) { append(code.substring(i, j)) }
            i = j
            continue
        }
        if (code[i] == '\'') {
            var j = i + 1
            while (j < code.length && !(code[j] == '\'' && code[j - 1] != '\\')) j++
            j = minOf(j + 1, code.length)
            withStyle(SpanStyle(color = ColString)) { append(code.substring(i, j)) }
            i = j
            continue
        }
        if (code[i].isDigit() || (code[i] == '.' && i + 1 < code.length && code[i + 1].isDigit())) {
            var j = i
            while (j < code.length && (code[j].isDigit() || code[j] == '.' || code[j] == 'f'
                        || code[j] == 'L' || code[j] == 'l' || code[j] == 'x'
                        || code[j] in 'a'..'f' || code[j] in 'A'..'F')) j++
            withStyle(SpanStyle(color = ColNumber)) { append(code.substring(i, j)) }
            i = j
            continue
        }
        if (code[i] in "()[]{}") {
            withStyle(SpanStyle(color = ColBracket)) { append(code[i]) }
            i++
            continue
        }
        if (code[i].isLetter() || code[i] == '_' || code[i] == '@') {
            var j = i
            if (code[j] == '@') j++
            while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '_')) j++
            val word = code.substring(i, j)
            val bare = if (word.startsWith("@")) word.substring(1) else word
            val color = when {
                word.startsWith("@")  -> ColAnnotation
                bare in KEYWORDS      -> ColKeyword
                bare in TYPES         -> ColType
                j < code.length && code[j] == '(' -> ColAnnotation
                else -> ColDefault
            }
            withStyle(SpanStyle(color = color)) { append(word) }
            i = j
            continue
        }
        if (code[i] in "+-*/%=<>!&|^~?:;,.") {
            withStyle(SpanStyle(color = ColOperator)) { append(code[i]) }
            i++
            continue
        }
        withStyle(SpanStyle(color = ColDefault)) { append(code[i]) }
        i++
    }
}

private data class KeyGroup(val label: String, val keys: List<String>)

private val SYMBOL_ROWS = listOf(
    listOf("{", "}", "(", ")", "[", "]", "<", ">"),
    listOf(";", ":", ".", ",", "=", "+", "-", "*", "/", "%"),
    listOf("\"", "'", "!", "&", "|", "^", "~", "?", "@", "\\n")
)

@Composable
fun CompilerApp() {
    val starter = """public class Main {
    public static void main(String[] args) {

        // ── Variables ─────────────────────────
        int    age    = 25;
        double pi     = 3.14;
        String name   = "Java";
        boolean flag  = true;

        // ── Arithmetic ────────────────────────
        int sum = age + 5;
        System.out.println("Name: " + name);
        System.out.println("Age:  " + age);
        System.out.println("Sum:  " + sum);
        System.out.println("Pi:   " + pi);

        // ── Array ─────────────────────────────
        int[] nums = {10, 20, 30, 40, 50};
        System.out.println("Array length: " + nums.length);
        System.out.println("First: " + nums[0]);

        // ── Loop ──────────────────────────────
        for (int i = 0; i < 3; i++) {
            System.out.println("i = " + i);
        }

        // ── String methods ────────────────────
        System.out.println(name.toUpperCase());
        System.out.println(name.length());
    }
}"""

    var textValue by remember { mutableStateOf(TextFieldValue(starter)) }
    var result    by remember { mutableStateOf<InterpreterResult?>(null) }
    var tab       by remember { mutableStateOf(Tab.EDITOR) }
    val interp    = remember { JavaInterpreter() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LeetCodeTopBar(
            activeTab = tab,
            onTabChange = { tab = it },
            onRun = {
                result = interp.run(textValue.text)
                tab = Tab.OUTPUT
            },
            onClear = { result = null }
        )

        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(initialAlpha = 0.3f) togetherWith fadeOut() },
            modifier = Modifier.weight(1f)
        ) { activeTab ->
            when (activeTab) {
                Tab.EDITOR -> CodeEditorPane(
                    textValue = textValue,
                    onChange = { textValue = it },
                    onInsert = { sym ->
                        val actual = if (sym == "\\n") "\n" else sym
                        val sel = textValue.selection
                        val newText = textValue.text.substring(0, sel.start) +
                                actual +
                                textValue.text.substring(sel.end)
                        val newCursor = sel.start + actual.length
                        textValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(newCursor)
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Tab.OUTPUT -> OutputPane(
                    result = result,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun LeetCodeTopBar(
    activeTab: Tab,
    onTabChange: (Tab) -> Unit,
    onRun: () -> Unit,
    onClear: () -> Unit
) {
    val SurfaceBg = Color(0xFF161B22)
    val Border    = Color(0xFF30363D)
    val TabActive = Color(0xFFF0883E)
    val TabIdle   = Color(0xFF8B949E)

    Column(modifier = Modifier.background(SurfaceBg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF0883E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("J", color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                }
                Column {
                    Text("JavaIDE",
                        color = Color(0xFFF0F6FC),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace)
                    Text("Lexer · Parser · AST · Eval",
                        color = Color(0xFF484F58),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (activeTab == Tab.OUTPUT) {
                    OutlinedButton(
                        onClick = onClear,
                        border = BorderStroke(1.dp, Border),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Clear", color = TabIdle,
                            fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
                Button(
                    onClick = onRun,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run",
                        tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Run", color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            listOf(Tab.EDITOR to "Code", Tab.OUTPUT to "Output").forEach { (t, label) ->
                val active = activeTab == t
                Column(
                    modifier = Modifier
                        .clickable { onTabChange(t) }
                        .padding(horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        label,
                        color = if (active) TabActive else TabIdle,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(bottom = 6.dp, top = 2.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(if (active) TabActive else Color.Transparent)
                    )
                }
            }
        }

        Divider(color = Border, thickness = 0.5.dp)
    }
}

@Composable
fun CodeEditorPane(
    textValue: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    onInsert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val BgEditor  = Color(0xFF0D1117)
    val BgGutter  = Color(0xFF0D1117)
    val ColGutter = Color(0xFF3D4451)
    val BorderCol = Color(0xFF21262D)
    val BgSymBar  = Color(0xFF161B22)

    val lines = textValue.text.split("\n")
    val sharedScroll = rememberScrollState()

    Column(modifier = modifier.background(BgEditor)) {
        Row(modifier = Modifier.weight(1f)) {

            Column(
                modifier = Modifier
                    .background(BgGutter)
                    .width(46.dp)
                    .fillMaxHeight()
                    .verticalScroll(sharedScroll)
                    .padding(top = 12.dp, bottom = 12.dp, end = 8.dp, start = 4.dp),
                horizontalAlignment = Alignment.End
            ) {
                lines.forEachIndexed { i, _ ->
                    Text(
                        text = "${i + 1}",
                        color = ColGutter,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(BorderCol))

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                BasicTextField(
                    value = textValue,
                    onValueChange = onChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(sharedScroll)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    textStyle = TextStyle(
                        // Transparent text — we render highlighted text via visualTransformation below
                        color = Color.Transparent,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 21.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFFF0883E)),
                    visualTransformation = {
                        TransformedText(
                            buildHighlightedText(textValue.text),
                            OffsetMapping.Identity
                        )
                    }
                )
            }
        }

        SymbolKeyboard(
            onKey = onInsert,
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSymBar)
        )
    }
}


@Composable
fun SymbolKeyboard(onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    val BgKey    = Color(0xFF21262D)
    val BgBrace  = Color(0xFF1C2D3A)
    val ColKey   = Color(0xFFCDD9E5)
    val Border   = Color(0xFF30363D)

    Column(
        modifier = modifier
            .border(BorderStroke(0.5.dp, Border))
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SYMBOL_ROWS.forEach { row ->
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(row) { sym ->
                    val isBracket = sym in listOf("{", "}", "(", ")", "[", "]")
                    val keyBg = if (isBracket) BgBrace else BgKey
                    val keyColor = if (isBracket) Color(0xFFFFD700) else ColKey
                    val display = if (sym == "\\n") "↵" else sym

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(keyBg)
                            .border(0.5.dp, Border, RoundedCornerShape(5.dp))
                            .clickable { onKey(sym) }
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            display,
                            color = keyColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OutputPane(result: InterpreterResult?, modifier: Modifier = Modifier) {
    val BgOutput = Color(0xFF0D1117)
    val Green    = Color(0xFF3FB950)
    val Red      = Color(0xFFF85149)
    val Idle     = Color(0xFF484F58)

    Column(
        modifier = modifier
            .background(BgOutput)
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            val (statusColor, statusText) = when {
                result == null     -> Idle    to "No output yet — press Run ▶"
                result.hasError()  -> Red     to "Runtime / Compile Error"
                else               -> Green   to "Execution Successful"
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(statusColor))
                Text(statusText, color = statusColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Divider(color = Color(0xFF21262D), thickness = 0.5.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when {
                result == null -> {
                    Text(
                        "// Output will appear here after execution",
                        color = Idle,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 22.sp
                    )
                }
                result.hasError() -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Error card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1C0A09))
                                .border(1.dp, Red.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                result.formattedError,
                                color = Red,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF010409))
                            .border(1.dp, Color(0xFF21262D), RoundedCornerShape(8.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            result.outputText.ifEmpty { "(no output)" },
                            color = Color(0xFFD2D9E3),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}