package tools.jackson.datatype.guava.deser;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.NullValueProvider;
import tools.jackson.databind.deser.std.ContainerDeserializerBase;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.util.AccessPattern;
import tools.jackson.databind.util.ClassUtil;

/**
 * Base class for Guava-specific collection deserializers.
 */
public abstract class GuavaCollectionDeserializer<T>
    extends ContainerDeserializerBase<T>
{
    /**
     * Deserializer used for values contained in collection being deserialized;
     * either assigned on constructor, or during resolve().
     */
    protected final ValueDeserializer<?> _valueDeserializer;

    /**
     * If value instances have polymorphic type information, this
     * is the type deserializer that can deserialize required type
     * information
     */
    protected final TypeDeserializer _valueTypeDeserializer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected GuavaCollectionDeserializer(JavaType selfType,
            ValueDeserializer<?> deser, TypeDeserializer typeDeser,
            NullValueProvider nuller, Boolean unwrapSingle)
    {
        super(selfType, nuller, unwrapSingle);
        _valueTypeDeserializer = typeDeser;
        _valueDeserializer = deser;
    }

    /**
     * Overridable fluent factory method used for creating contextual
     * instances.
     */
    public abstract GuavaCollectionDeserializer<T> withResolved(
            ValueDeserializer<?> valueDeser, TypeDeserializer typeDeser, 
            NullValueProvider nuller, Boolean unwrapSingle);

    /**
     * Method called to finalize setup of this deserializer,
     * after deserializer itself has been registered. This
     * is needed to handle recursive and transitive dependencies.
     */
    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
    {
        Boolean unwrapSingle = findFormatFeature(ctxt, property, Collection.class,
                JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        ValueDeserializer<?> valueDeser = _valueDeserializer;
        valueDeser = findConvertingContentDeserializer(ctxt, property, valueDeser);
        TypeDeserializer valueTypeDeser = _valueTypeDeserializer;
        if (valueDeser == null) {
            valueDeser = ctxt.findContextualValueDeserializer(_containerType.getContentType(), property);
        }
        if (valueTypeDeser != null) {
            valueTypeDeser = valueTypeDeser.forProperty(property);
        }

        NullValueProvider nuller = findContentNullProvider(ctxt, property, valueDeser);

        if ( (unwrapSingle != _unwrapSingle)
                || (nuller != _nullProvider)
                || (valueDeser != _valueDeserializer)
                || (valueTypeDeser != _valueTypeDeserializer)) {
            return withResolved(valueDeser, valueTypeDeser, nuller, unwrapSingle);
        }
        return this;
    }

    /*
    /**********************************************************
    /* Base class method implementations
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    @Override
    public ValueDeserializer<Object> getContentDeserializer() {
        return (ValueDeserializer<Object>) _valueDeserializer;
    }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Collection;
    }

    /*
    /**********************************************************
    /* Deserialization interface
    /**********************************************************
     */

    /**
     * Base implementation that does not assume specific type
     * inclusion mechanism. Sub-classes are expected to override
     * this method if they are to handle type information.
     */
    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws JacksonException
    {
        return typeDeserializer.deserializeTypedFromArray(p, ctxt);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt)
            throws JacksonException
    {
        // Should usually point to START_ARRAY
        if (p.isExpectedStartArrayToken()) {
            return _deserializeContents(p, ctxt);
        }
        // But may support implicit arrays from single values?
        if (ctxt.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)) {
            return _deserializeFromSingleValue(p, ctxt);
        }
        return (T) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    /*
    /**********************************************************************
    /* Abstract methods for impl classes
    /**********************************************************************
     */

    // Force abstract-ness for subclasses
    @Override
    public abstract AccessPattern getEmptyAccessPattern();

    // Force abstract-ness for subclasses
    @Override
    public abstract Object getEmptyValue(DeserializationContext ctxt);

    protected abstract T _deserializeContents(JsonParser p, DeserializationContext ctxt)
        throws JacksonException;

    /**
     * Method used to support implicit coercion from a single non-array value
     * into single-element collection.
     */
    protected T _deserializeFromSingleValue(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        final ValueDeserializer<?> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _valueTypeDeserializer;
        final JsonToken t = p.currentToken();

        final Object value;

        if (t == JsonToken.VALUE_NULL) {
            if (_skipNullValues) {
                return _createEmpty(ctxt);
            }
            value = _nullProvider.getNullValue(ctxt);
        } else if (typeDeser == null) {
            value = valueDes.deserialize(p, ctxt);
        } else {
            value = valueDes.deserializeWithType(p, ctxt, typeDeser);
        }
        return _createWithSingleElement(ctxt, value);

    }

    protected abstract T _createEmpty(DeserializationContext ctxt);

    protected abstract T _createWithSingleElement(DeserializationContext ctxt, Object value);

    /**
     * Some/many Guava containers do not allow addition of {@code null} values,
     * so isolate handling here.
     *
     * @since 2.17
     */
    protected void _tryToAddNull(JsonParser p, DeserializationContext ctxt, Collection<?> set)
    {
        // Ideally we'd have better idea of where nulls are accepted, but first
        // let's just produce something better than NPE:
        try {
            set.add(null);
        } catch (NullPointerException e) {
            ctxt.handleUnexpectedToken(_valueType, JsonToken.VALUE_NULL, p,
                    "Guava `Collection` of type %s does not accept `null` values",
                    ClassUtil.getTypeDescription(getValueType(ctxt)));
        }
    }
}