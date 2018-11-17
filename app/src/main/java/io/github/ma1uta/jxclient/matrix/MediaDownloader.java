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

package io.github.ma1uta.jxclient.matrix;

import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Download various media content (images, audio, video).
 */
public class MediaDownloader {

    private final MatrixAccount account;

    public MediaDownloader(MatrixAccount account) {
        this.account = account;
    }

    /**
     * Download media content provided by the mxc url.
     *
     * @param mxcUrl content url.
     * @return url with http/https/file schemas with content.
     */
    public CompletionStage<String> download(String mxcUrl) {
        var result = new CompletableFuture<String>();
        int start = mxcUrl.indexOf("mxc://") + "mxc://".length();
        result.complete("https://" + account.getClient().getHomeserverUrl() + "/_matrix/media/r0/download/" + mxcUrl.substring(start));
        //TODO enable cache
        /*account.getExecutorService().execute(() -> {
            validate(mxcUrl);
            int start = mxcUrl.indexOf("mxc://");
            int domain = mxcUrl.indexOf("/", start);
            account.getClient().content().download(mxcUrl.substring(start, domain), mxcUrl.substring(domain), true)
                .thenAccept(inputStream -> {
                    try {
                        Path cacheLocation = Paths.get("");
                        Files.copy(inputStream, cacheLocation);
                        result.complete(cacheLocation.toUri().toURL().toExternalForm());
                    } catch (IOException e) {
                        e.printStackTrace();
                        result.completeExceptionally(e);
                    }
                });
        });*/
        return result;
    }

    /**
     * Download media content and invoke the callback in the FXThread.
     *
     * @param mxcUrl   content url.
     * @param callback callback.
     */
    public void download(String mxcUrl, Consumer<String> callback) {
        download(mxcUrl).thenAccept(url -> Platform.runLater(() -> callback.accept(url)));
    }

    private void validate(String mxcUrl) {
        //TODO
    }
}
