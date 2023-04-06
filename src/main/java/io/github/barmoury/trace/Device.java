package io.github.barmoury.trace;

import io.github.barmoury.converter.ObjectConverter;
import lombok.Data;
import lombok.SneakyThrows;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

import java.io.Serializable;

@Data
public class Device implements Serializable {

    String osName;
    String osVersion;
    String engineName;
    String deviceName;
    String deviceType;
    String deviceClass;
    String browserName;
    String engineVersion;
    String browserVersion;

    static UserAgentAnalyzer uaa = UserAgentAnalyzer
            .newBuilder()
            .hideMatcherLoadStats()
            .withCache(10000).build();

    public static Device build(String userAgentHeader) {
        Device device = new Device();
        UserAgent userAgent = uaa.parse(userAgentHeader);
        device.setBrowserName(userAgent.getValue("AgentName"));
        device.setDeviceName(userAgent.getValue("DeviceName"));
        device.setDeviceType(userAgent.getValue("DeviceBrand"));
        device.setDeviceClass(userAgent.getValue("DeviceClass"));
        device.setBrowserVersion(userAgent.getValue("AgentVersion"));
        device.setOsName(userAgent.getValue("OperatingSystemName"));
        device.setEngineName(userAgent.getValue("LayoutEngineName"));
        device.setOsVersion(userAgent.getValue("OperatingSystemVersion"));
        device.setEngineVersion(userAgent.getValue("LayoutEngineVersion"));
        return device;
    }

    public static class DeviceConverter extends ObjectConverter<Device> {

        @SneakyThrows
        @Override
        public Device convertToEntityAttribute(String s) {
            return objectMapper.readValue(s, Device.class);
        }

    }

}
