package io.github.barmoury.trace;

import io.github.barmoury.converter.ObjectConverter;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;

@Data
@Builder
public class Domain {

    String ip;
    String name;
    String created;
    String expires;
    String changed;
    String idnName;
    String askWhois;
    Contacts contacts;
    Registrar registrar;
    String[] nameservers;

    @jakarta.persistence.Converter
    public static class Converter extends ObjectConverter<Domain> {

        @SneakyThrows
        @Override
        public Domain convertToEntityAttribute(String s) {
            return objectMapper.readValue(s, Domain.class);
        }

    }

    @Data
    @Builder
    public static class Contacts {

        Contact tech;
        Contact owner;
        Contact admin;

    }

    @Data
    @Builder
    public static class Contact {

        String name;
        String email;
        String country;
        String address;
        String organization;

    }

    @Data
    @Builder
    public static class Registrar {

        String url;
        String name;
        String email;

    }

}
