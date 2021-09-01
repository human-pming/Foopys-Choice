/* Copyright (c) Facebook, Inc. and its affiliates. */

package com.facebook.witai.voicedemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.net.Uri;


import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.Object;
import java.util.Random;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class MainActivity extends AppCompatActivity {
    private Button speakButton;
    private Button okayButton;
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
    private int ran;

    /* Go to your Wit.ai app Management > Settings and obtain the Client Access Token */
    private static final String CLIENT_ACCESS_TOKEN = "YLWNN3BC5KM2D6CWYR7DNBBXM6RE5R3T";
    private View view;

    //private static final int ran = (int)((Math.random()*10000)%3 +1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Button button1=(Button) findViewById(R.id.viewButton);
        button1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.accuweather.com/"));
                startActivity(intent);

            }
        });


        if (!checkPermissionsFromDevice()) requestPermissions();

        // Get a reference to the TextView and Button from the UI
        speechTranscription = findViewById(R.id.speechTranscription);
        speakButton = findViewById(R.id.speakButton);
        okayButton = findViewById(R.id.okayButton);

        // Initialize TextToSpeech
        initializeTextToSpeech(this.getApplicationContext());

        // Initialize HTTP Client
        initializeHttpClient();

        // Wire up speakButton to an onClickListener
        speakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!recordingInProgress.get()) {
                    startRecording();
                    speakButton.setText("Listening ...");
                    Log.d("speakButton", "Start listening ...");
                }  else {
                    stopRecording();
                    speakButton.setText("Speak");
                    Log.d("speakButton", "Stop listening ...");
                }
            }
        });


    }

    public static int RandomExample()
    {
        Random random = new Random(); //랜덤 객체 생성(디폴트 시드값 : 현재시간)
        random.setSeed(System.currentTimeMillis()); //시드값 설정을 따로 할수도 있음
        return (random.nextInt(4)+1);
    }

    public void viewButton (View view){
        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.accuweather.com/"));
        startActivity(intent);
    }

    // Processes the response from Speech API and responds to the user appropriately
    // See here for shape of the response: https://wit.ai/docs/http#get__message_link
    private void respondToUser(String response) {
        Log.v("respondToUser", response);
        String intentName = null;
        String speakerName = null;  //speakerName->weather
        String responseText = "";

        try {
            // Parse the intent name from the Wit.ai response
            JSONObject data = new JSONObject(response);

            // Update the TextView with the voice transcription
            // Run it on the MainActivity's UI thread since it's the owner
            final String utterance = data.getString("text");
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    speechTranscription.setText(utterance);
                }
            });

            // Get most confident intent
            JSONObject intent = getMostConfident(data.getJSONArray("intents"));
            if (intent == null) {
                textToSpeech.speak("I'm sorry. Please say it again?", TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
                return;
            }
            intentName = intent.getString("name");
            Log.v("respondToUser", intentName);

            // Parse and get the most confident entity value for the name
            // speakerName에 keyword 저장
            JSONObject nameEntity = getMostConfident((data.getJSONObject("entities")).getJSONArray("weather_condition_fin:weather_condition_fin")); //"weather_condition_fin:weather_condition_fin"
            speakerName = (String) nameEntity.get("value");
            Log.v("respondToUser", speakerName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Handle intents(지정한 intent와 동일한지 파악)
        if (intentName.equals("check_weather")) {
            responseText = speakerName != null ? "Let's see some good recipes to eat in this weather. Press show button.":"Let's see some good recipes to eat in this weather. Press show button."; //"그런가요? 제가 그 날씨에 알맞은 음식을 추천할게요" //"Nice to meet you " + speakerName : "Nice to meet you";
            //System.out.println(speakerName);
            textToSpeech.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
        } else {
            // If there is no matching intent, let the user know and ask them to try againp
            textToSpeech.speak("I'm sorry. Please say it again.", TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
        }

        //okay버튼에 대한 액션 구현(사용자가 대답한 날씨를 기존 keyword와 매칭 후 okay버튼 누르면 이동)
        if(speakerName.equals("sunny")) {
            okayButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {

                    int ran = RandomExample();
                    switch(ran){
                        case 1 :
                            Intent intent1 = new Intent(MainActivity.this , Sunny1.class);
                            startActivity(intent1); // 액티비티 이동.
                            break;
                        case 2 :
                            Intent intent2 = new Intent(MainActivity.this , Sunny2.class);
                            startActivity(intent2); // 액티비티 이동.
                            break;
                        case 3 :
                            Intent intent3 = new Intent(MainActivity.this , Sunny3.class);
                            startActivity(intent3); // 액티비티 이동.
                            break;
                        case 4 :
                            Intent intent4 = new Intent(MainActivity.this , Sunny4.class);
                            startActivity(intent4); // 액티비티 이동.
                            break;
                        case 5 :
                            Intent intent5 = new Intent(MainActivity.this , Sunny5.class);
                            startActivity(intent5); // 액티비티 이동.
                            break;


                    }

                }

            });

        }
        else if(speakerName.equals("rainy")) {
            okayButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {

                    int ran = RandomExample();
                    switch(ran){
                        case 1 :
                            Intent intent1 = new Intent(MainActivity.this , Rainy1.class);
                            startActivity(intent1); // 액티비티 이동.
                            break;
                        case 2 :
                            Intent intent2 = new Intent(MainActivity.this , Rainy2.class);
                            startActivity(intent2); // 액티비티 이동.
                            break;
                        case 3 :
                            Intent intent3 = new Intent(MainActivity.this , Rainy3.class);
                            startActivity(intent3); // 액티비티 이동.
                            break;
                        case 4 :
                            Intent intent4 = new Intent(MainActivity.this , Rainy4.class);
                            startActivity(intent4); // 액티비티 이동.
                            break;
                        case 5 :
                            Intent intent5 = new Intent(MainActivity.this , Rainy5.class);
                            startActivity(intent5); // 액티비티 이동.
                            break;


                    }

                }

            });
        }
        else if(speakerName.equals("snowy")) {
            okayButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {

                    int ran = RandomExample();
                    switch(ran){
                        case 1 :
                            Intent intent1 = new Intent(MainActivity.this , Snowy1.class);
                            startActivity(intent1); // 액티비티 이동.
                            break;
                        case 2 :
                            Intent intent2 = new Intent(MainActivity.this , Snowy2.class);
                            startActivity(intent2); // 액티비티 이동.
                            break;
                        case 3 :
                            Intent intent3 = new Intent(MainActivity.this , Snowy3.class);
                            startActivity(intent3); // 액티비티 이동.
                            break;
                        case 4 :
                            Intent intent4 = new Intent(MainActivity.this , Snowy4.class);
                            startActivity(intent4); // 액티비티 이동.
                            break;
                        case 5 :
                            Intent intent5 = new Intent(MainActivity.this , Snowy5.class);
                            startActivity(intent5); // 액티비티 이동.
                            break;


                    }

                }

            });
        }
        else if(speakerName.equals("cloudy")) {
            okayButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {

                    int ran = RandomExample();
                    switch(ran){
                        case 1 :
                            Intent intent1 = new Intent(MainActivity.this , Cloudy1.class);
                            startActivity(intent1); // 액티비티 이동.
                            break;
                        case 2 :
                            Intent intent2 = new Intent(MainActivity.this , Cloudy2.class);
                            startActivity(intent2); // 액티비티 이동.
                            break;
                        case 3 :
                            Intent intent3 = new Intent(MainActivity.this , Cloudy3.class);
                            startActivity(intent3); // 액티비티 이동.
                            break;
                        case 4 :
                            Intent intent4 = new Intent(MainActivity.this , Cloudy4.class);
                            startActivity(intent4); // 액티비티 이동.
                            break;
                        case 5 :
                            Intent intent5 = new Intent(MainActivity.this , Cloudy5.class);
                            startActivity(intent5); // 액티비티 이동.
                            break;


                    }

                }

            });
        }
        else if(speakerName.equals("windy")) {
            okayButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {

                    int ran = RandomExample();
                    switch(ran){
                        case 1 :
                            Intent intent1 = new Intent(MainActivity.this , Windy1.class);
                            startActivity(intent1); // 액티비티 이동.
                            break;
                        case 2 :
                            Intent intent2 = new Intent(MainActivity.this , Windy2.class);
                            startActivity(intent2); // 액티비티 이동.
                            break;
                        case 3 :
                            Intent intent3 = new Intent(MainActivity.this , Windy3.class);
                            startActivity(intent3); // 액티비티 이동.
                            break;
                        case 4 :
                            Intent intent4 = new Intent(MainActivity.this , Windy4.class);
                            startActivity(intent4); // 액티비티 이동.
                            break;
                        case 5 :
                            Intent intent5 = new Intent(MainActivity.this , Windy5.class);
                            startActivity(intent5); // 액티비티 이동.
                            break;


                    }

                }

            });
        }
    }



    // Get the resolved intent or entity with the highest confidence from Wit Speech API
    // https://wit.ai/docs/recipes#which-confidence-threshold-should-you-use
    private JSONObject getMostConfident(JSONArray list) {
        JSONObject confidentObject = null;
        double maxConfidence = 0.0;
        for (int i = 0; i < list.length(); i++) {
            try {
                JSONObject object = list.getJSONObject(i);
                double currConfidence = object.getDouble("confidence");
                if (currConfidence > maxConfidence) {
                    maxConfidence = currConfidence;
                    confidentObject = object;
                }
            } catch(JSONException e) {
                e.printStackTrace();
            }
        }
        return confidentObject;
    }

    // Instantiate a new AudioRecord and start streaming the recording to the Wit Speech API
    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL, AUDIO_FORMAT, BUFFER_SIZE);
        recorder.startRecording();
        recordingInProgress.set(true);
        recordingThread = new Thread(new StreamRecordingRunnable(), "Stream Recording Thread");
        recordingThread.start();
    }

    // Release resources for the AudioRecord and Runnable when recording is stopped
    private void stopRecording() {
        if (recorder == null) return;
        recordingInProgress.set(false);
        recorder.stop();
        recorder.release();
        recorder = null;
        recordingThread = null;
    }

    // Define a Runnable to stream the recording data to the Speech API
    // https://wit.ai/docs/http#post__speech_link
    private class StreamRecordingRunnable implements Runnable {
        @Override
        public void run() {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            RequestBody requestBody = new RequestBody() {
                @Override
                public MediaType contentType() {
                    return MediaType.parse("audio/raw;encoding=signed-integer;bits=16;rate=8000;endian=little");
                }

                @Override
                public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
                    while (recordingInProgress.get()) {
                        int result = recorder.read(buffer, BUFFER_SIZE);
                        if (result < 0) {
                            throw new RuntimeException("Reading of audio buffer failed: " +
                                    getBufferReadFailureReason(result));
                        }
                        bufferedSink.write(buffer);
                        buffer.clear();
                    }
                }
            };

            // Start streaming audio to Wit.ai Speech API
            Request request = httpRequestBuilder.post(requestBody).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    respondToUser(responseData);
                    Log.v("Streaming Response", responseData);
                }
            } catch (IOException e) {
                Log.e("Streaming Response", e.getMessage());
            }
        }

        private String getBufferReadFailureReason(int errorCode) {
            switch (errorCode) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                case AudioRecord.ERROR:
                    return "ERROR";
                default:
                    return "Unknown (" + errorCode + ")";
            }
        }
    }

    // Instantiate a Request.Builder that can be used for all the streaming requests
    // https://square.github.io/okhttp/recipes/#post-streaming-kt-java
    // https://wit.ai/docs/http#post__speech_link
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

    // Initialize the Android TextToSpeech
    // https://developer.android.com/reference/android/speech/tts/TextToSpeech
    private void initializeTextToSpeech(Context applicationContext) {
        textToSpeech = new TextToSpeech(applicationContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int ttsStatus) {
                // Disable the speakButton and provide the status of app while waiting for TextToSpeech to initialize
                speechTranscription.setHint("Loading app ...");
                speakButton.setEnabled(false);

                // Check the status of the initialization
                if (ttsStatus == TextToSpeech.SUCCESS) {
                    if(CLIENT_ACCESS_TOKEN == "<YOUR CLIENT ACCESS TOKEN>") {
                        textToSpeech.speak("Hi! Before we start the demo. Please set the client access token.", TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
                    } else {
                        textToSpeech.speak("Hello! I am Foopy taking care of your happy diet.\n" +
                                "I will recommend a suitable diet according to the weather. How’s the weather outside?", TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
                    }
                    speechTranscription.setHint("Press Speak and say something!");
                    speakButton.setEnabled(true);
                } else {
                    String message = "TextToSpeech initialization failed";
                    speechTranscription.setTextColor(Color.RED);
                    speechTranscription.setText(message);
                    Log.e("TextToSpeech", message);
                }
            }
        });
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.RECORD_AUDIO
        }, 1000);
    }

    private boolean checkPermissionsFromDevice() {
        int recordAudioResult = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int internetResult = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);

        return recordAudioResult == PackageManager.PERMISSION_GRANTED
                && internetResult == PackageManager.PERMISSION_GRANTED;
    }


}
