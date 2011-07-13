package eel.kitchen.jsonschema.validators.providers;

import eel.kitchen.jsonschema.validators.EnumValidator;
import eel.kitchen.jsonschema.validators.type.BooleanValidator;
import org.codehaus.jackson.JsonNode;

public final class BooleanValidatorProvider
    extends AbstractValidatorProvider
{
    public BooleanValidatorProvider(final JsonNode schemaNode)
    {
        super(schemaNode, "boolean", BooleanValidator.class, true, false);
    }
}
