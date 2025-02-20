/*
 * Copyright (C) 2021 Thibault B.
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
package io.github.thibaultbee.streampack.logger

import android.util.Log

/**
 * Implementation of [ILogger]. Use as default logger in StreamPack.
 * It calls Android [Log].
 */
class StreamPackLogger: ILogger {
    override fun e(obj: Any, message: String, tr: Throwable?) {
        Log.e(obj.javaClass.simpleName, message, tr)
    }

    override fun w(obj: Any, message: String, tr: Throwable?) {
        Log.w(obj.javaClass.simpleName, message, tr)
    }

    override fun i(obj: Any, message: String, tr: Throwable?) {
        Log.i(obj.javaClass.simpleName, message, tr)
    }

    override fun v(obj: Any, message: String, tr: Throwable?) {
        Log.v(obj.javaClass.simpleName, message, tr)
    }

    override fun d(obj: Any, message: String, tr: Throwable?) {
        Log.d(obj.javaClass.simpleName, message, tr)
    }
}