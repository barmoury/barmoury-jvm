package io.github.barmoury.api.controller;

import io.github.barmoury.api.MutableHttpServletRequest;
import io.github.barmoury.api.ValidationGroups;
import io.github.barmoury.api.config.JwtTokenUtil;
import io.github.barmoury.api.model.Model;
import io.github.barmoury.api.model.Session;
import io.github.barmoury.api.model.UserDetails;
import io.github.barmoury.api.service.SessionService;
import io.github.barmoury.eloquent.QueryArmoury;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Optional;

public abstract class SessionController<T extends Session<?>, L> extends Controller<T, Model.Request> {

    @Autowired
    JwtTokenUtil jwtTokenUtil;

    @Autowired
    SessionService<T, L> sessionService;

    @Override
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> store(HttpServletRequest httpServletRequest, Authentication authentication,
                                   @Validated(ValidationGroups.Create.class) @RequestBody
                                   Model.Request request) {
        throw new UnsupportedOperationException("you cannot manually create a session");
    }

    @Override
    @RequestMapping(value = "/{id}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(HttpServletRequest httpServletRequest, Authentication authentication,
                                    @PathVariable Object id, @RequestBody Model.Request request) {
        throw new UnsupportedOperationException("A session cannot be updated");
    }

    @RequestMapping(value = "/self", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getSelfSessions(Authentication authentication, HttpServletRequest request, Pageable pageable) {
        UserDetails<?> userDetails = (UserDetails<?>) authentication.getPrincipal();
        MutableHttpServletRequest mutableHttpServletRequest = new MutableHttpServletRequest(request);
        mutableHttpServletRequest.addParameter("actor_id", userDetails.getId());
        mutableHttpServletRequest.addParameter("status", "ACTIVE");
        return super.sIndex(mutableHttpServletRequest, authentication, pageable, true);
    }

    @RequestMapping(value = "/self", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteSelfSessions(Authentication authentication, HttpServletRequest request) {
        UserDetails<?> userDetails = (UserDetails<?>) authentication.getPrincipal();
        sessionService.getBarmourySessionRepository().deleteSelfSessions(userDetails.getId());
        return processResponse(HttpStatus.NO_CONTENT, null, null);
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/self/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getSelfSession(Authentication authentication, HttpServletRequest request, @PathVariable L id) {
        UserDetails<?> userDetails = (UserDetails<?>) authentication.getPrincipal();
        Optional<T> barmourySession = sessionService.getSelfSession(id, userDetails.getId());
        if (barmourySession.isEmpty()) {
            throw new EntityNotFoundException(String.format("no session found with the specified id '%d'", id));
        }
        preResponse(request, authentication, barmourySession.get());
        return processResponse(HttpStatus.OK, barmourySession.get(),
                String.format("%s fetched successfully", this.fineName));
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/self/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteSelfSession(Authentication authentication, HttpServletRequest request, @PathVariable L id) {
        UserDetails<?> userDetails = (UserDetails<?>) authentication.getPrincipal();
        Optional<T> barmourySession = sessionService.getSelfSession(id, userDetails.getId());
        if (barmourySession.isEmpty()) {
            throw new EntityNotFoundException(String.format("no session found with the specified id '%d'", id));
        }
        sessionService.getBarmourySessionRepository().delete(barmourySession.get());
        return processResponse(HttpStatus.NO_CONTENT, null, null);
    }

}
