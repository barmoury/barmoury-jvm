package io.github.barmoury.api.service;

import io.github.barmoury.trace.Device;
import nl.basjes.parse.useragent.UserAgent;
import org.springframework.data.domain.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import io.github.barmoury.testing.ValueGenerator;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import io.github.barmoury.api.model.Session;
import io.github.barmoury.api.repository.BarmourySessionRepository;

import java.util.Date;
import java.util.Optional;

public abstract class SessionService<T extends Session<?>> {

    public abstract T initializeSession();
    public abstract BarmourySessionRepository<T> getBarmourySessionRepository();

    public Page<T> getActiveSessions(String sessionId, Pageable pageable) {
        getBarmourySessionRepository().updatedExpiredSessions();
        return getBarmourySessionRepository()
                .findAllBySessionIdAndStatus(sessionId, "ACTIVE", pageable);
    }

    public Optional<T> getSession(String sessionToken) {
        return getBarmourySessionRepository()
                .findBySessionToken(sessionToken);
    }

    public Optional<T> getSession(String sessionToken, String status) {
        return getBarmourySessionRepository()
                .findBySessionTokenAndStatus(sessionToken, status);
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
        T barmourySession = this.initializeSession();
        barmourySession.setActorId(actorId);
        barmourySession.setActorType(actorType);
        barmourySession.setIpAddress(ipAddress);
        barmourySession.setSessionId(sessionId);
        barmourySession.setLastAuthToken(authToken);
        barmourySession.setSessionToken(sessionToken);
        barmourySession.setExpirationDate(expiryDate);
        barmourySession.setDevice(Device.build(httpServletRequest.getHeader("User-Agent")));
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
