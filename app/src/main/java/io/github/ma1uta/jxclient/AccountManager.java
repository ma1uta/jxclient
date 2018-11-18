/*
 * Copyright sablintolya@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ma1uta.jxclient;

import io.github.ma1uta.jxclient.matrix.MatrixAccount;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * AccountManager factory.
 */
public class AccountManager {

    private final ResourceBundle i18nBundle;
    private final Map<String, Account> accountMap = new HashMap<>();

    public AccountManager(ResourceBundle i18nBundle) {
        this.i18nBundle = i18nBundle;
    }

    /**
     * Account types.
     */
    public enum Type {
        /**
         * Matrix account.
         */
        MATRIX
    }

    /**
     * Load all available accounts.
     *
     * @throws BackingStoreException when cannot load accounts (often it means I/O error).
     */
    public void loadAccounts() throws BackingStoreException {
        var root = Preferences.userRoot();
        var accounts = root.node("jxclient/accounts");
        for (var accountName : accounts.childrenNames()) {
            newAccount(Type.MATRIX, accounts.node(accountName));
        }
    }

    /**
     * Provides loaded accounts.
     *
     * @return loaded accounts.
     */
    public Collection<Account> accounts() {
        return accountMap.values();
    }

    /**
     * Provides a new account with specified preferences.
     *
     * @param type        account type.
     * @param preferences account preferences.
     * @return new account instance.
     */
    public Account newAccount(Type type, Preferences preferences) {
        Account account;
        switch (type) {
            case MATRIX:
                account = new MatrixAccount();
                break;
            default:
                return null;
        }
        account.init(this, preferences, i18nBundle);
        accountMap.put(Integer.toString(accountMap.size()), account);
        return account;
    }

    /**
     * Provides a new account.
     *
     * @param type account type.
     * @return new account instance.
     */
    public Account newAccount(Type type) {
        return newAccount(type, null);
    }

    /**
     * Remove account.
     *
     * @param account account to remove.
     */
    public void removeAccount(Account account) {
        String accountToRemove = null;
        for (Map.Entry<String, Account> accountEntry : accountMap.entrySet()) {
            if (accountEntry.getValue().equals(account)) {
                accountToRemove = accountEntry.getKey();
                break;
            }
        }
        if (accountToRemove != null) {
            var root = Preferences.userRoot();
            try {
                accountMap.remove(accountToRemove);
                root.node("jxclient/accounts/" + accountToRemove).removeNode();
            } catch (BackingStoreException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Save account preferences.
     *
     * @param account     account to save.
     * @param preferences account preferences.
     */
    public void sync(Account account, Map<String, String> preferences) {
        var root = Preferences.userRoot();
        String id = null;
        for (Map.Entry<String, Account> entry : accountMap.entrySet()) {
            if (entry.getValue().equals(account)) {
                id = entry.getKey();
                break;
            }
        }
        if (id != null) {
            Preferences node = root.node("jxclient/accounts/" + id);
            preferences.forEach(node::put);
            try {
                node.sync();
            } catch (BackingStoreException e) {
                e.printStackTrace();
            }
        }
    }
}
