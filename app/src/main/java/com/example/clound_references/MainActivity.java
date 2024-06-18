package com.example.clound_references;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;

    EditText searchFoldersEditText;
    FirebaseUser user;
    GridLayout folderContainer;
    String lastAddedFolder; // Almacena la última carpeta creada
    String lastDeletedFolder; // Almacena la última carpeta eliminada
    ArrayList<String> completeFolderList; // Arreglo que almacena la lista completa de carpetas
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        searchFoldersEditText = findViewById(R.id.search_folders);
        folderContainer = findViewById(R.id.folder_container);
        user = auth.getCurrentUser();
        textView = findViewById(R.id.cerrarS);
        completeFolderList = new ArrayList<>();

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                auth.signOut();
                Intent intentLogout = new Intent(MainActivity.this, login.class);
                startActivity(intentLogout);
                finish();
                Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
            }
        });

        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), login.class);
            startActivity(intent);
            finish();
        }

        searchFoldersEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before text changes
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchFolders(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed after text changes
            }
        });

        findViewById(R.id.navigation_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Home Selected", Toast.LENGTH_SHORT).show();
                showAddFolderDialog(); //Añadir carpeta.
            }
        });



        findViewById(R.id.navigation_communities).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Communities Selected", Toast.LENGTH_SHORT).show();
                showDeleteFolderDialog(); //eliminar carpeta.
            }
        });

    }

    private void showAddFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nombre de la nueva carpeta:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("ACEPTAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String folderName = input.getText().toString();
                if (!folderName.isEmpty()) {
                    addFolderButton(folderName);
                    lastAddedFolder = folderName; // Actualiza la última carpeta creada
                    updateCompleteFolderList();
                    Log.d("MainActivity", "Carpeta creada: " + folderName); // Imprime el nombre en la consola
                    printFolderLists(); // Imprime las listas de carpetas
                } else {
                    Toast.makeText(MainActivity.this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void showDeleteFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nombre de la carpeta a eliminar:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("ELIMINAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String folderName = input.getText().toString();
                if (completeFolderList.contains(folderName)) {
                    deleteFolderButton(folderName);
                    lastDeletedFolder = folderName; // Actualiza la última carpeta eliminada
                    updateCompleteFolderList();
                    Log.d("MainActivity", "Carpeta eliminada: " + folderName); // Imprime el nombre en la consola
                    printFolderLists(); // Imprime las listas de carpetas
                } else {
                    Toast.makeText(MainActivity.this, "Carpeta no encontrada", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void addFolderButton(String folderName) {
        View folderView = getLayoutInflater().inflate(R.layout.folder_item, null);

        ImageButton folderButton = folderView.findViewById(R.id.folder_button);
        TextView folderText = folderView.findViewById(R.id.folder_text);

        folderButton.setImageResource(R.drawable.carpeta); // Usa la imagen "carpeta.png"
        folderText.setText(folderName);

        folderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FolderActivity.class);
                intent.putExtra("folder_name", folderName);
                startActivity(intent);
            }
        });

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        folderView.setLayoutParams(params);

        folderContainer.addView(folderView);
    }

    private void deleteFolderButton(String folderName) {
        int childCount = folderContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = folderContainer.getChildAt(i);
            TextView folderText = view.findViewById(R.id.folder_text);
            if (folderText.getText().toString().equals(folderName)) {
                folderContainer.removeView(view);
                completeFolderList.remove(folderName); // Elimina la carpeta del arreglo completo
                return;
            }
        }
    }

    private void updateCompleteFolderList() {
        if (lastAddedFolder != null && !completeFolderList.contains(lastAddedFolder)) {
            completeFolderList.add(lastAddedFolder);
        }
        if (lastDeletedFolder != null) {
            completeFolderList.remove(lastDeletedFolder);
        }
    }

    private void printFolderLists() { //MOSTRAR LOS DATOS PARA MANDAR A LA BASE DE DATOS
        Log.d("MainActivity", "Last Added Folder: " + lastAddedFolder);
        Log.d("MainActivity", "Last Deleted Folder: " + lastDeletedFolder);
        Log.d("MainActivity", "Complete Folder List: " + completeFolderList);
    }

    private void searchFolders(String query) {
        folderContainer.removeAllViews(); // Elimina todas las vistas antes de mostrar las buscadas
        for (String folder : completeFolderList) {
            if (folder.toLowerCase().contains(query.toLowerCase())) {
                addFolderButton(folder); // Agrega solo las carpetas que coincidan con la búsqueda
            }
        }
    }

    public String getLastAddedFolder() {
        return lastAddedFolder;
    }

    public String getLastDeletedFolder() {
        return lastDeletedFolder;
    }

    public ArrayList<String> getCompleteFolderList() {
        return completeFolderList;
    }
}
