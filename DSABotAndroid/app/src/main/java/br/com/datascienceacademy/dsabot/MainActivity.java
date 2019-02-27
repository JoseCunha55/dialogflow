package br.com.datascienceacademy.dsabot;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;

public class MainActivity extends AppCompatActivity implements AIListener {

    private static final String CLIENT_ACCESS_TOKEN = "INSIRA_SUA_CHAVE_AQUI";
    private static final int MY_PERMISSIONS_REQUEST_CODE_RECORD_AUDIO = 10000;

    private EditText consultaEditText;
    private TextView resultadoTextView;

    private AIService aiService;

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        consultaEditText = findViewById(R.id.consulta_edittext);
        resultadoTextView = findViewById(R.id.resultado_textview);

        final AIConfiguration config = new AIConfiguration(CLIENT_ACCESS_TOKEN,
                AIConfiguration.SupportedLanguages.PortugueseBrazil,
                AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(this, config);

        aiService.setListener(this);

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                tts.setLanguage(new Locale("pt-BR"));
            }
        });
    }

    public void consultar(View v) {
        // Toast.makeText(MainActivity.this, "Olá DSA", Toast.LENGTH_SHORT).show();
        //resultadoTextView.setText(consultaEditText.getText().toString());

        if(consultaEditText.getText().toString().trim().equals("")) {
            Toast.makeText(this,"Digite algo",Toast.LENGTH_SHORT).show();
            return;
        }

        final AIRequest aiRequest = new AIRequest();
        aiRequest.setQuery(consultaEditText.getText().toString());

        new AsyncTask<AIRequest, Void, AIResponse>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                resultadoTextView.setText("Processando");
            }

            @Override
            protected AIResponse doInBackground(AIRequest... requests) {
                final AIRequest request = requests[0];
                try {
                    final AIResponse response = aiService.textRequest(aiRequest);
                    return response;
                } catch (AIServiceException e) {
                }
                return null;
            }
            @Override
            protected void onPostExecute(AIResponse aiResponse) {
                if (aiResponse != null) {
                   resultadoTextView.setText(aiResponse.getResult().getFulfillment().getSpeech());
                }
                else {
                    resultadoTextView.setText("Ocorreu um erro");
                }

            }
        }.execute(aiRequest);
    }

    @Override
    public void onResult(AIResponse result) {
        if(result != null && !result.isError()) {

            String resposta = result.getResult().getFulfillment().getSpeech();

            resultadoTextView.setText(resposta);

            if(Build.VERSION.SDK_INT >= 21 && tts != null) {
                tts.speak(resposta, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        }
    }

    @Override
    public void onError(AIError error) {
        resultadoTextView.setText("Erro: " + error.getMessage());
    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }

    public void consultarVoz(View v) {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {

                AlertDialog.Builder janela = new AlertDialog.Builder(MainActivity.this);
                janela.setTitle("Ação requerida");
                janela.setMessage("Conceda a permissão na próxima tela para que possamos utilizar seu microfone");
                janela.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[] { Manifest.permission.RECORD_AUDIO },
                                MY_PERMISSIONS_REQUEST_CODE_RECORD_AUDIO);
                    }
                });

                janela.show();

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.RECORD_AUDIO },
                        MY_PERMISSIONS_REQUEST_CODE_RECORD_AUDIO);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else {
            aiService.startListening();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == MY_PERMISSIONS_REQUEST_CODE_RECORD_AUDIO) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                aiService.startListening();
            }
        }
    }
}
