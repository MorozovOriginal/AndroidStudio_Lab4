package com.example.lab4;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;

public class MainActivity extends AppCompatActivity {

    private Spinner spinnerQueryType;
    private Button downloadButton;
    private Button viewButton;
    private File downloadedFile;
    private PopupWindow popupWindow;
    private String selectedJournalNumber; // Переменная для хранения выбранного номера журнала

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация элементов интерфейса
        spinnerQueryType = findViewById(R.id.spinnerQueryType);
        downloadButton = findViewById(R.id.downloadButton);
        viewButton = findViewById(R.id.viewButton);

        // Настройка Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.journal_numbers, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerQueryType.setAdapter(adapter);

        // Обработчик выбора элемента в Spinner
        spinnerQueryType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedJournalNumber = parent.getItemAtPosition(position).toString(); // Сохраняем выбранный номер
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не делаем
            }
        });

        // Отложенный вызов PopupWindow
        findViewById(R.id.mainLayout).post(this::showFullScreenPopup);

        // Обработчик кнопки скачивания
        downloadButton.setOnClickListener(v -> {
            if (selectedJournalNumber != null) {
                new DownloadFile().execute("https://ntv.ifmo.ru/file/journal/" + selectedJournalNumber + ".pdf");
            } else {
                Toast.makeText(MainActivity.this, "Выберите номер журнала!", Toast.LENGTH_SHORT).show();
            }
        });

        // Обработчик кнопки просмотра PDF
        viewButton.setOnClickListener(v -> {
            if (downloadedFile != null && downloadedFile.exists()) {
                openPdfFile();
            } else {
                Toast.makeText(MainActivity.this, "Ошибка!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Отображение PopupWindow на весь экран
    private void showFullScreenPopup() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_layout, null);

        // Настройка PopupWindow
        popupWindow = new PopupWindow(
                popupView,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, // Ширина
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, // Высота
                true // Фокус на PopupWindow
        );

        // Находим элементы внутри PopupWindow
        Button okButton = popupView.findViewById(R.id.okButton);

        // Обработчик кнопки "ОК"
        okButton.setOnClickListener(v -> popupWindow.dismiss());

        // Отображение PopupWindow
        popupWindow.showAtLocation(findViewById(R.id.mainLayout), Gravity.CENTER, 0, 0);
    }

    // Открытие скачанного PDF
    private void openPdfFile() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", downloadedFile);
        intent.setDataAndType(fileUri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    // Класс для асинхронного скачивания файла
    private class DownloadFile extends android.os.AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... f_url) {
            try {
                java.net.URL url = new java.net.URL(f_url[0]);
                java.net.URLConnection connection = url.openConnection();
                connection.connect();

                // Скачиваем файл
                java.io.InputStream input = new java.io.BufferedInputStream(url.openStream(), 8192);
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                downloadedFile = new File(dir, "journal.pdf");
                java.io.OutputStream output = new java.io.FileOutputStream(downloadedFile);

                byte[] data = new byte[1024];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

                return "Файл успешно скачан!";
            } catch (Exception e) {
                return "Ошибка: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String message) {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            if (downloadedFile != null && downloadedFile.exists()) {
                viewButton.setVisibility(View.VISIBLE); // Показываем кнопку для просмотра
            }
        }
    }
}