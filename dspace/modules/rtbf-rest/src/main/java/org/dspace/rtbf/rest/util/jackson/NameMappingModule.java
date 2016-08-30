package org.dspace.rtbf.rest.util.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;

public class NameMappingModule extends Module {

	@Override
	public String getModuleName() {
		return "Local name map module";
	}

	@Override
	public void setupModule(SetupContext context) {
        context.addBeanSerializerModifier(new NMSerializerModifier());
	}

	@Override
	public Version version() {
		return Version.unknownVersion();
	}

}
