package com.example.rmulo.poliglotamobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;

import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    public static final int REQUEST_CHECK = 0;
    private Spinner mSpLocale;
    private TextToSpeech mSpeech;
    private EditText mEdtText;
    private TextView tvTranslatedText;
    private ProgressBar pb;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Configura o progressBar
        handler = new Handler();
        pb = (ProgressBar)findViewById(R.id.progressBar);
        updateProgressBar(false);

        // Checa se os arquivos de recurso do TTS estao disponiveis
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, REQUEST_CHECK);

        // Inicializa objetos
        mSpLocale = (Spinner) findViewById(R.id.spLocale);
        mEdtText = (EditText) findViewById(R.id.edtText);
        tvTranslatedText = ((TextView) findViewById(R.id.tvTranslatedText));

        // Cria o adapter com o Array de Idiomas
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.locale_arrays));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpLocale.setAdapter(adapter);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Faz as verificacoes no retorno da chamada da Intent(recurso) do TTS.
        if (requestCode == REQUEST_CHECK) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS){
                System.out.println("RSL: Inicializa o mSpeech");
                // Inicializa o mSpeech passando o contexto (this) e quem implementa a
                // interface OnInitListener (this)
                mSpeech = new TextToSpeech(this, this);
            } else {
                System.out.println("RSL: Nao tem os recursos do TTS instalado");
                // Nao tem os recursos do TTS instalado, solicita instalacao
                Intent intent = new Intent();
                intent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onInit(int status) {
        // Verifica se obteve sucesso na inicializacao do mSpeech
        if (status != TextToSpeech.SUCCESS) {
            Toast.makeText(this, R.string.falha_na_inicializacao, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        // Shutdown no Speech
        if (mSpeech != null) {
            mSpeech.stop();
            mSpeech.shutdown();
        }
        super.onDestroy();
    }

    public void btnTraduzirClick(View v) throws Exception {
        String text = mEdtText.getText().toString();

        // Verifica se algum texto foi digitado
        if (text.length() == 0) {
            Toast.makeText(this,R.string.digite_o_texto_para_traduzir, Toast.LENGTH_LONG).show();
        } else {
            updateProgressBar(true);

            //Esconder o teclado
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

            class TranslateTask extends AsyncTask<Void, Void, Void> {

                String translatedText = "";
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        String text = mEdtText.getText().toString();
                        translatedText = translate(text);
                    } catch (Exception e) {
                        e.printStackTrace();
                        translatedText = e.toString();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    tvTranslatedText.setText(translatedText);
                    super.onPostExecute(result);
                    updateProgressBar(false);
                }
            }

            new TranslateTask().execute();

        }
    }


    public String translate(String text) throws Exception{
        //Atribui o id e secret gerado no Microsoft MarkerPlace
        //http://blogs.msdn.com/b/translation/p/gettingstarted1.aspx
        Translate.setClientId("romulolima");
        Translate.setClientSecret("B514rq96dWedYIavyTOHS9ahqqnn8MST9sUpbIZHPGY=");

        String translatedText = "";

        switch (mSpLocale.getSelectedItemPosition()) {
            case 0:
                translatedText = Translate.execute(text, Language.ENGLISH);
                break;
            case 1:
                translatedText = Translate.execute(text, Language.PORTUGUESE);
                break;
            case 2:
                translatedText = Translate.execute(text, Language.FRENCH);
                break;
            default:
                translatedText = Translate.execute(text, Language.SPANISH);
                break;
        }
        return translatedText;
    }


    public void btnFalarClick(View v) {

        // Configura os recursos especificos de acordo
        // com o idioma selecionado que precisam ser
        // carregados antes do motor comecar a falar.
        int result = -1;
        switch (mSpLocale.getSelectedItemPosition()) {
            case 0:
                result = mSpeech.setLanguage(Locale.ENGLISH);
                break;
            case 1:
                result = mSpeech.setLanguage(new Locale("pt_BR"));
                break;
            case 2:
                result = mSpeech.setLanguage(Locale.FRENCH);
                break;
            default:
                result = mSpeech.setLanguage(new Locale("spa"));
                break;
        }

        // Verifica se o dispositivo suporta o idioma selecionado
        if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Exibe mensagem de erro
            Toast.makeText(this, R.string.idioma_nao_suportado, Toast.LENGTH_LONG).show();
            return;
        }

        String text = tvTranslatedText.getText().toString();

        // Verifica se algum texto foi digitado
        if (text.length() == 0) {
            Toast.makeText(this,R.string.primeiro_digite_o_texto_para_traduzir, Toast.LENGTH_LONG).show();
        } else {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                mSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }else{
                mSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null,null);
            }
        }
    }

    private void updateProgressBar(final boolean visivel) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (visivel) {
                    pb.setVisibility(View.VISIBLE);
                } else {
                    pb.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
