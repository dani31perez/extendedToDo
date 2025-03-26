package com.example.extendedtodo
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.extendedtodo.ui.theme.ExtendedToDoTheme
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
            ExtendedToDoTheme  {
                ToDoApp()
            }
        }
    }
}

@Composable
fun ToDoApp() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Solicitar permisos en el inicio
    LaunchedEffect (Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    //Estaods necesarios para editar y añadir tasks
    var currentToDo by remember { mutableStateOf("") }
    val toDoList = remember { mutableStateListOf<Task>() }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var editedImageUri by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var editedTask by remember { mutableStateOf<Task>(Task("","")) }
    var currentTitle by remember { mutableStateOf("") }

    // Creamos los launcher para añadir imagenes
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) {
            uri -> if(uri!= null){
        selectedImageUri = uri.toString()
    } else {
        selectedImageUri = null
    }
    }
    val editedImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) {
            uri -> if(uri!= null){
        editedImageUri = uri.toString()
    } else {
        editedImageUri = null
    }
    }
    //Llamamos al ToDoScreen con los params necesarios
    ToDoScreen (
        selectedImageUri = selectedImageUri,
        currentToDo = currentToDo,
        toDoList = toDoList,
        imagePickerLauncher = imagePickerLauncher,
        onValueChange = { currentToDo = it },
        //Funcion para añadir, si está vacío el titulo lanza un Toast
        onAddToDo = {
            if(currentToDo.isNotEmpty()) {
                toDoList.add(Task(currentToDo, selectedImageUri))
                currentToDo = ""
                selectedImageUri = null
            }else {
                Toast.makeText(context, "You cannot add a task with an empty title", Toast.LENGTH_SHORT).show()
            }
        },
        onRemoveToDo = {task -> toDoList.remove(task)},
        //Cambiamos showDialog para poder ver el OpenDialog
        onOpenDialog = {
                task ->
            editedTask = task
            currentTitle = task.title
            editedImageUri = task.imageUri
            showDialog = true
        },
    )

    if(showDialog){
        //Llamamos a OpenDialog con los params necesarios
        OpenDialog(
            currentTitle = currentTitle,
            onTitleChange = { currentTitle = it},
            editedImageUri = editedImageUri,
            editedImagePickerLauncher = editedImagePickerLauncher,
            //Si apacha cancel se cierra el OpenDialog
            onCloseDialog = {showDialog = false},
            // Si apacha save se edita el task, si el titulo es vacío lanza un toast
            onEditToDo = {
                if(currentTitle.isNotEmpty()) {
                    val index = toDoList.indexOf(editedTask)
                    if(editedImageUri != null && editedImageUri.toString().isNotBlank()){
                        toDoList[index] = Task(currentTitle, editedImageUri)
                    } else {
                        toDoList[index] = Task(currentTitle, editedTask.imageUri)
                    }
                    showDialog = false
                    editedImageUri = null
                } else {
                    Toast.makeText(context, "You cannot edit a task with an empty title", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun OpenDialog(
    currentTitle: String,
    onTitleChange: (String) -> Unit,
    editedImageUri: String?,
    editedImagePickerLauncher: ActivityResultLauncher<String>?,
    onCloseDialog: () -> Unit,
    onEditToDo: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(30.dp, 20.dp)){
                Text(
                    text = "Edit Task",
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedTextField(
                    value = currentTitle,
                    onValueChange = {onTitleChange(it)},
                    label = { Text("Task") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .padding(0.dp,16.dp)
                )
                Row {
                    Button(onClick = { editedImagePickerLauncher?.launch("image/*")},
                        modifier = Modifier.padding(0.dp,16.dp)) {
                        Text("Select Image")
                    }
                    editedImageUri?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .align(Alignment.CenterVertically)

                        )
                    }
                }


                Row (
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ){
                    TextButton(onClick = {onCloseDialog()}) {
                        Text("Cancel")
                    }

                    TextButton(onClick = {onEditToDo()}) {
                        Text("Save")
                    }
                }
            }

        }
    }
}

@Composable
fun ToDoScreen(
    selectedImageUri: String?,
    currentToDo: String,
    toDoList: List<Task>,
    imagePickerLauncher: ActivityResultLauncher<String>?,
    onValueChange: (String) -> Unit,
    onAddToDo: () -> Unit,
    onRemoveToDo: (Task) -> Unit,
    onOpenDialog: (Task) -> Unit
)
{

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Todo List",
            style = MaterialTheme.typography.headlineMedium,
        )
        OutlinedTextField(
            value = currentToDo,
            onValueChange = {onValueChange(it)},
            label = { Text("New item") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Row {
                Button(onClick = { imagePickerLauncher?.launch("image/*")}){
                    Text("Pick Image")
                }
                selectedImageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                    )
                }
            }

            Button(onClick = {onAddToDo()}){
                Text("AddTask")
            }
        }
        LazyColumn {
            items(toDoList) { toDo ->
                TodoItem(toDo = toDo, onRemoveToDo = onRemoveToDo, onOpenDialog = onOpenDialog)
            }
        }
    }
}

@Composable
fun TodoItem (toDo: Task, onRemoveToDo: (Task) -> Unit, onOpenDialog: (Task) -> Unit){
    Card (
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row (
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterVertically)
            ){
                toDo.imageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                    )
                }
                Text(text = toDo.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.CenterVertically))
            }

            Row (modifier = Modifier.align(Alignment.CenterVertically)){
                IconButton(onClick = {onOpenDialog(toDo)}) { Icon(imageVector = Icons.Filled.Edit, contentDescription = "") }
                IconButton(onClick = {onRemoveToDo(toDo)}) { Icon(imageVector = Icons.Filled.Delete, contentDescription = "" ) }
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun ToDoPreview() {
    ExtendedToDoTheme {
        ToDoScreen(
            selectedImageUri = null,
            currentToDo = "",
            toDoList = listOf(),
            imagePickerLauncher = null,
            onValueChange = {},
            onAddToDo = {},
            onRemoveToDo = {},
            onOpenDialog = {}
        )
    }
}

data class Task(val title: String, val imageUri: String?)