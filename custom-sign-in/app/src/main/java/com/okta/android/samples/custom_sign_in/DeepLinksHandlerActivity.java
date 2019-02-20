package com.okta.android.samples.custom_sign_in;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.okta.android.samples.custom_sign_in.util.OktaProgressDialog;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.client.AuthenticationClients;
import com.okta.authn.sdk.resource.AuthenticationResponse;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeepLinksHandlerActivity extends Activity {
    private String TAG = "DeepLinksHandler";
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private AuthenticationClient authenticationClient = null;
    private OktaProgressDialog oktaProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        oktaProgressDialog = new OktaProgressDialog(this);

        String path = getIntent().getData().getPath();
        if(path == null) {
            finish();
        }

        init();

        if(path.startsWith("/signin/reset-password/")) {
            handleResetPassword(path);
        } else {
            finish();
        }
    }

    private void init() {
        authenticationClient = AuthenticationClients.builder()
                .setOrgUrl(BuildConfig.BASE_URL)
                .build();
    }

    private void handleResetPassword(String path) {
        String[] elements = path.split("/");
        if(elements.length == 0) {
            finish();
        }

        String token = elements[elements.length-1];
        oktaProgressDialog.show();
        executor.submit(() -> {
            try {
                AuthenticationResponse response = authenticationClient.verifyRecoveryToken(token, new AuthenticationStateHandlerAdapter() {
                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) {
                        showMessage(authenticationResponse.toString());
                        finish();
                    }

                    @Override
                    public void handleRecovery(AuthenticationResponse recovery) {
                        // Get next action
                        String stateToken = recovery.getStateToken();
                        if(stateToken == null)
                            throw new IllegalArgumentException("Missed stateToken");

                        Map<String, String> recovery_question = recovery.getUser().getRecoveryQuestion();
                        String question = recovery_question.get("question");
                        if(question == null)
                            throw new IllegalArgumentException("Missed question");

                        startActivity(PasswordRecoveryActivity.createPasswordRecoveryQuestion(getBaseContext(), question, stateToken));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showMessage(e.getLocalizedMessage());
                });
                Log.e(TAG, Log.getStackTraceString(e));
            } finally {
                runOnUiThread(() -> oktaProgressDialog.hide());
                finish();
            }
        });
    }

    private void showMessage(String message) {
        Toast.makeText(this, message,Toast.LENGTH_LONG).show();
    }
}