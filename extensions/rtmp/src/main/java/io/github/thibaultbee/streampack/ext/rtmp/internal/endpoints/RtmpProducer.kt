/*
 * Copyright (C) 2022 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.ext.rtmp.internal.endpoints

import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.endpoints.ILiveEndpoint
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.logger.ILogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import video.api.rtmpdroid.Rtmp
import java.net.SocketException

class RtmpProducer(
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val logger: ILogger
) :
    ILiveEndpoint {
    override var onConnectionListener: OnConnectionListener? = null

    private var socket = Rtmp()
    private var isOnError = false

    private var _isConnected = false
    override val isConnected: Boolean
        get() = _isConnected

    override fun configure(config: Int) {
    }

    override suspend fun connect(url: String) {
        withContext(coroutineDispatcher) {
            try {
                isOnError = false
                socket.connect("$url live=1 flashver=FMLE/3.0\\20(compatible;\\20FMSc/1.0)")
                _isConnected = true
                onConnectionListener?.onSuccess()
            } catch (e: Exception) {
                socket = Rtmp()
                _isConnected = false
                onConnectionListener?.onFailed(e.message ?: "Unknown error")
                throw e
            }
        }
    }

    override fun disconnect() {
        synchronized(this) {
            socket.close()
            _isConnected = false
            socket = Rtmp()
        }
    }

    override fun write(packet: Packet) {
        if (isOnError) return

        try {
            synchronized(this) {
                socket.write(packet.buffer)
            }
        } catch (e: SocketException) {
            disconnect()
            isOnError = true
            _isConnected = false
            onConnectionListener?.onLost(e.message ?: "Socket error")
            logger.e(this, "Error while writing packet to socket", e)
            throw e
        }
    }

    override fun startStream() {
        socket.connectStream()
    }

    override fun stopStream() {
        if (isConnected) {
            /**
             * deleteStream is blocking, if the connection does not exist yet.
             */
            socket.deleteStream()
        }
    }

    override fun release() {
    }
}