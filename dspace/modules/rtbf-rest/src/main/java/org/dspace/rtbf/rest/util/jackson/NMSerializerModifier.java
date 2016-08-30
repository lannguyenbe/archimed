package org.dspace.rtbf.rest.util.jackson;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dspace.rtbf.rest.common.Constants;
import org.dspace.rtbf.rest.util.RsConfigurationManager;
import org.mortbay.log.Log;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

public class NMSerializerModifier extends BeanSerializerModifier {
    private static Logger log = Logger.getLogger(NMSerializerModifier.class);

	@Override
    public List<BeanPropertyWriter> changeProperties(
            SerializationConfig config, BeanDescription beanDesc,
            List<BeanPropertyWriter> beanProperties) {

        List<NameMapper> propertyMappings = getNameMappingsFromConfig();
        NameMapper mapper = mapperForClass(propertyMappings, beanDesc.getBeanClass());
        if (mapper == null) {
            return beanProperties;
        }
        
        List<BeanPropertyWriter> propsToWrite = new ArrayList<BeanPropertyWriter>();
        for (BeanPropertyWriter propWriter : beanProperties) {
            String propName = propWriter.getName();
            String outputName = mapper.nameMappings.getProperty(propName);
            if (outputName != null) {
            	if (outputName.isEmpty()) { // do not serialize it
                	log.warn("Name mapping " + propName + " is not serialized");
            		continue; 
            	}
                BeanPropertyWriter modifiedWriter = new NameMappingWriter(
                        propWriter, outputName);
                propsToWrite.add(modifiedWriter);
            } else {
                propsToWrite.add(propWriter);
            }
        }
        return propsToWrite;
    }

	private NameMapper mapperForClass(List<NameMapper> nameMappings, Class<?> beanClass) {
        for (NameMapper mapping : nameMappings) {
            if (mapping.classToFilter.equals(beanClass)) {
                return mapping;
            }
        }
    	log.warn("Name mapping not found for "+ beanClass.getName());
        return null;
	}


	private List<NameMapper> getNameMappingsFromConfig() {
		List<NameMapper> nameMappings = new ArrayList<>();
		Properties props;
		
		props = (Properties) RsConfigurationManager.getInstance().getAttribute(Constants.NAMINGMETA);
		if (props != null && !props.isEmpty()) {
			nameMappings.add(new NameMapper(org.dspace.rtbf.rest.common.RTBObject.class, props));
			nameMappings.add(new NameMapper(org.dspace.rtbf.rest.common.Sequence.class, props));
			nameMappings.add(new NameMapper(org.dspace.rtbf.rest.common.Episode.class, props));
			nameMappings.add(new NameMapper(org.dspace.rtbf.rest.common.Serie.class, props));
		}
		return nameMappings;
		
	}
}
