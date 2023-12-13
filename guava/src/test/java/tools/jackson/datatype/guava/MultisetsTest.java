package tools.jackson.datatype.guava;

import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import com.google.common.collect.*;

/**
 * Unit tests to verify handling of various {@link Multiset}s.
 * 
 * @author tsaloranta
 */
public class MultisetsTest extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Unit tests for verifying handling in absence of module registration
    /**********************************************************************
     */
    
    /**
     * Multi-sets can actually be serialized as regular collections, without
     * problems.
     */
    public void testWithoutSerializers() throws Exception
    {
        
        ObjectMapper mapper = new ObjectMapper();
        Multiset<String> set = LinkedHashMultiset.create();
        // hash-based multi-sets actually keeps 'same' instances together, otherwise insertion-ordered:
        set.add("abc");
        set.add("foo");
        set.add("abc");
        String json = mapper.writeValueAsString(set);
        assertEquals("[\"abc\",\"abc\",\"foo\"]", json);
    }

    // 11-Jul-2017, tatu: Not sure if this test makes sense actually...
    public void testWithoutDeserializers() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            /*Multiset<String> set =*/ mapper.readValue("[\"abc\",\"abc\",\"foo\"]",
                    new TypeReference<Multiset<String>>() { });
            fail("Should have failed");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "cannot find a deserializer");
        }
    }

    /*
    /**********************************************************************
    /* Unit tests for actual registered module
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = mapperWithModule();
    
    public void testDefaultMultiset() throws Exception
    {
        _testMultiset(new TypeReference<Multiset<String>>() { });
    }
    
    public void testDefaultSortedMultiset() throws Exception {
        _testMultiset(new TypeReference<SortedMultiset<String>>() { });
    }

    public void testLinkedHashMultiset() throws Exception {
        _testMultiset(new TypeReference<LinkedHashMultiset<String>>() { });
    }
    
    public void testHashMultiset() throws Exception {
        _testMultiset(new TypeReference<HashMultiset<String>>() { });
    }
    
    public void testTreeMultiset() throws Exception {
        _testMultiset(new TypeReference<TreeMultiset<String>>() { });
    }
    
    public void testImmutableMultiset() throws Exception {
        _testMultiset(new TypeReference<ImmutableMultiset<String>>() { });
    }

    public void testImmutableSortedMultiset() throws Exception {
        _testMultiset(new TypeReference<ImmutableSortedMultiset<String>>() { });
    }

    private <T extends Multiset<String>> void _testMultiset(TypeReference<T> typeRef)
        throws Exception
    {
        T set = MAPPER.readValue("[\"abc\",\"abc\",\"foo\"]", typeRef);
        assertEquals(3, set.size());
        assertEquals(1, set.count("foo"));
        assertEquals(2, set.count("abc"));
        assertEquals(0, set.count("bar"));
    }
    
    public void testFromSingle() throws Exception
    {
        ObjectMapper mapper = builderWithModule()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .build();
        Multiset<String> set = mapper.readValue("\"abc\"",
                new TypeReference<Multiset<String>>() { });
        assertEquals(1, set.size());
        assertTrue(set.contains("abc"));
    }
}
