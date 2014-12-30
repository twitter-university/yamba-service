package com.twitter.university.android.yamba.sync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.twitter.university.android.yamba.service.BuildConfig;
import com.twitter.university.android.yamba.service.NewAccountActivity;
import com.twitter.university.android.yamba.service.R;
import com.twitter.university.android.yamba.service.YambaApplication;

import java.util.UUID;


public class AccountMgr extends AbstractAccountAuthenticator {
    private static final String TAG = "ACCT";

    public static final String KEY_HANDLE = "YambaAuth.HANDLE";
    public static final String KEY_ENDPOINT = "YambaAuth.ENDPOINT";
    public static final String AUTH_TYPE_CLIENT = "YambaAuth.AUTH_CLIENT";

    public static final int ERROR_UNKNOWN_TYPE = -1;
    public static final int ERROR_UNKNOWN_AUTH = -2;
    public static final int ERROR_EXISTS = -3;

    public static Bundle buildAccountExtras(String handle, String endpoint) {
        Bundle acctExtras = new Bundle();
        acctExtras.putString(AccountMgr.KEY_HANDLE, handle);
        acctExtras.putString(AccountMgr.KEY_ENDPOINT, endpoint);
        return acctExtras;
    }


    private final YambaApplication app;

    public AccountMgr(YambaApplication app) {
        super(app);
        this.app = app;
    }

    @Override
    public Bundle addAccount(
        AccountAuthenticatorResponse resp,
        String accountType,
        String authTokenType,
        String[] requiredFeatures,
        Bundle options)
    {
        if (BuildConfig.DEBUG) { Log.d(TAG, "addAccount"); }
        Bundle reply = new Bundle();

        String at = app.getString(R.string.account_type);

        if (!at.equals(accountType)) {
            reply.putInt(AccountManager.KEY_ERROR_CODE, ERROR_UNKNOWN_TYPE);
            reply.putString(
                AccountManager.KEY_ERROR_MESSAGE,
                app.getString(R.string.account_err_unknown_type));
            return reply;
        }

        if (0 < AccountManager.get(app).getAccountsByType(at).length) {
            reply.putInt(AccountManager.KEY_ERROR_CODE, ERROR_EXISTS);
            reply.putString(
                AccountManager.KEY_ERROR_MESSAGE,
                app.getString(R.string.account_err_exists));
            return reply;
        }

        reply.putParcelable(
            AccountManager.KEY_INTENT,
            new Intent(app, NewAccountActivity.class));

        return reply;
    }

    @Override
    public Bundle getAuthToken(
        AccountAuthenticatorResponse response,
        Account account,
        String authTokenType,
        Bundle options)
    {
        Bundle reply = new Bundle();
        reply.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        reply.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);

        if (!app.getString(R.string.account_type).equals(account.type)) {
            reply.putInt(AccountManager.KEY_ERROR_CODE, ERROR_UNKNOWN_TYPE);
            reply.putString(AccountManager.KEY_ERROR_MESSAGE, app.getString(R.string.account_err_unknown_type));
            return reply;
        }

        if (!AUTH_TYPE_CLIENT.equals(authTokenType)) {
            reply.putInt(AccountManager.KEY_ERROR_CODE, ERROR_UNKNOWN_AUTH);
            reply.putString(AccountManager.KEY_ERROR_MESSAGE, app.getString(R.string.account_err_unknown_auth));
            return reply;
        }

        AccountManager mgr = AccountManager.get(app);
        String token = UUID.randomUUID().toString();
        app.createClient(
            token,
            mgr.getUserData(account, AccountMgr.KEY_HANDLE),
            mgr.getPassword(account),
            mgr.getUserData(account, AccountMgr.KEY_ENDPOINT));

        reply.putString(AccountManager.KEY_AUTHTOKEN, token);

        return reply;
    }

    @Override
    public Bundle updateCredentials(
        AccountAuthenticatorResponse response,
        Account account,
        String authTokenType,
        Bundle options)
    {
        throw new UnsupportedOperationException("Update credentials not supported.");
    }

    @Override
    public Bundle hasFeatures(
        AccountAuthenticatorResponse response,
        Account account,
        String[] features)
    {
        throw new UnsupportedOperationException("Has features not supported.");
    }

    @Override
    public Bundle confirmCredentials(
        AccountAuthenticatorResponse response,
        Account account,
        Bundle options)
    {
        throw new UnsupportedOperationException("Confirm credentials not supported.");
    }

    @Override
    public Bundle editProperties(
        AccountAuthenticatorResponse response,
        String accountType)
    {
        throw new UnsupportedOperationException("Edit properties not supported.");
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        throw new UnsupportedOperationException("Auth Token Label not supported.");
    }
}
