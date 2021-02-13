import androidx.compose.desktop.AppManager
import androidx.compose.desktop.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.KeyStroke
import androidx.compose.ui.window.Menu
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuItem
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

fun main() {
    val text = mutableStateOf("")
    val currentFile = mutableStateOf<File?>(null)
    val modified = mutableStateOf(false)
    val suspendedFunction = mutableStateOf<(() -> Unit)?>(null)

    // :: クラスリテラル
    fun new() {
        if (modified.value) {
            suspendedFunction.value = ::new
            return
        }
        text.value = ""
        modified.value = false
    }

    fun open() {
        println("open - value : ${modified.value}")
        if (modified.value) {
            suspendedFunction.value = ::open
            return
        }

        val parentComponent = AppManager.focusedWindow?.window
        val fileChooser = JFileChooser()
        fileChooser.fileFilter = FileNameExtensionFilter("テキストファイル", "txt")

        val result = fileChooser.showOpenDialog(parentComponent)
        println("open - result : $result")

        if (result == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            text.value = file.readText(Charsets.UTF_8)
            currentFile.value = file
            modified.value = false
        }
    }

    fun saveAs() {
        val parentComponent = AppManager.focusedWindow?.window // フォーカス中のUIコンポーネントを返却する
        val fileChooser = JFileChooser() // ← swingのコンポーネント

        val fileExtensionFilter = "txt"
        fileChooser.fileFilter = FileNameExtensionFilter("テキストファイル", fileExtensionFilter)
        fileChooser.selectedFile = currentFile.value  // デフォルトファイル名

        val result = fileChooser.showSaveDialog(parentComponent)
        println("saveAs - result : $result")

        if (result == JFileChooser.APPROVE_OPTION) {

            // 拡張子が付いてなかったら足す
            var selectedFile = fileChooser.selectedFile
            if (selectedFile.toString().substring(selectedFile.toString().length - 4) != ".$fileExtensionFilter") {
                selectedFile = File(selectedFile.absolutePath + selectedFile.name + ".txt")
            }

            selectedFile.writeText(text.value)
            modified.value = false
            currentFile.value = selectedFile
        }
    }

    fun save() {
        val file = currentFile.value
        if (file != null) {
            file.writeText(text.value)
            modified.value = false
        } else {
            saveAs()
        }
    }

    // 編集中に他のファイルを開いたりしたときの警告ダイアログ
    @Composable
    fun showAlertDialog() {
        if (suspendedFunction.value != null) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {
                    Button(onClick = {
                        save()
                        suspendedFunction.value?.invoke()
                        suspendedFunction.value = null
                    }) { Text("はい") }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            modified.value = false
                            suspendedFunction.value?.invoke()
                            suspendedFunction.value = null
                        }
                    ) {
                        Text("いいえ")
                    }
                },
                text = { Text("保存しますか？") }
            )
        }
    }

    // メニューバーの定義
    val menuBar = MenuBar(
        Menu(
            name = "ファイル",
            item = arrayOf(
                MenuItem(
                    name = "新規作成",
                    onClick = { new() },
                    shortcut = KeyStroke(Key.N)
                ),
                MenuItem(
                    name = "開く", shortcut = KeyStroke(Key.O),
                    onClick = {
                        open()
                    }
                ),
                MenuItem(name = "名前を付けて保存",
                    onClick = { saveAs() }),
                MenuItem(name = "上書き保存",
                    shortcut = KeyStroke(Key.S),
                    onClick = {
                        save()
                    })
            )
        )
    )

    Window(title = "メモ帳", menuBar = menuBar) {
        MaterialTheme {
            showAlertDialog()
            TextField(
                value = text.value,
                onValueChange = {
                    text.value = it
                    modified.value = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(red = 82, green = 189, blue = 172)),
                label = { Text("") },
                textStyle = TextStyle(fontSize = 24.sp)
            )
        }
    }
}