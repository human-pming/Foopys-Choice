package com.facebook.witai.foopyschoice;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class Snowy3 extends AppCompatActivity {
    private TextView speechTranscription;
    private TextToSpeech textToSpeech;
    private OkHttpClient httpClient;
    private HttpUrl.Builder httpBuilder;
    private Request.Builder httpRequestBuilder;
    private AudioRecord recorder;
    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, AUDIO_FORMAT) * 10;
    private static final AtomicBoolean recordingInProgress = new AtomicBoolean(false);
    private Thread recordingThread;
    private ImageButton sat;
    private ImageButton dissat;

    /* Go to your Wit.ai app Management > Settings and obtain the Client Access Token */
    private static final String CLIENT_ACCESS_TOKEN = "QRJTI2MIEYI5CUOKJZ5WWZTJY2VBWT2M";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snowy3);

        Button button1=(Button) findViewById(R.id.videoButton);
        button1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=FRNcmhvdUGA"));
                startActivity(intent);

            }
        });
        sat = findViewById(R.id.sat);
        dissat = findViewById(R.id.dissat);

        // Initialize HTTP Client
        initializeHttpClient();

        // Initialize TextToSpeech
        initializeTextToSpeech(this.getApplicationContext());

        sat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textToSpeech.speak("Have a nice day with a diet suitable for today's weather. This was the chatbot Foopy recommending a meal.", TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
            }
        });
        dissat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textToSpeech.speak("If you're dissatisfied with your recommended diet, get a new one!", TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
                //dialog 등장
                show();

            }
        });

    }
    public void show()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Do you like the diet I recommended?");
        builder.setMessage("If you are dissatisfied with your recommended diet, get a new one!\n");
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which){
                    case DialogInterface.BUTTON_POSITIVE:
                        // User clicked the Yes button
                        //random하게 식단 다시 intent로 출력
                        Intent intent = new Intent(Snowy3.this , Snowy4.class);
                        startActivity(intent); // 액티비티 이동.
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        // User clicked the No button
                        textToSpeech.speak("Have a nice day with a diet suitable for today's weather. This was the chatbot Foopy recommending a meal.", TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
                        break;
                }
            }
        };
        builder.setPositiveButton("Yes",dialogClickListener);
        builder.setNegativeButton("No",dialogClickListener);
        builder.show();
    }

    private void initializeHttpClient() {
        httpClient = new OkHttpClient();
        httpBuilder = HttpUrl.parse("https://api.wit.ai/speech").newBuilder();
        httpBuilder.addQueryParameter("v", "20200805");
        httpRequestBuilder = new Request.Builder()
                .url(httpBuilder.build())
                .header("Authorization", "Bearer " + CLIENT_ACCESS_TOKEN)
                .header("Content-Type", "audio/raw")
                .header("Transfer-Encoding", "chunked");
    }

    private void initializeTextToSpeech(Context applicationContext) {
        textToSpeech = new TextToSpeech(applicationContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int ttsStatus) {
                // Disable the speakButton and provide the status of app while waiting for TextToSpeech to initialize
                if (ttsStatus == TextToSpeech.SUCCESS) {
                    if (CLIENT_ACCESS_TOKEN == "<YOUR CLIENT ACCESS TOKEN>") {
                        textToSpeech.speak("Hi! Before we start the demo. Please set the client access token.", TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
                    }else {
                        textToSpeech.speak("This is Foopy's recommended diet in snowy and cold weather. This diet keeps your body warm.", TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
                    }
                }
            }
        });
    }

}
//tts로 발화문 말하기
//textToSpeech.speak("I'm sorry. Please say it again.", TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());

