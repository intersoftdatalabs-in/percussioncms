/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.services.error;

import com.percussion.i18n.PSI18nUtils;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Modern Java 11 base class for unchecked runtime exceptions in the services package.
 *
 * <p>This exception class provides comprehensive support for internationalized error messages
 * with both direct message construction and message key-based lookup from SystemResources.tmx.
 * It offers both traditional exception handling and modern Java 11 patterns with Optional
 * and factory method support.
 *
 * <p>The class supports two types of messages:
 * <ul>
 *   <li><strong>Raw messages:</strong> Direct string messages provided by callers</li>
 *   <li><strong>Internationalized messages:</strong> Message keys with arguments for i18n lookup</li>
 * </ul>
 *
 * <p>All constructors include comprehensive validation and null safety following
 * Java 11 best practices.
 *
 * @author Yu-Bing Chen
 * @since Java 11 Modernization
 */
public class PSRuntimeException extends RuntimeException {

    /**
     * Serial version UID for serialization compatibility.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The key of the internationalized message defined in SystemResources.tmx.
     * Set by {@link #setMsgKeyAndArgs(String, Object[])} or constructor variants.
     */
    private String msgKey;

    /**
     * The arguments for the internationalized message defined in SystemResources.tmx.
     * Set by {@link #setMsgKeyAndArgs(String, Object[])} or constructor variants.
     */
    private Object[] msgArgs;

    /**
     * The raw message provided directly by the caller.
     * Takes precedence over internationalized messages when present.
     */
    private String rawMessage;

    /**
     * Default constructor creating an exception without a specific message.
     */
    public PSRuntimeException() {
        super();
    }

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param errorMsg the detail message, may be null
     */
    public PSRuntimeException(String errorMsg) {
        super(errorMsg);
        this.rawMessage = errorMsg;
    }

    /**
     * Constructs an exception with the specified cause.
     *
     * @param cause the cause of the exception, may be null
     */
    public PSRuntimeException(Throwable cause) {
        super(cause);
        this.rawMessage = cause != null ? cause.getMessage() : null;
    }

    /**
     * Constructs an exception with the specified detail message and cause.
     *
     * @param errorMsg the detail message, may be null
     * @param cause the cause of the exception, may be null
     */
    public PSRuntimeException(String errorMsg, Throwable cause) {
        super(errorMsg, cause);
        this.rawMessage = errorMsg;
    }

    /**
     * Constructs an exception with internationalized message support.
     *
     * @param msgKey the message key for SystemResources.tmx lookup, must not be null or empty
     * @param msgArgs the arguments for message formatting, must not be null
     * @throws IllegalArgumentException if msgKey is null/empty or msgArgs is null
     */
    public PSRuntimeException(String msgKey, Object... msgArgs) {
        super();
        setMsgKeyAndArgs(msgKey, msgArgs);
    }

    /**
     * Constructs an exception with internationalized message support and a cause.
     *
     * @param cause the cause of the exception, may be null
     * @param msgKey the message key for SystemResources.tmx lookup, must not be null or empty
     * @param msgArgs the arguments for message formatting, must not be null
     * @throws IllegalArgumentException if msgKey is null/empty or msgArgs is null
     */
    public PSRuntimeException(Throwable cause, String msgKey, Object... msgArgs) {
        super(cause);
        setMsgKeyAndArgs(msgKey, msgArgs);
    }

