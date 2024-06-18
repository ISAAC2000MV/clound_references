package com.example.clound_references;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;

public class FolderActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;

    private static final String PREFS_NAME = "permissions_prefs";
    private static final String PREFS_FILES_PERMISSION = "files_permission";
    private static final String PREFS_CAMERA_PERMISSION = "camera_permission";

    TextView folderNameTextView;
    LinearLayout pdfContainer, imageContainer, linkContainer;
    Bitmap currentPhoto;

    ArrayList<String> lastPdfData = new ArrayList<>();
    ArrayList<String> lastImageData = new ArrayList<>();
    ArrayList<String> lastLinkData = new ArrayList<>();
    ArrayList<String> allPdfData = new ArrayList<>();
    ArrayList<String> allImageData = new ArrayList<>();
    ArrayList<String> allLinkData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder);

        folderNameTextView = findViewById(R.id.folder_name);
        pdfContainer = findViewById(R.id.pdf_container);
        imageContainer = findViewById(R.id.image_container);
        linkContainer = findViewById(R.id.link_container);

        String folderName = getIntent().getStringExtra("folder_name");
        if (folderName != null) {
            folderNameTextView.setText(folderName);
        }

        findViewById(R.id.navigation_home).setOnClickListener(v -> Toast.makeText(FolderActivity.this, "Home Selected", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navigation_collections).setOnClickListener(v -> Toast.makeText(FolderActivity.this, "Collections Selected", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navigation_add).setOnClickListener(v -> checkPermissionsAndOpenFilePicker());

        findViewById(R.id.navigation_camera).setOnClickListener(v -> openCamera());

        findViewById(R.id.navigation_communities).setOnClickListener(v -> Toast.makeText(FolderActivity.this, "Communities Selected", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navigation_logout).setOnClickListener(v -> {
            startActivity(new Intent(FolderActivity.this, login.class));
            finish();
        });

        findViewById(R.id.navigation_links).setOnClickListener(v -> showLinkDialog());

        requestPermissions();
    }

    private void showLinkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar Link");
        builder.setItems(new CharSequence[]{"Escanear QR", "Ingresar manualmente"}, (dialog, which) -> {
            if (which == 0) {
                IntentIntegrator integrator = new IntentIntegrator(FolderActivity.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                integrator.setPrompt("Escanear código QR");
                integrator.setCameraId(0);
                integrator.setBeepEnabled(false);
                integrator.setBarcodeImageEnabled(false);
                integrator.initiateScan();
            } else if (which == 1) {
                showManualLinkDialog();
            }
        });
        builder.show();
    }

    private void showManualLinkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ingresar Link Manualmente");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText titleInput = new EditText(this);
        titleInput.setHint("Título");
        layout.addView(titleInput);

        final EditText linkInput = new EditText(this);
        linkInput.setHint("Link");
        layout.addView(linkInput);

        builder.setView(layout);

        builder.setPositiveButton("Aceptar", (dialog, which) -> {
            String title = titleInput.getText().toString();
            String link = linkInput.getText().toString();
            if (!title.isEmpty() && !link.isEmpty()) {
                addLinkToContainer(title, link);
                lastLinkData.clear();
                lastLinkData.add(link);
                updateAllLinkData();
                printData();
            } else {
                Toast.makeText(FolderActivity.this, "El título y el link no pueden estar vacíos", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                showTitleDialog(uri);
            }
        } else if (requestCode == REQUEST_PERMISSIONS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    savePermissionStatus(PREFS_FILES_PERMISSION, true);
                    openFilePicker();
                } else {
                    Toast.makeText(this, "Permisos de almacenamiento denegados", Toast.LENGTH_SHORT).show();
                    openAppSettings();
                }
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            currentPhoto = (Bitmap) extras.get("data");
            showPhotoTitleDialog();
        } else {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_LONG).show();
                } else {
                    showTitleDialogForLink(result.getContents());
                }
            }
        }
    }

    private void showTitleDialogForLink(String link) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Título del link:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Aceptar", (dialog, which) -> {
            String title = input.getText().toString();
            if (!title.isEmpty()) {
                addLinkToContainer(title, link);
                lastLinkData.clear();
                lastLinkData.add(link);
                updateAllLinkData();
                printData();
            } else {
                Toast.makeText(FolderActivity.this, "El título no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addLinkToContainer(String title, String link) {
        View linkItem = getLayoutInflater().inflate(R.layout.file_item, null);

        ImageView fileIcon = linkItem.findViewById(R.id.file_icon);
        TextView fileTitle = linkItem.findViewById(R.id.file_title);

        fileIcon.setImageResource(android.R.drawable.ic_menu_share);
        fileTitle.setText(title);

        linkItem.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            startActivity(browserIntent);
        });

        linkContainer.addView(linkItem);
    }

    private void checkPermissionsAndOpenFilePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                openFilePicker();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                startActivityForResult(intent, REQUEST_PERMISSIONS);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
            } else {
                openFilePicker();
            }
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "image/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_CAPTURE);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void showTitleDialog(Uri uri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Título del archivo:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("ACEPTAR", (dialog, which) -> {
            String title = input.getText().toString();
            if (!title.isEmpty()) {
                addFileToContainer(uri, title);
                String mimeType = getContentResolver().getType(uri);
                if (mimeType != null && mimeType.startsWith("application/pdf")) {
                    lastPdfData.clear();
                    lastPdfData.add(uri.toString());
                    updateAllPdfData();
                } else if (mimeType != null && mimeType.startsWith("image/")) {
                    lastImageData.clear();
                    lastImageData.add(uri.toString());
                    updateAllImageData();
                }
                printData();
            } else {
                Toast.makeText(FolderActivity.this, "El título no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showPhotoTitleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Título de la foto:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("ACEPTAR", (dialog, which) -> {
            String title = input.getText().toString();
            if (!title.isEmpty()) {
                addPhotoToContainer(currentPhoto, title);
                lastImageData.clear();
                lastImageData.add(title);
                updateAllImageData();
                printData();
            } else {
                Toast.makeText(FolderActivity.this, "El título no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addPhotoToContainer(Bitmap photo, String title) {
        View fileItem = getLayoutInflater().inflate(R.layout.file_item, null);

        ImageView fileIcon = fileItem.findViewById(R.id.file_icon);
        TextView fileTitle = fileItem.findViewById(R.id.file_title);

        fileIcon.setImageResource(android.R.drawable.ic_menu_gallery);
        fileTitle.setText(title);

        fileItem.setOnClickListener(v -> showImage(photo));

        imageContainer.addView(fileItem);
    }

    private void addFileToContainer(Uri uri, String title) {
        String mimeType = getContentResolver().getType(uri);
        LinearLayout container;

        if (mimeType != null && mimeType.startsWith("application/pdf")) {
            container = pdfContainer;
        } else if (mimeType != null && mimeType.startsWith("image/")) {
            container = imageContainer;
        } else {
            Toast.makeText(this, "Tipo de archivo no soportado", Toast.LENGTH_SHORT).show();
            return;
        }

        View fileItem = getLayoutInflater().inflate(R.layout.file_item, null);

        ImageView fileIcon = fileItem.findViewById(R.id.file_icon);
        TextView fileTitle = fileItem.findViewById(R.id.file_title);

        fileIcon.setImageResource(mimeType.startsWith("application/pdf") ? android.R.drawable.ic_menu_agenda : android.R.drawable.ic_menu_gallery);
        fileTitle.setText(title);

        fileItem.setOnClickListener(v -> openFile(uri, mimeType));

        container.addView(fileItem);
    }

    private void openFile(Uri uri, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void showImage(Bitmap photo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Imagen");

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(photo);
        builder.setView(imageView);

        builder.setPositiveButton("CERRAR", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS || requestCode == REQUEST_IMAGE_CAPTURE) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                savePermissionStatus(PREFS_FILES_PERMISSION, true);
                openFilePicker();
            } else {
                Toast.makeText(this, "Permisos de almacenamiento denegados", Toast.LENGTH_SHORT).show();
                openAppSettings();
            }
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void requestPermissions() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean filesPermissionGranted = preferences.getBoolean(PREFS_FILES_PERMISSION, false);
        boolean cameraPermissionGranted = preferences.getBoolean(PREFS_CAMERA_PERMISSION, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!filesPermissionGranted) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                    startActivityForResult(intent, REQUEST_PERMISSIONS);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, REQUEST_PERMISSIONS);
                }
            }
        } else {
            if (!filesPermissionGranted || !cameraPermissionGranted) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                }, REQUEST_PERMISSIONS);
            }
        }
    }

    private void savePermissionStatus(String key, boolean status) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, status);
        editor.apply();
    }

    private void updateAllPdfData() {
        if (!lastPdfData.isEmpty() && !allPdfData.contains(lastPdfData.get(0))) {
            allPdfData.addAll(lastPdfData);
        }
    }

    private void updateAllImageData() {
        if (!lastImageData.isEmpty() && !allImageData.contains(lastImageData.get(0))) {
            allImageData.addAll(lastImageData);
        }
    }

    private void updateAllLinkData() {
        if (!lastLinkData.isEmpty() && !allLinkData.contains(lastLinkData.get(0))) {
            allLinkData.addAll(lastLinkData);
        }
    }

    private void printData() {
        Log.d("FolderActivity", "Last PDF Data: " + lastPdfData);
        Log.d("FolderActivity", "Last Image Data: " + lastImageData);
        Log.d("FolderActivity", "Last Link Data: " + lastLinkData);
        Log.d("FolderActivity", "All PDF Data: " + allPdfData);
        Log.d("FolderActivity", "All Image Data: " + allImageData);
        Log.d("FolderActivity", "All Link Data: " + allLinkData);
    }
}
