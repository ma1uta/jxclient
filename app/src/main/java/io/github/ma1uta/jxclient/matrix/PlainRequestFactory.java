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

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.ma1uta.matrix.EmptyResponse;
import io.github.ma1uta.matrix.ErrorResponse;
import io.github.ma1uta.matrix.RateLimitedErrorResponse;
import io.github.ma1uta.matrix.Secured;
import io.github.ma1uta.matrix.client.AuthenticationRequred;
import io.github.ma1uta.matrix.client.RequestParams;
import io.github.ma1uta.matrix.client.factory.RequestFactory;
import io.github.ma1uta.matrix.client.model.auth.AuthenticationFlows;
import io.github.ma1uta.matrix.event.Event;
import io.github.ma1uta.matrix.event.content.EventContent;
import io.github.ma1uta.matrix.event.content.RoomEncryptedContent;
import io.github.ma1uta.matrix.event.content.RoomMessageContent;
import io.github.ma1uta.matrix.impl.exception.MatrixException;
import io.github.ma1uta.matrix.impl.exception.RateLimitedException;
import io.github.ma1uta.matrix.support.jackson.EventContentDeserializer;
import io.github.ma1uta.matrix.support.jackson.EventDeserializer;
import io.github.ma1uta.matrix.support.jackson.RoomEncryptedContentDeserializer;
import io.github.ma1uta.matrix.support.jackson.RoomMessageContentDeserializer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.ws.rs.Path;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

/**
 * Plain request factory.
 */
public class PlainRequestFactory implements RequestFactory {

    private static final System.Logger LOGGER = System.getLogger("PLAIN_REQUEST_FACTRORY");

    private final String homeserverUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final EventContentDeserializer eventContentDeserializer = new EventContentDeserializer();

