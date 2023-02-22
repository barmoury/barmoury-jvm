package io.github.barmoury.api.controller;

import io.github.barmoury.api.ValidationGroups;
import io.github.barmoury.api.config.JwtTokenUtil;
import io.github.barmoury.api.model.BarmouryModel;
import io.github.barmoury.api.model.BarmourySession;
import io.github.barmoury.api.model.BarmouryUserDetails;
import io.github.barmoury.api.service.BarmourySessionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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

public abstract class BarmourySessionController<T extends BarmourySession<?>> extends BarmouryController<T, BarmouryModel.Request> {

    @Autowired
    JwtTokenUtil jwtTokenUtil;

    @Autowired
    BarmourySessionService<T> barmourySessionService;

    @Override
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> store(Authentication authentication, HttpServletRequest httpServletRequest,
                                   @Validated(ValidationGroups.Create.class) @RequestBody
                                   BarmouryModel.Request request) {
        throw new UnsupportedOperationException("you cannot manually create a session");
    }

    @Override
    @RequestMapping(value = "/{id}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(Authentication authentication, HttpServletRequest httpServletRequest,
                                    @PathVariable long id, @RequestBody BarmouryModel.Request request) {
        throw new UnsupportedOperationException("a session cannot be updated");
    }

    @RequestMapping(value = "/self", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getSelfSessions(Authentication authentication, HttpServletRequest request, Pageable pageable) {
        BarmouryUserDetails<?> userDetails = (BarmouryUserDetails<?>) authentication.getPrincipal();
        return processResponse(HttpStatus.OK, barmourySessionService.getActiveSessions(userDetails.getId(), pageable),
                String.format("%s list fetched successfully", this.fineName));
    }

    @RequestMapping(value = "/self", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteSelfSessions(Authentication authentication, HttpServletRequest request) {
        BarmouryUserDetails<?> userDetails = (BarmouryUserDetails<?>) authentication.getPrincipal();
        barmourySessionService.getBarmourySessionRepository().deleteSelfSessions(userDetails.getId());
        return processResponse(HttpStatus.NO_CONTENT, null, null);
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/self/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getSelfSession(Authentication authentication, HttpServletRequest request, @PathVariable long id) {
        BarmouryUserDetails<?> userDetails = (BarmouryUserDetails<?>) authentication.getPrincipal();
        Optional<T> barmourySession = barmourySessionService.getSelfSession(id, userDetails.getId());
        if (barmourySession.isEmpty()) {
            throw new EntityNotFoundException(String.format("no session found with the specified id '%d'", id));
        }
        return processResponse(HttpStatus.OK, barmourySession.get(),
                String.format("%s fetched successfully", this.fineName));
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/self/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteSelfSession(Authentication authentication, HttpServletRequest request, @PathVariable long id) {
        BarmouryUserDetails<?> userDetails = (BarmouryUserDetails<?>) authentication.getPrincipal();
        Optional<T> barmourySession = barmourySessionService.getSelfSession(id, userDetails.getId());
        if (barmourySession.isEmpty()) {
            throw new EntityNotFoundException(String.format("no session found with the specified id '%d'", id));
        }
        barmourySessionService.getBarmourySessionRepository().delete(barmourySession.get());
        return processResponse(HttpStatus.NO_CONTENT, null, null);
    }

}
