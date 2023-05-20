package io.github.barmoury.crypto.pgp;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import io.github.barmoury.api.config.PgpConfig;
import io.github.barmoury.crypto.pgp.PgpTranslate;
import io.github.barmoury.util.FieldUtil;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class PgpTranslateDeserializer<T extends PgpTranslate> extends JsonDeserializer<T>
        implements ContextualDeserializer {

    JavaType type;

    public PgpTranslateDeserializer(JavaType type) {
        this.type = type;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext deserializationContext,
                                                BeanProperty beanProperty) throws JsonMappingException {
        JavaType type = deserializationContext.getContextualType() != null
                ? deserializationContext.getContextualType()
                : beanProperty.getMember().getType();
        return new PgpTranslateDeserializer<>(type);
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public T deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, JacksonException {
        String namingStrategy = PgpConfig.getNamingStrategy();
        Class<T> tClass = (Class<T>) type.getRawClass();
        JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
        if (FieldUtil.isSubclassOf(tClass, PgpTranslate.class) && jsonNode.isTextual()) {
            jsonNode = PgpConfig.getObjectMapper()
                    .readTree(PgpConfig.decodeEncryptedString(jsonNode.asText()));
        }
        T body = tClass.getConstructor().newInstance();
        List<Field> fields = FieldUtil.getAllFields(tClass);
        for (Field field : fields) {
            String name = field.getName();
            if (namingStrategy.equalsIgnoreCase("SNAKE_CASE")) name = FieldUtil.toSnakeCase(name);
            if (jsonNode.has(name)) {
                JsonNode value = jsonNode.get(name);
                boolean fieldIsAccessible = field.canAccess(body);
                if (!fieldIsAccessible) field.setAccessible(true);
                if (FieldUtil.objectsHasAnyType(field.getType(), int.class, Integer.class,
                        byte.class, Byte.class, short.class, Short.class))
                { field.set(body, value.asInt()); }
                else if (FieldUtil.objectsHasAnyType(field.getType(), long.class, Long.class))
                { field.set(body, value.asLong()); }
                else if (FieldUtil.objectsHasAnyType(field.getType(), float.class, Float.class,
                        double.class, Double.class))
                { field.set(body, value.asDouble()); }
                else if (FieldUtil.objectsHasAnyType(field.getType(), char.class, Character.class))
                { field.set(body, value.asText().charAt(0)); }
                else if (FieldUtil.objectsHasAnyType(field.getType(), String.class))
                { field.set(body, value.asText()); }
                else if (FieldUtil.isSubclassOf(field.getType(), PgpTranslate.class))
                { field.set(body, PgpConfig.getObjectMapper().readValue(PgpConfig.decodeEncryptedString(value.asText()),
                        field.getType()));  }
                else
                { field.set(body, jsonParser.getCodec().treeToValue(value, field.getType())); }
                if (!fieldIsAccessible) field.setAccessible(false);
            }
        }
        return body;
    }

}
