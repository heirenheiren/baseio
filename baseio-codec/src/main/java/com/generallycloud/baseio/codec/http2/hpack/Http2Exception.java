/*
 * Copyright 2015-2017 GenerallyCloud.com
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.generallycloud.baseio.codec.http2.hpack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Http2Exception extends IOException {

    private static final long  serialVersionUID = -6941186345430164209L;
    private final Http2Error   error;
    private final ShutdownHint shutdownHint;

    public Http2Exception(Http2Error error) {
        this(error, ShutdownHint.HARD_SHUTDOWN);
    }

    public Http2Exception(Http2Error error, ShutdownHint shutdownHint) {
        this.error = error;
        this.shutdownHint = shutdownHint;
    }

    public Http2Exception(Http2Error error, String message) {
        this(error, message, ShutdownHint.HARD_SHUTDOWN);
    }

    public Http2Exception(Http2Error error, String message, ShutdownHint shutdownHint) {
        super(message);
        this.error = error;
        this.shutdownHint = shutdownHint;
    }

    public Http2Exception(Http2Error error, String message, Throwable cause) {
        this(error, message, cause, ShutdownHint.HARD_SHUTDOWN);
    }

    public Http2Exception(Http2Error error, String message, Throwable cause,
            ShutdownHint shutdownHint) {
        super(message, cause);
        this.error = error;
        this.shutdownHint = shutdownHint;
    }

    public Http2Error error() {
        return error;
    }

    /**
     * Provide a hint as to what type of shutdown should be executed. Note this hint may be ignored.
     */
    public ShutdownHint shutdownHint() {
        return shutdownHint;
    }

    /**
     * Use if an error has occurred which can not be isolated to a single stream, but instead applies
     * to the entire connection.
     * @param error The type of error as defined by the HTTP/2 specification.
     * @param fmt String with the content and format for the additional debug data.
     * @param args Objects which fit into the format defined by {@code fmt}.
     * @return An exception which can be translated into a HTTP/2 error.
     */
    public static Http2Exception connectionError(Http2Error error, String fmt, Object... args) {
        return new Http2Exception(error, String.format(fmt, args));
    }

    /**
     * Use if an error has occurred which can not be isolated to a single stream, but instead applies
     * to the entire connection.
     * @param error The type of error as defined by the HTTP/2 specification.
     * @param cause The object which caused the error.
     * @param fmt String with the content and format for the additional debug data.
     * @param args Objects which fit into the format defined by {@code fmt}.
     * @return An exception which can be translated into a HTTP/2 error.
     */
    public static Http2Exception connectionError(Http2Error error, Throwable cause, String fmt,
            Object... args) {
        return new Http2Exception(error, String.format(fmt, args), cause);
    }

    /**
     * Use if an error has occurred which can not be isolated to a single stream, but instead applies
     * to the entire connection.
     * @param error The type of error as defined by the HTTP/2 specification.
     * @param fmt String with the content and format for the additional debug data.
     * @param args Objects which fit into the format defined by {@code fmt}.
     * @return An exception which can be translated into a HTTP/2 error.
     */
    public static Http2Exception closedStreamError(Http2Error error, String fmt, Object... args) {
        return new ClosedStreamCreationException(error, String.format(fmt, args));
    }

    /**
     * Use if an error which can be isolated to a single stream has occurred.  If the {@code id} is not
     * {@link Http2CodecUtil#CONNECTION_STREAM_ID} then a {@link Http2Exception.StreamException} will be returned.
     * Otherwise the error is considered a connection error and a {@link Http2Exception} is returned.
     * @param id The stream id for which the error is isolated to.
     * @param error The type of error as defined by the HTTP/2 specification.
     * @param fmt String with the content and format for the additional debug data.
     * @param args Objects which fit into the format defined by {@code fmt}.
     * @return If the {@code id} is not
     * {@link Http2CodecUtil#CONNECTION_STREAM_ID} then a {@link Http2Exception.StreamException} will be returned.
     * Otherwise the error is considered a connection error and a {@link Http2Exception} is returned.
     */
    public static Http2Exception streamError(int id, Http2Error error, String fmt, Object... args) {
        return Http2CodecUtil.CONNECTION_STREAM_ID == id
                ? Http2Exception.connectionError(error, fmt, args)
                : new StreamException(id, error, String.format(fmt, args));
    }

    /**
     * Use if an error which can be isolated to a single stream has occurred.  If the {@code id} is not
     * {@link Http2CodecUtil#CONNECTION_STREAM_ID} then a {@link Http2Exception.StreamException} will be returned.
     * Otherwise the error is considered a connection error and a {@link Http2Exception} is returned.
     * @param id The stream id for which the error is isolated to.
     * @param error The type of error as defined by the HTTP/2 specification.
     * @param cause The object which caused the error.
     * @param fmt String with the content and format for the additional debug data.
     * @param args Objects which fit into the format defined by {@code fmt}.
     * @return If the {@code id} is not
     * {@link Http2CodecUtil#CONNECTION_STREAM_ID} then a {@link Http2Exception.StreamException} will be returned.
     * Otherwise the error is considered a connection error and a {@link Http2Exception} is returned.
     */
    public static Http2Exception streamError(int id, Http2Error error, Throwable cause, String fmt,
            Object... args) {
        return Http2CodecUtil.CONNECTION_STREAM_ID == id
                ? Http2Exception.connectionError(error, cause, fmt, args)
                : new StreamException(id, error, String.format(fmt, args), cause);
    }

    /**
     * Check if an exception is isolated to a single stream or the entire connection.
     * @param e The exception to check.
     * @return {@code true} if {@code e} is an instance of {@link Http2Exception.StreamException}.
     * {@code false} otherwise.
     */
    public static boolean isStreamError(Http2Exception e) {
        return e instanceof StreamException;
    }

    /**
     * Get the stream id associated with an exception.
     * @param e The exception to get the stream id for.
     * @return {@link Http2CodecUtil#CONNECTION_STREAM_ID} if {@code e} is a connection error.
     * Otherwise the stream id associated with the stream error.
     */
    public static int streamId(Http2Exception e) {
        return isStreamError(e) ? ((StreamException) e).streamId()
                : Http2CodecUtil.CONNECTION_STREAM_ID;
    }

    /**
     * Provides a hint as to if shutdown is justified, what type of shutdown should be executed.
     */
    public static enum ShutdownHint {
        /**
         * Do not shutdown the underlying channel.
         */
        NO_SHUTDOWN,
        /**
         * Attempt to execute a "graceful" shutdown. The definition of "graceful" is left to the implementation.
         * An example of "graceful" would be wait for some amount of time until all active streams are closed.
         */
        GRACEFUL_SHUTDOWN,
        /**
         * Close the channel immediately after a {@code GOAWAY} is sent.
         */
        HARD_SHUTDOWN;
    }

    /**
     * Used when a stream creation attempt fails but may be because the stream was previously closed.
     */
    public static final class ClosedStreamCreationException extends Http2Exception {
        private static final long serialVersionUID = -6746542974372246206L;

        public ClosedStreamCreationException(Http2Error error) {
            super(error);
        }

        public ClosedStreamCreationException(Http2Error error, String message) {
            super(error, message);
        }

        public ClosedStreamCreationException(Http2Error error, String message, Throwable cause) {
            super(error, message, cause);
        }
    }

    /**
     * Represents an exception that can be isolated to a single stream (as opposed to the entire connection).
     */
    public static final class StreamException extends Http2Exception {
        private static final long serialVersionUID = 602472544416984384L;
        private final int         streamId;

        StreamException(int streamId, Http2Error error, String message) {
            super(error, message, ShutdownHint.NO_SHUTDOWN);
            this.streamId = streamId;
        }

        StreamException(int streamId, Http2Error error, String message, Throwable cause) {
            super(error, message, cause, ShutdownHint.NO_SHUTDOWN);
            this.streamId = streamId;
        }

        public int streamId() {
            return streamId;
        }
    }

    /**
     * Provides the ability to handle multiple stream exceptions with one throw statement.
     */
    public static final class CompositeStreamException extends Http2Exception
            implements Iterable<StreamException> {
        private static final long           serialVersionUID = 7091134858213711015L;
        private final List<StreamException> exceptions;

        public CompositeStreamException(Http2Error error, int initialCapacity) {
            super(error, ShutdownHint.NO_SHUTDOWN);
            exceptions = new ArrayList<>(initialCapacity);
        }

        public void add(StreamException e) {
            exceptions.add(e);
        }

        @Override
        public Iterator<StreamException> iterator() {
            return exceptions.iterator();
        }
    }

}
