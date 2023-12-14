package tools.jackson.datatype.guava.ser;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;
import com.google.common.base.Optional;

import java.io.Serializable;
import java.util.List;

/**
 * {@link ValueSerializerModifier} needed to sneak in handler to exclude "absent"
 * optional values iff handling of "absent as nulls" is enabled.
 */
public class GuavaBeanSerializerModifier extends ValueSerializerModifier
    implements Serializable
{
    static final long serialVersionUID = 1L;
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
            BeanDescription beanDesc,
            List<BeanPropertyWriter> beanProperties)
    {
        for (int i = 0; i < beanProperties.size(); ++i) {
            final BeanPropertyWriter writer = beanProperties.get(i);
            if (Optional.class.isAssignableFrom(writer.getType().getRawClass())) {
                beanProperties.set(i, new GuavaOptionalBeanPropertyWriter(writer));
            }
        }
        return beanProperties;
    }
}