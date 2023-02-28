package io.github.barmoury.api.service;

import lombok.Getter;
import nl.basjes.parse.useragent.UserAgent;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import io.github.barmoury.testing.ValueGenerator;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import io.github.barmoury.api.model.BarmourySession;
import org.springframework.beans.factory.annotation.Autowired;
import io.github.barmoury.api.repository.BarmourySessionRepository;

import java.util.Date;
import java.util.Optional;

public abstract class BarmourySessionService<T extends BarmourySession<?>> {

    public abstract T initializeSession();
    public abstract BarmourySessionRepository<T> getBarmourySessionRepository();

    public Page<T> getActiveSessions(String sessionId, Pageable pageable) {
        return getBarmourySessionRepository()
                .findAllBySessionIdAndStatus(sessionId, "ACTIVE", pageable);
    }

    public Optional<T> getSelfSession(long id, String sessionId) {
        return getBarmourySessionRepository()
                .findByIdAndSessionIdAndStatus(id, sessionId, "ACTIVE");
    }

    public Optional<T> getSessionByAuth(String sessionToken, String lastAuthToken) {
        return getBarmourySessionRepository()
                .findBySessionTokenAndLastAuthTokenAndStatus(sessionToken, lastAuthToken, "ACTIVE");
    }

    public T createSession(HttpServletRequest httpServletRequest, String sessionId,
                                 String actorId, String actorType, String authToken, Date expiryDate) {
        String sessionToken = ValueGenerator.generateRandomString(50) +
                authToken.substring(authToken.length() - 30) +
                ValueGenerator.generateRandomString(String.format("%s", new Date().getTime()), 20) +
                ValueGenerator.generateRandomString(50) +
                authToken.substring(0, 50);
        String ipAddress = httpServletRequest.getRemoteAddr();
        UserAgentAnalyzer uaa = UserAgentAnalyzer.newBuilder().hideMatcherLoadStats().withCache(10000)
                .build();
        UserAgent userAgent = uaa.parse(httpServletRequest.getHeader("User-Agent"));

        BarmourySession.Device device = new BarmourySession.Device();
        device.setBrowserName(userAgent.getValue("AgentName"));
        device.setDeviceName(userAgent.getValue("DeviceName"));
        device.setDeviceType(userAgent.getValue("DeviceBrand"));
        device.setDeviceClass(userAgent.getValue("DeviceClass"));
        device.setBrowserVersion(userAgent.getValue("AgentVersion"));
        device.setOsName(userAgent.getValue("OperatingSystemName"));
        device.setEngineName(userAgent.getValue("LayoutEngineName"));
        device.setOsVersion(userAgent.getValue("OperatingSystemVersion"));
        device.setEngineVersion(userAgent.getValue("LayoutEngineVersion"));

        T barmourySession = this.initializeSession();
        barmourySession.setActorId(actorId);
        barmourySession.setActorType(actorType);
        barmourySession.setIpAddress(ipAddress);
        barmourySession.setSessionId(sessionId);
        barmourySession.setLastAuthToken(authToken);
        barmourySession.setSessionToken(sessionToken);
        barmourySession.setExpirationDate(expiryDate);
        barmourySession.setDevice(device);
        return getBarmourySessionRepository().saveAndFlush(barmourySession);
    }

    public T updateSession(T barmourySession, String authToken) {
        barmourySession.setUpdatedAt(new Date());
        barmourySession.setLastAuthToken(authToken);
        barmourySession.setRefreshCount(barmourySession.getRefreshCount() + 1);
        return getBarmourySessionRepository().saveAndFlush(barmourySession);
    }

    public T save(T barmourySession) {
        barmourySession.setUpdatedAt(new Date());
        return getBarmourySessionRepository().saveAndFlush(barmourySession);
    }

    public boolean sessionIsValid(T barmourySession) {
        if (barmourySession.getExpirationDate().before(new Date())) {
            barmourySession.setStatus("INACTIVE");
            getBarmourySessionRepository().saveAndFlush(barmourySession);
            return false;
        }
        return true;
    }

}
