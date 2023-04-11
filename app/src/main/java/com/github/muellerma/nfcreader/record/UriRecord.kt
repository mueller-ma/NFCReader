/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.github.muellerma.nfcreader.record

import android.app.Activity
import android.net.Uri
import android.nfc.NdefRecord
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.muellerma.nfcreader.R
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * A parsed record containing a Uri.
 */
class UriRecord private constructor(private val uri: Uri) : ParsedNdefRecord {
    override fun getView(
        activity: Activity,
        inflater: LayoutInflater,
        parent: ViewGroup,
        offset: Int
    ): View {
        val text = inflater.inflate(R.layout.tag_text, parent, false) as TextView
        text.autoLinkMask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS
        text.text = uri.toString()
        return text
    }

    companion object {
        /**
         * NFC Forum "URI Record Type Definition"
         *
         * This is a mapping of "URI Identifier Codes" to URI string prefixes,
         * per section 3.2.2 of the NFC Forum URI Record Type Definition document.
         */
        private val URI_PREFIX_MAP = mapOf(
            0x00.toByte() to "",
            0x01.toByte() to "http://www.",
            0x02.toByte() to "https://www.",
            0x03.toByte() to "http://",
            0x04.toByte() to "https://",
            0x05.toByte() to "tel:",
            0x06.toByte() to "mailto:",
            0x07.toByte() to "ftp://anonymous:anonymous@",
            0x08.toByte() to "ftp://ftp.",
            0x09.toByte() to "ftps://",
            0x0A.toByte() to "sftp://",
            0x0B.toByte() to "smb://",
            0x0C.toByte() to "nfs://",
            0x0D.toByte() to "ftp://",
            0x0E.toByte() to "dav://",
            0x0F.toByte() to "news:",
            0x10.toByte() to "telnet://",
            0x11.toByte() to "imap:",
            0x12.toByte() to "rtsp://",
            0x13.toByte() to "urn:",
            0x14.toByte() to "pop:",
            0x15.toByte() to "sip:",
            0x16.toByte() to "sips:",
            0x17.toByte() to "tftp:",
            0x18.toByte() to "btspp://",
            0x19.toByte() to "btl2cap://",
            0x1A.toByte() to "btgoep://",
            0x1B.toByte() to "tcpobex://",
            0x1C.toByte() to "irdaobex://",
            0x1D.toByte() to "file://",
            0x1E.toByte() to "urn:epc:id:",
            0x1F.toByte() to "urn:epc:tag:",
            0x20.toByte() to "urn:epc:pat:",
            0x21.toByte() to "urn:epc:raw:",
            0x22.toByte() to "urn:epc:",
            0x23.toByte() to "urn:nfc:"
        )

        /**
         * Convert [NdefRecord] into a [Uri].
         * This will handle both TNF_WELL_KNOWN / RTD_URI and TNF_ABSOLUTE_URI.
         *
         * @throws IllegalArgumentException if the NdefRecord is not a record
         * containing a URI.
         */
        fun parse(record: NdefRecord): UriRecord {
            val tnf = record.tnf
            if (tnf == NdefRecord.TNF_WELL_KNOWN) {
                return parseWellKnown(record)
            } else if (tnf == NdefRecord.TNF_ABSOLUTE_URI) {
                return parseAbsolute(record)
            }
            throw IllegalArgumentException("Unknown TNF $tnf")
        }

        /** Parse and absolute URI record  */
        private fun parseAbsolute(record: NdefRecord): UriRecord {
            val payload: ByteArray = record.payload
            val uri = Uri.parse(String(payload, StandardCharsets.UTF_8))
            return UriRecord(uri)
        }

        /** Parse an well known URI record  */
        private fun parseWellKnown(record: NdefRecord): UriRecord {
            require(Arrays.equals(record.type, NdefRecord.RTD_URI))
            val payload: ByteArray = record.payload
            /*
             * payload[0] contains the URI Identifier Code, per the
             * NFC Forum "URI Record Type Definition" section 3.2.2.
             *
             * payload[1]...payload[payload.length - 1] contains the rest of
             * the URI.
             */
            val prefix: String = URI_PREFIX_MAP[payload[0]]!! // TODO

            val outputStream = ByteArrayOutputStream()
            outputStream.write(prefix.toByteArray(StandardCharsets.UTF_8))
            outputStream.write(payload.copyOfRange(1, payload.size))
            val fullUri: ByteArray = outputStream.toByteArray()

            val uri = Uri.parse(String(fullUri, StandardCharsets.UTF_8))
            return UriRecord(uri)
        }

        fun isUri(record: NdefRecord): Boolean {
            return try {
                parse(record)
                true
            } catch (e: IllegalArgumentException) {
                false
            }
        }
    }
}