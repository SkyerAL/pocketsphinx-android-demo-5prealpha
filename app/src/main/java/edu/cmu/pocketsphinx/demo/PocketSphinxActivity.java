/* ====================================================================
 * Copyright (c) 2014 Alpha Cephei Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ALPHA CEPHEI INC. ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 */

package edu.cmu.pocketsphinx.demo;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;

import android.app.Activity;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;
import android.widget.Toast;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.model.Model;
import edu.cmu.pocketsphinx.model.ModelState;

public class PocketSphinxActivity extends Activity implements
        RecognitionListener {

    private static final String KWS_SEARCH = "wakeup";
    private static final String FORECAST_SEARCH = "blabla";
    private static final String DIGITS_SEARCH = "podbor";
    private static final String MENU_SEARCH = "menu";
    private static final String KEYPHRASE = "start";

    private static String TEK_SEARCH = "KWS_SEARCH";


    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;

    private TextToSpeech mTextToSpeech;
    private final Queue<String> mSpeechQueue = new LinkedList<>();

    private PocketSphinxActivity instance;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Prepare the data for UI
        captions = new HashMap<String, Integer>();
        captions.put(KWS_SEARCH, R.string.kws_caption);
        captions.put(MENU_SEARCH, R.string.menu_caption);
        captions.put(DIGITS_SEARCH, R.string.digits_caption);
        captions.put(FORECAST_SEARCH, R.string.forecast_caption);
        setContentView(R.layout.main);
        ((TextView) findViewById(R.id.caption_text))
                .setText("Preparing the recognizer");

        instance = this;

        mTextToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR) {
                    return;
                } else {
                    Toast.makeText(instance, "Статус загрузки = " + status, Toast.LENGTH_SHORT).show();
                }
                Locale locale = new Locale("ru");
                //if (mTextToSpeech.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {}
                int result = mTextToSpeech.setLanguage(locale);
                if (result == mTextToSpeech.LANG_MISSING_DATA || result == mTextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(instance, "Языковой пакет не загружен", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    Toast.makeText(instance, "Статус загрузки локали = " + result, Toast.LENGTH_SHORT).show();
                }
                mTextToSpeech.setOnUtteranceCompletedListener(mUtteranceCompletedListener);
                //mTextToSpeech.setOnUtteranceProgressListener(mUtteranceCompletedListener);
            }
        });

        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(PocketSphinxActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    ((TextView) findViewById(R.id.caption_text))
                            .setText("Failed to init recognizer " + result);
                } else {
                    TEK_SEARCH = KWS_SEARCH;
                    switchSearch(TEK_SEARCH);
                }
            }
        }.execute();
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        String answer = null;
        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            if (Model.setState(ModelState.ACTIVATE)) {
                answer = getString(R.string.answer_activate);
            }
            TEK_SEARCH = MENU_SEARCH;
            switchSearch(TEK_SEARCH);
            if (answer != null) {
                process(answer);
            }
        }
        else if (text.equals(DIGITS_SEARCH)) {
            if (Model.setState(ModelState.START)) {
                answer = getString(R.string.answer_start) + " " + getString(R.string.answer_article) + " " + Model.getData().getCurrent().getName();
            }
            TEK_SEARCH = DIGITS_SEARCH;
            switchSearch(TEK_SEARCH);
            if (answer != null) {
                process(answer);
            }
        }
        else if (text.equals(FORECAST_SEARCH)) {
/*
            TEK_SEARCH = FORECAST_SEARCH;
            switchSearch(TEK_SEARCH);
*/
            if (Model.setState(ModelState.START)) {
                answer = getString(R.string.answer_start) + " " + getString(R.string.answer_article) + " " + Model.getData().getCurrent().getName();
            }

            TEK_SEARCH = DIGITS_SEARCH;
            switchSearch(TEK_SEARCH);
            if (answer != null) {
                process(answer);
            }

        }
        else {
            ((TextView) findViewById(R.id.result_text)).setText(text);
            recognizer.stop();
            //processing(hypothesis);

        }
    }

    private void processing(Hypothesis hypothesis) {

      //  mHandler.removeCallbacks(mStopRecognitionCallback);
        recognizer.stop();
        // TODO: проверка
        String text = null;
//        int score = 0;

 /*       if (hypothesis != null) {
            score = hypothesis.getBestScore();
            text = hypothesis.getHypstr();
            Toast.makeText(this, text + " ***** Score: " + score, Toast.LENGTH_SHORT).show();
        }
*/
//        mRecognizer.stop();
//        mRecognizer.cancel();

        text = hypothesis.getHypstr();

//        if (score<-3000 && COMMAND_SEARCH.equals(mRecognizer.getSearchName())) {
        if (text != null) {
//              Toast.makeText(this, "Score: " + score, Toast.LENGTH_SHORT).show();

            //Toast.makeText(this, text + " ***** Score: " + score, Toast.LENGTH_SHORT).show();
            ((TextView) findViewById(R.id.result_text)).setText(text);

            // TODO: states
            String answer = null;

/*
            if (text.equals(getString(R.string.command_activate_1)) || text.equals(getString(R.string.command_activate_2))) {
                if (Model.setState(ModelState.ACTIVATE)) {
                    answer = getString(R.string.answer_activate);
                }
            } else if (text.equals(getString(R.string.command_start_1)) || text.equals(getString(R.string.command_start_2))) {
                if (Model.setState(ModelState.START)) {
                    answer = getString(R.string.answer_start) + " " + getString(R.string.answer_article) + " " + Model.getData().getCurrent().getName();
                }
            } else
*/
            if (text.equals("gotovo") || text.equals("dalshe") || text.equals("next")) {
                if (Model.getState() == ModelState.START) {
                    if (Model.getData().getNext() != null) {
                        answer = getString(R.string.answer_article) + " " + Model.getData().getCurrent().getName();
                    } else {
                        Model.setState(ModelState.STOP);
                        answer = getString(R.string.answer_stop);
                    }
                }
            } else if (text.equals("povtory") || text.equals("eseraz") || text.equals("neponyal")) {
                if (Model.getState() == ModelState.START) {
                    answer = getString(R.string.answer_article) + " " + Model.getData().getCurrent().getName();
                }
            } else if (text.equals("zavershit") || text.equals("stop")) {
                if (Model.setState(ModelState.STOP)) {
                    answer = getString(R.string.answer_stop);
                    TEK_SEARCH = KWS_SEARCH;
                    switchSearch(TEK_SEARCH);
                }
            } else {
                //answer = getString(R.string.answer_undefined);
            }

            if (answer != null) {
                process(answer);
            } else {
                recognizer.startListening(TEK_SEARCH);
            }
//            }

        }else{
            recognizer.startListening(TEK_SEARCH);
        }
    }



    @Override
    public void onResult(Hypothesis hypothesis) {
        ((TextView) findViewById(R.id.result_text)).setText("");
        if (hypothesis != null) {
            processing(hypothesis);
            String text = hypothesis.getHypstr();
            makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onEndOfSpeech() {
  /*      if (DIGITS_SEARCH.equals(recognizer.getSearchName())
                || FORECAST_SEARCH.equals(recognizer.getSearchName()))
            switchSearch(KWS_SEARCH);*/
    }

    private void switchSearch(String searchName) {
        recognizer.stop();
        recognizer.startListening(searchName);
        String caption = getResources().getString(captions.get(searchName));
        ((TextView) findViewById(R.id.caption_text)).setText(caption);
    }

    private void setupRecognizer(File assetsDir) {
        File modelsDir = new File(assetsDir, "models");
        recognizer = defaultSetup()
                .setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
                .setDictionary(new File(modelsDir, "dict/cmu07a.dic"))
                .setRawLogDir(assetsDir).setKeywordThreshold(1e-20f)
                .getRecognizer();
        recognizer.addListener(this);

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
        // Create grammar-based searches.
        File menuGrammar = new File(modelsDir, "grammar/menu.gram");
        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
        File digitsGrammar = new File(modelsDir, "grammar/digits.gram");
        recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);
        // Create language model search.
        File languageModel = new File(modelsDir, "lm/weather.dmp");
        recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);
    }

    ///
    /// Синтез речи
    ///

    private void speak(String text) {
        synchronized (mSpeechQueue) {
            recognizer.stop();
            mSpeechQueue.add(text);
            HashMap<String, String> params = new HashMap<>(2);
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UUID.randomUUID().toString());
            params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
            params.put(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS, "true");
            mTextToSpeech.speak(text, TextToSpeech.QUEUE_ADD, params);
        }
    }

    private final TextToSpeech.OnUtteranceCompletedListener mUtteranceCompletedListener = new TextToSpeech.OnUtteranceCompletedListener() {
        @Override
        public void onUtteranceCompleted(String utteranceId) {
            synchronized (mSpeechQueue) {
                mSpeechQueue.poll();
                if (mSpeechQueue.isEmpty()) {
//                    recognizer.startListening(KWS_SEARCH);
                    recognizer.startListening(TEK_SEARCH);
                    //startRecognition();
                }
            }
        }
    };
    private void process(String text) {
        //Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        ((TextView) findViewById(R.id.result_text)).setText(text);
        speak(text);
    }
}