    /**
     * Sets both message key and arguments for internationalized error messages.
     *
     * <p>This method configures the exception to use message key-based internationalization
     * rather than raw string messages. The message key should correspond to an entry
     * in SystemResources.tmx.
     *
     * @param key the message key defined in SystemResources.tmx, must not be null or empty
     * @param args the arguments for message formatting, must not be null
     * @throws IllegalArgumentException if key is null/empty or args is null
     */
    protected final void setMsgKeyAndArgs(String key, Object... args) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Message key cannot be null or empty");
        }
        Objects.requireNonNull(args, "Message arguments cannot be null");

        this.msgKey = key;
        this.msgArgs = args.clone(); // Defensive copy to prevent external modification
    }

    /**
     * Gets the message key used for internationalization.
     *
     * @return an Optional containing the message key, or empty if using raw messages
     */
    public Optional<String> getMessageKey() {
        return Optional.ofNullable(msgKey);
    }

    /**
     * Gets the message arguments used for internationalization.
     *
     * @return an Optional containing a copy of the message arguments, or empty if using raw messages
     */
    public Optional<Object[]> getMessageArgs() {
        return Optional.ofNullable(msgArgs).map(Object[]::clone);
    }

    /**
     * Gets the raw message if one was provided directly.
     *
     * @return an Optional containing the raw message, or empty if using internationalized messages
     */
    public Optional<String> getRawMessage() {
        return Optional.ofNullable(rawMessage);
    }

    /**
     * Checks if this exception uses internationalized messages.
     *
     * @return true if using message key-based internationalization, false if using raw messages
     */
    public boolean hasInternationalizedMessage() {
        return msgKey != null;
    }

    @Override
    public String getMessage() {
        // Raw message takes precedence over internationalized message
        if (rawMessage != null) {
            return rawMessage;
        }

        // Use internationalized message if available
        if (msgKey != null) {
            try {
                var text = PSI18nUtils.getString(msgKey);
                return MessageFormat.format(text, msgArgs);
            } catch (Exception e) {
                // Fallback to key and args if internationalization fails
                return String.format("Message key: %s, Args: %s", msgKey,
                    msgArgs != null ? java.util.Arrays.toString(msgArgs) : "[]");
            }
        }

        // Fallback to superclass behavior
        return super.getMessage();
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }

    /**
     * Creates a new exception with a formatted message using the provided template and arguments.
     *
     * @param messageTemplate the message template with placeholders, must not be null
     * @param args the arguments for formatting, may be empty
     * @return a new PSRuntimeException with the formatted message
     * @throws IllegalArgumentException if messageTemplate is null
     */
    public static PSRuntimeException withFormattedMessage(String messageTemplate, Object... args) {
        Objects.requireNonNull(messageTemplate, "Message template cannot be null");
        var formattedMessage = String.format(messageTemplate, args);
        return new PSRuntimeException(formattedMessage);
    }

    /**
     * Creates a new exception with a formatted message and cause.
     *
     * @param cause the cause of the exception, may be null
     * @param messageTemplate the message template with placeholders, must not be null
     * @param args the arguments for formatting, may be empty
     * @return a new PSRuntimeException with the formatted message and cause
     * @throws IllegalArgumentException if messageTemplate is null
     */
    public static PSRuntimeException withFormattedMessage(Throwable cause, String messageTemplate, Object... args) {
        Objects.requireNonNull(messageTemplate, "Message template cannot be null");
        var formattedMessage = String.format(messageTemplate, args);
        return new PSRuntimeException(formattedMessage, cause);
    }

    /**
     * Creates a new exception with internationalized message support.
     *
     * @param msgKey the message key for SystemResources.tmx lookup, must not be null or empty
     * @param msgArgs the arguments for message formatting, must not be null
     * @return a new PSRuntimeException with internationalized message
     * @throws IllegalArgumentException if msgKey is null/empty or msgArgs is null
     */
    public static PSRuntimeException withInternationalizedMessage(String msgKey, Object... msgArgs) {
        return new PSRuntimeException(msgKey, msgArgs);
    }

    /**
     * Creates a new exception lazily using a message supplier.
     *
     * @param messageSupplier the supplier for the exception message, must not be null
     * @return a new PSRuntimeException with the supplied message
     * @throws IllegalArgumentException if messageSupplier is null
     */
    public static PSRuntimeException withLazyMessage(Supplier<String> messageSupplier) {
        Objects.requireNonNull(messageSupplier, "Message supplier cannot be null");
        return new PSRuntimeException(messageSupplier.get());
    }

    @Override
    public String toString() {
        var className = getClass().getName();
        var message = getLocalizedMessage();
        return message != null ? className + ": " + message : className;
    }
}
