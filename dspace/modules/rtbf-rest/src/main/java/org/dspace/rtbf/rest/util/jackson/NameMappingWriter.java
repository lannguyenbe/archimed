package org.dspace.rtbf.rest.util.jackson;

import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

public class NameMappingWriter extends BeanPropertyWriter {
	BeanPropertyWriter _writer;

	protected NameMappingWriter(BeanPropertyWriter w, String targetName) {
		super(w, new SerializedString(targetName));
		_writer = w;
	}

}
