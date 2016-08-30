package org.dspace.rtbf.rest.util.jackson;

import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;

// 10.12.2017 Lan : actually not use, otherwire need to be declared in RsJacksonContextResolver.java
// escape LF or CR+LF to unicode literal \u2028
public class CustomCharacterEscapes extends CharacterEscapes {

    private static final SerializedString ESCAPE_LF = new SerializedString("\u2028");
    private static final SerializedString ESCAPE_CR = new SerializedString("");

    private final int[] escapeCodesForAscii;


    public CustomCharacterEscapes() {
        escapeCodesForAscii = standardAsciiEscapesForJSON();
        escapeCodesForAscii[(int) '\n'] = ESCAPE_CUSTOM; 
        escapeCodesForAscii[(int) '\r'] = ESCAPE_CUSTOM; 
    }

    @Override
    public SerializableString getEscapeSequence(int ch) {
        switch (ch) {
            case '\n':
                return ESCAPE_LF;
            case '\r':
                /*
                 * Don't output anything for carriage return, since we're
                 * collapsing \r\n into just \u2028.
                 */
                return ESCAPE_CR;
            default:
                /*
                 * For all other chars, return a non-escaped string.
                 * This shouldn't be called since the generator uses
                 * JsonGenerator.Feature.ESCAPE_NON_ASCII,
                 * 
                 */
                return new SerializedString(new String(new char[] {(char) ch})); 
        }
    }

    @Override
    public int[] getEscapeCodesForAscii() {
        return escapeCodesForAscii;
    }

}