    public PlainRequestFactory(String homeserverUrl) {
        this.homeserverUrl = Objects.requireNonNull(homeserverUrl, "Homeserver must be specified.");
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();

        var eventModule = new SimpleModule();
        eventModule.addDeserializer(Event.class, new EventDeserializer());
        eventModule.addDeserializer(RoomEncryptedContent.class, new RoomEncryptedContentDeserializer());
        eventModule.addDeserializer(RoomMessageContent.class, new RoomMessageContentDeserializer());

        mapper.registerModule(eventModule);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @Override
    public String getHomeserverUrl() {
        return homeserverUrl;
    }

    @Override
    public <T, R> CompletableFuture<R> post(Class<?> apiClass, String apiMethod, RequestParams params, T payload, Class<R> responseClass) {
        return post(apiClass, apiMethod, params, payload, responseClass, MediaType.APPLICATION_JSON);
    }

    @Override
    public <T, R> CompletableFuture<R> post(Class<?> apiClass, String apiMethod, RequestParams params, T payload, Class<R> responseClass,
                                            String requestType) {
        return invokeRequest(createRequest(apiClass, apiMethod, params, payload, HttpRequest.Builder::POST, requestType),
            extractor(responseClass));
    }

    @Override
    public <R> CompletableFuture<R> get(Class<?> apiClass, String apiMethod, RequestParams params, GenericType<R> genericType) {
        return invokeRequest(createRequest(apiClass, apiMethod, params, HttpRequest.Builder::GET),
            extractor(genericType));
    }

    @Override
    public <R> CompletableFuture<R> get(Class<?> apiClass, String apiMethod, RequestParams params, Class<R> responseClass) {
        return invokeRequest(createRequest(apiClass, apiMethod, params, HttpRequest.Builder::GET),
            extractor(responseClass));
    }

    @Override
    public <T, R> CompletableFuture<R> put(Class<?> apiClass, String apiMethod, RequestParams params, T payload, Class<R> responseClass) {
        return invokeRequest(createRequest(apiClass, apiMethod, params, payload, HttpRequest.Builder::PUT, MediaType.APPLICATION_JSON),
            extractor(responseClass));
    }

    @Override
    public CompletableFuture<EmptyResponse> delete(Class<?> apiClass, String apiMethod, RequestParams params) {
        return invokeRequest(createRequest(apiClass, apiMethod, params, HttpRequest.Builder::DELETE),
            extractor(EmptyResponse.class));
    }

    @Override
    public EventContent deserialize(byte[] content, String eventType) {
        try {
            return eventContentDeserializer.deserialize(content, eventType, mapper);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the function to read an entity with specified class from the response.
     *
     * @param genericType The {@link GenericType} of the entity. Used when the entity is a generic class.
     * @param <R>         The class of the instance.
     * @return the entity extractor.
     */
    private <R> Function<byte[], R> extractor(GenericType<R> genericType) {
        return response -> {
            try {
                return mapper.readValue(response, mapper.getTypeFactory().constructType(genericType.getType()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Return the function to read an entity with specified class from the response.
     *
     * @param responseClass The class instance of the entity.
     * @param <R>           The class of the instance.
     * @return the entity extractor.
     */
    private <R> Function<byte[], R> extractor(Class<R> responseClass) {
        return response -> {
            try {
                return mapper.readValue(response, responseClass);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Build request uri.
     *
     * @param apiClass  Invoked api class.
     * @param apiMethod Invoked method.
     * @param params    Request parameters.
     * @return The request URI.
     */
    private URI uri(Class<?> apiClass, String apiMethod, RequestParams params) {
        validateMethod(apiClass, apiMethod, params);

        String hs = getHomeserverUrl();

        if (!hs.startsWith("http://") || hs.startsWith("https://")) {
            hs = "https://" + hs;
        }

        var urlBuilder = new StringBuilder(hs);
        var classAnnotation = apiClass.getAnnotation(Path.class);
        urlBuilder.append(classAnnotation.value());
        var methodPath = (String) AccessController.doPrivileged((PrivilegedAction<?>) () -> {
            for (Method method : apiClass.getDeclaredMethods()) {
                if (method.getName().equals(apiMethod)) {
                    return method.getAnnotation(Path.class).value();
                }
            }
            throw new IllegalArgumentException(String.format("%s doesn't contain a method: %s", apiClass.getName(), apiMethod));
        });
        urlBuilder.append(methodPath);

        var skipAmp = true;
        if (!params.getQueryParams().isEmpty()) {
            urlBuilder.append("?");
            for (Map.Entry<String, String> parameterEntry : params.getQueryParams().entrySet()) {
                if (skipAmp) {
                    urlBuilder.append("&");
                    skipAmp = false;
                }
                urlBuilder.append(parameterEntry.getKey()).append("=").append(parameterEntry.getValue());
            }
        }

        var pathTemplate = urlBuilder.toString();
        for (Map.Entry<String, String> pathEntry : params.getPathParams().entrySet()) {
            pathTemplate = pathTemplate.replace("{" + pathEntry.getKey() + "}", encode(pathEntry.getValue()));
        }
        URI uri = URI.create(pathTemplate);
        LOGGER.log(DEBUG, "Request url: {}", uri);
        return uri;
    }

    /**
     * Translates a string into application/x-www-form-urlencoded format using a UTF-8 encoding scheme.
     *
     * @param origin The original string.
     * @return The translated string.
     * @throws IllegalArgumentException when the origin string is empty.
     * @throws RuntimeException         when JVM doesn't support the UTF-8 (write me if this happens).
     */
    private String encode(String origin) {
        if (origin == null) {
            String msg = "Empty value.";
            LOGGER.log(ERROR, msg);
            throw new IllegalArgumentException(msg);
        }
        return URLEncoder.encode(origin, StandardCharsets.UTF_8);
    }

    /**
     * Check that the access token is provided if the protected resource is requested.
     *
     * @param apiClass  API class.
     * @param apiMethod API method.
     * @param params    The request params.
     * @throws IllegalArgumentException if the access token missing.
     */
    private void validateMethod(Class<?> apiClass, String apiMethod, RequestParams params) {
        Method[] methods = AccessController.doPrivileged((PrivilegedAction<Method[]>) apiClass::getDeclaredMethods);
        Method method = Arrays.stream(methods).filter(m -> m.getName().equals(apiMethod)).findAny().orElseThrow(
            () -> new IllegalArgumentException(String.format("Cannot find the method %s in the class %s", apiMethod, apiClass.getName())));

        boolean secured = method.getAnnotation(Secured.class) != null;
        if (secured && (params.getAccessToken() == null || params.getAccessToken().trim().isEmpty())) {
            throw new IllegalArgumentException("The `access_token` should be specified in order to access to the secured resource.");
        }
    }

    /**
     * Create request with body.
     *
     * @param apiClass    Invoked api class.
     * @param apiMethod   Invoked api method.
     * @param params      The request parameters.
     * @param payload     The request payload.
     * @param action      The request action (POST, GET, PUT, DELETE).
     * @param contentType The request content type.
     * @return The http request.
     */
    private HttpRequest createRequest(Class<?> apiClass,
                                      String apiMethod,
                                      RequestParams params,
                                      Object payload,
                                      BiFunction<HttpRequest.Builder, HttpRequest.BodyPublisher, HttpRequest.Builder> action,
                                      String contentType) {
        var builder = HttpRequest.newBuilder(uri(apiClass, apiMethod, params));
        builder = action.apply(builder, createPublisher(payload));
        builder = applyHeaders(builder, params, contentType);
        return builder.build();
    }

    /**
     * Create request without body.
     *
     * @param apiClass  Invoked api class.
     * @param apiMethod Invoked api method.
     * @param params    The request parameters.
     * @param action    The request action (POST, GET, PUT, DELETE).
     * @return The http request.
     */
    private HttpRequest createRequest(Class<?> apiClass,
                                      String apiMethod,
                                      RequestParams params,
                                      Function<HttpRequest.Builder, HttpRequest.Builder> action) {
        var builder = HttpRequest.newBuilder(uri(apiClass, apiMethod, params));
        builder = action.apply(builder);
        builder = applyHeaders(builder, params, MediaType.APPLICATION_JSON);
        return builder.build();
    }

    /**
     * Create request body publisher.
     *
     * @param payload The request payload.
     * @return The request publisher.
     */
    private HttpRequest.BodyPublisher createPublisher(Object payload) {
        try {
            byte[] buf = mapper.writeValueAsBytes(payload);
            if (LOGGER.isLoggable(DEBUG)) {
                LOGGER.log(DEBUG, "Request body: {}", new String(buf, StandardCharsets.UTF_8));
            }
            return HttpRequest.BodyPublishers.ofByteArray(buf);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Add headers to a request.
     *
     * @param builder     The request builder.
     * @param params      The request parameters.
     * @param contentType The request content type.
     * @return The request builder with headers.
     */
    private HttpRequest.Builder applyHeaders(HttpRequest.Builder builder, RequestParams params, String contentType) {
        var current = builder;
        for (Map.Entry<String, String> headerParam : params.getHeaderParams().entrySet()) {
            current = current.header(headerParam.getKey(), encode(headerParam.getValue()));
        }
        if (params.getAccessToken() != null) {
            current = current.header("Authorization", "Bearer " + params.getAccessToken());
        }
        return current.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
    }

    /**
     * Invoke request.
     *
     * @param request   The http request.
     * @param extractor The response extractor.
     * @param <R>       The response class.
     * @return The response promise.
     */
    private <R> CompletableFuture<R> invokeRequest(HttpRequest request, Function<byte[], R> extractor) {
        var responseBodyHandler = HttpResponse.BodyHandlers.ofByteArray();
        var result = new CompletableFuture<R>();
        invokeRequest(result, () -> httpClient.sendAsync(request, responseBodyHandler), extractor, 0);
        return result;
    }

    /**
     * Invoke request.
     *
     * @param result    The response promise.
     * @param action    The action to invoke request.
     * @param extractor The response extractor.
     * @param delay     The delay of the current request.
     * @param <R>       The response class.
     */
    private <R> void invokeRequest(CompletableFuture<R> result, Supplier<CompletableFuture<HttpResponse<byte[]>>> action,
                                   Function<byte[], R> extractor, long delay) {
        action.get().thenAccept(response -> {
            try {
                var status = response.statusCode();
                LOGGER.log(DEBUG, "Response status: {}", status);
                switch (status) {
                    case SUCCESS:
                        success(result, response, extractor);
                        break;

                    case UNAUTHORIZED:
                        unauthorized(result, response);
                        break;

                    case RATE_LIMITED:
                        rateLimited(result, response, action, extractor, delay);
                        break;

                    default:
                        error(result, response);
                }

            } catch (Exception e) {
                LOGGER.log(ERROR, "Unknown exception.", e);
                result.completeExceptionally(e);
            } catch (Throwable e) {
                LOGGER.log(ERROR, "Throwable!", e);
                result.completeExceptionally(e);
            }

            if (LOGGER.isLoggable(DEBUG)) {
                LOGGER.log(DEBUG, "Done: {}", result.isDone());
                LOGGER.log(DEBUG, "Cancelled: {}", result.isCancelled());
                LOGGER.log(DEBUG, "Exception: {}", result.isCompletedExceptionally());
            }
        });
    }

    private <R> void success(CompletableFuture<R> result, HttpResponse<byte[]> response, Function<byte[], R> extractor) {
        LOGGER.log(DEBUG, "Success.");
        result.complete(extractor.apply(response.body()));
    }

    private <R> void unauthorized(CompletableFuture<R> result, HttpResponse<byte[]> response) {
        LOGGER.log(DEBUG, "Authentication required.");
        result.completeExceptionally(new AuthenticationRequred(extractor(AuthenticationFlows.class).apply(response.body())));
    }

    private <R> void rateLimited(CompletableFuture<R> result,
                                 HttpResponse<byte[]> response,
                                 Supplier<CompletableFuture<HttpResponse<byte[]>>> action,
                                 Function<byte[], R> extractor,
                                 long delay) {
        LOGGER.log(WARNING, "Rate limited.");
        var rateLimited = extractor(RateLimitedErrorResponse.class).apply(response.body());

        if (LOGGER.isLoggable(DEBUG)) {
            LOGGER.log(DEBUG, "Retry after milliseconds: {}", rateLimited.getRetryAfterMs());
            LOGGER.log(DEBUG, "Errcode: {}", rateLimited.getErrcode());
            LOGGER.log(DEBUG, "Error: {}", rateLimited.getError());
        }

        var newDelay = rateLimited.getRetryAfterMs() != null ? rateLimited.getRetryAfterMs() : delay * DELAY_FACTOR;

        if (delay > MAX_DELAY) {
            LOGGER.log(ERROR, "Cannot send request, maximum delay was reached.");
            result.completeExceptionally(
                new RateLimitedException(rateLimited.getErrcode(), rateLimited.getError(), rateLimited.getRetryAfterMs()));
        } else {
            LOGGER.log(ERROR, "Sleep milliseconds: {}", delay);
            CompletableFuture.delayedExecutor(newDelay, TimeUnit.MILLISECONDS)
                .execute(() -> invokeRequest(result, action, extractor, newDelay));
        }
    }

    private <R> void error(CompletableFuture<R> result, HttpResponse<byte[]> response) {
        LOGGER.log(DEBUG, "Error.");
        var error = extractor(ErrorResponse.class).apply(response.body());

        if (LOGGER.isLoggable(DEBUG)) {
            LOGGER.log(DEBUG, "Errcode: {}", error.getErrcode());
            LOGGER.log(DEBUG, "Error: {}", error.getError());
        }

        var status = response.statusCode();
        if (error == null) {
            result.completeExceptionally(
                new MatrixException(MatrixException.M_INTERNAL, "Missing error response.", status));
        } else {
            result.completeExceptionally(
                new MatrixException(error.getErrcode(), error.getError(), status));
        }
    }
}
