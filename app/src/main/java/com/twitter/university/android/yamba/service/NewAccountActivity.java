package com.twitter.university.android.yamba.service;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.twitter.university.android.yamba.sync.AccountMgr;


public class NewAccountActivity extends AccountAuthenticatorActivity {
    private static final String TAG = "ACCOUNT";


    private class EmptyWatcher implements TextWatcher {
        private final int id;

        EmptyWatcher(int id) {
            this.id = id;
            setEmpty(id, true);
        }

        @Override
        public void afterTextChanged(Editable s) { setEmpty(id, 0 >= s.length()); }

        @Override
        public void beforeTextChanged(CharSequence s, int b, int n, int a) { }

        @Override
        public void onTextChanged(CharSequence s, int b, int p, int n) { }
    }

    private final SparseBooleanArray empty = new SparseBooleanArray();

    private String accountType;
    private int pollInterval;

    private EditText handle;
    private EditText password;
    private EditText endpoint;
    private Button submit;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.yamba, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.menu_about:
                about();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        accountType = getString(R.string.account_type);
        // could get this from the user...
        pollInterval = getResources().getInteger(R.integer.poll_interval) * 1000;

        setContentView(R.layout.activity_new_account);

        submit = (Button) findViewById(R.id.account_submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { newAccount(); }
        });

        handle = (EditText) findViewById(R.id.account_handle);
        handle.addTextChangedListener(new EmptyWatcher(R.id.account_handle));

        password = (EditText) findViewById(R.id.account_password);
        password.addTextChangedListener(new EmptyWatcher(R.id.account_password));

        endpoint = (EditText) findViewById(R.id.account_endpoint);
        endpoint.addTextChangedListener(new EmptyWatcher(R.id.account_endpoint));
    }

    void setEmpty(int id, boolean isEmpty) {
        empty.put(id, isEmpty);
        submit.setEnabled(valid());
    }

    private void newAccount() {
        String hdl = handle.getText().toString();
        String pwd = password.getText().toString();
        String uri = endpoint.getText().toString();

        int err = 0;
        if (TextUtils.isEmpty(hdl)) { err = R.string.err_empty_handle; }
        else if (TextUtils.isEmpty(pwd)) { err = R.string.err_empty_password; }
        else if (TextUtils.isEmpty(uri)) { err = R.string.err_empty_endpoint; }
        if (0 != err) {
            Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
            return;
        }

        createAccount(hdl, pwd, uri);

        finish();
    }

    private void createAccount(String handle, String password, String endpoint) {
        String acctName = new StringBuilder().append(handle).append("@").append(endpoint).toString();
        if (BuildConfig.DEBUG) {
            Log.d( TAG, "create account: " + accountType + ": " + acctName);
        }

        Account account = new Account(acctName, accountType);
        Bundle acctExtras = AccountMgr.buildAccountExtras(handle, endpoint);
        if (!AccountManager.get(this).addAccountExplicitly(account, password, acctExtras)) {
            Toast.makeText(this, R.string.account_failed, Toast.LENGTH_LONG).show();
            return;
        }

        ContentResolver.setIsSyncable(account, YambaContract.AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(account, YambaContract.AUTHORITY, true);
        ContentResolver.addPeriodicSync(account, YambaContract.AUTHORITY, new Bundle(), pollInterval);
    }

    private boolean valid() {
        for (int i = 0; i < empty.size(); i++) {
            if (empty.valueAt(i)) { return false; }
        }
        return true;
    }

    private void about() {
        Toast.makeText(this, R.string.about_yamba, Toast.LENGTH_LONG).show();
    }
}

