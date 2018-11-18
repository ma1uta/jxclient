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

import javafx.scene.control.Tab;

import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * Account.
 */
public interface Account {

    /**
     * Provide account type.
     *
     * @return account type.
     */
    AccountManager.Type getType();

    /**
     * Init account with the specified preferences.
     *
     * @param accountManager accountManager factory.
     * @param accountNode    preferences.
     * @param i18n           resource bundle.
     */
    void init(AccountManager accountManager, Preferences accountNode, ResourceBundle i18n);

    /**
     * Provides UI of this account.
     *
     * @return tab with UI.
     */
    Tab getTab();

    /**
     * Either login view is showed.
     *
     * @return {@code true} if th login view is showed.
     */
    boolean isLoginView();
}
