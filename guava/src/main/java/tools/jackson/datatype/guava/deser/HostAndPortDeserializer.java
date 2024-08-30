package tools.jackson.datatype.guava.deser;

import tools.jackson.core.*;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.FromStringDeserializer;

import com.google.common.net.HostAndPort;

public class HostAndPortDeserializer extends FromStringDeserializer<HostAndPort>
{
    public final static HostAndPortDeserializer std = new HostAndPortDeserializer();
    
    public HostAndPortDeserializer() { super(HostAndPort.class); }

    @Override
    public HostAndPort deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        // Need to override this method, which otherwise would work just fine,
        // since we have legacy JSON Object format to support too:
        if (p.hasToken(JsonToken.START_OBJECT)) { // old style
            JsonNode root = p.readValueAsTree();
            // [datatypes-collections#45]: we actually have 2 possibilities depending on Guava version
            String host;

            JsonNode hostNode = root.get("host");
            if (hostNode == null) {
                hostNode = root.get("hostText");
            }
            if (hostNode == null || hostNode.isNull()) {
                host = "";
            } else {
                host = hostNode.asText();
            }
            JsonNode n = root.get("port");
            if (n == null) {
                return HostAndPort.fromString(host);
            }
            return HostAndPort.fromParts(host, n.asInt());
        }
        return super.deserialize(p, ctxt);
    }

    @Override
    protected HostAndPort _deserialize(String value, DeserializationContext ctxt)
        throws JacksonException
    {
        return HostAndPort.fromString(value);
    }
}