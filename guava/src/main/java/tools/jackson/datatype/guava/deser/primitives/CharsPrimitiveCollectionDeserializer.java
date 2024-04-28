package tools.jackson.datatype.guava.deser.primitives;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.datatype.guava.util.PrimitiveTypes;

import com.google.common.primitives.Chars;

import java.util.Collection;
import java.util.List;

public class CharsPrimitiveCollectionDeserializer
        extends BaseGuavaPrimitivesCollectionDeserializer<Character, List<Character>, Collection<Character>> {
    public CharsPrimitiveCollectionDeserializer() {
        super(PrimitiveTypes.CharsType, Character.class);
    }

    @Override
    protected Character asPrimitive(JsonParser parser) throws JacksonException {
        return (char) parser.getValueAsString().toCharArray()[0];
    }

    @Override
    protected List<Character> finish(Collection<Character> characters) {
        return Chars.asList(Chars.toArray(characters));
    }
}