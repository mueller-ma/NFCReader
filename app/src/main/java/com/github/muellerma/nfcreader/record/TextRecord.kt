/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.muellerma.nfcreader.record

import android.app.Activity
import android.nfc.NdefRecord
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.muellerma.nfcreader.R
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * An NFC Text Record
 */
class TextRecord private constructor(private val text: String) : ParsedNdefRecord {
    /**
     * Returns the ISO/IANA language code associated with this text element.
     */

    override fun getView(
        activity: Activity,
        inflater: LayoutInflater,
        parent: ViewGroup,
        offset: Int
    ): View {
        val view = inflater.inflate(R.layout.tag_text, parent, false) as TextView
        view.text = this.text
        return view
    }

    companion object {
        // TODO: deal with text fields which span multiple NdefRecords
        fun parse(record: NdefRecord): TextRecord {
            require(record.tnf == NdefRecord.TNF_WELL_KNOWN)
            require(Arrays.equals(record.type, NdefRecord.RTD_TEXT))
            return try {
                val payload: ByteArray = record.payload
                /*
                 * payload[0] contains the "Status Byte Encodings" field, per the
                 * NFC Forum "Text Record Type Definition" section 3.2.1.
                 *
                 * bit7 is the Text Encoding Field.
                 *
                 * if (Bit_7 == 0): The text is encoded in UTF-8 if (Bit_7 == 1):
                 * The text is encoded in UTF16
                 *
                 * Bit_6 is reserved for future use and must be set to zero.
                 *
                 * Bits 5 to 0 are the length of the IANA language code.
                 */
                val textEncoding = if (payload[0].toInt() and 128 == 0) StandardCharsets.UTF_8 else StandardCharsets.UTF_16
                val languageCodeLength = payload[0].toInt() and 63
                val text = String(
                    payload,
                    languageCodeLength + 1,
                    payload.size - languageCodeLength - 1,
                    textEncoding
                )
                TextRecord(text)
            } catch (e: UnsupportedEncodingException) {
                // should never happen unless we get a malformed tag.
                throw IllegalArgumentException(e)
            }
        }

        fun isText(record: NdefRecord): Boolean {
            return try {
                parse(record)
                true
            } catch (e: IllegalArgumentException) {
                false
            }
        }
    }
}