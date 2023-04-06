package io.github.barmoury.api.controller;

import io.github.barmoury.api.ValidationGroups;
import io.github.barmoury.api.model.Model;
import io.github.barmoury.api.persistence.EloquentQuery;
import io.github.barmoury.audit.Auditor;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.HibernateValidatorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// TODO validate list of entity for multiple
public abstract class Controller<T1 extends Model, T2 extends Model.Request> {

    String tableName;
    String fineName;
    Class<T1> entityClass;
    @Autowired EntityManager entityManager;
    JpaRepository<T1, Long> repository;
    @Autowired LocalValidatorFactoryBean localValidatorFactoryBean;
    static final String NO_RESOURCE_FORMAT_STRING = "No %s found with the specified id";
    static final String ACCESS_DENIED = "Access denied. You do not have the required role to access this endpoint";

    public String validateBeforeCommit(T1 r) {
        if (r == null) return "Invalid entity";
        return null;
    }

    public void setup(Class<T1> entityClass, JpaRepository<T1, Long> repository) {
        this.repository = repository;
        this.entityClass = entityClass;
        Entity entity = this.entityClass.getAnnotation(Entity.class);
        this.tableName = entity.name();
        this.fineName = this.entityClass.getSimpleName();
    }

    public void preResponse(T1 entity) {}
    public void preQuery(HttpServletRequest request) {}
    public void preCreate(HttpServletRequest request, Authentication authentication, T1 entity, T2 entityRequest) {}
    public void postCreate(HttpServletRequest request, Authentication authentication, T1 entity) {}
    public void preUpdate(HttpServletRequest request, Authentication authentication, T1 entity, T2 entityRequest) {}
    public void postUpdate(HttpServletRequest request, Authentication authentication, T1 entity) {}
    public void preDelete(HttpServletRequest request, Authentication authentication, T1 entity, long id) {}
    public void postDelete(HttpServletRequest request, Authentication authentication, T1 entity) {}

    public abstract <T> ResponseEntity<?> processResponse(HttpStatus httpStatus, T data, String message);

    public Auditor<T1> getAuditor() {
        return null;
    }

    public String[] getRouteMethodRoles(RouteMethod ignored) {
        return new String[0];
    }

    @SuppressWarnings("unchecked")
    @ModelAttribute("injectUpdateFieldId")
    public T2 injectUpdateFieldId(HttpServletRequest httpServletRequest,
                                               T2 resourceRequest) {
        if (!(httpServletRequest.getMethod().equals(RequestMethod.POST.name())
                || httpServletRequest.getMethod().equals(RequestMethod.PUT.name())
                || httpServletRequest.getMethod().equals(RequestMethod.PATCH.name()))) {
            return null;
        }
        Map<String, Long> pathParameters = (Map<String, Long>) httpServletRequest
                .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathParameters.containsKey("id")) resourceRequest.updateEntityId = pathParameters.get("id");
        return resourceRequest;
    }

    @RequestMapping(value = "/stat", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> stat(HttpServletRequest request) throws ParseException {
        String[] roles = getRouteMethodRoles(RouteMethod.STAT);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(request::isUserInRole)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        preQuery(request);
        return processResponse(HttpStatus.OK, EloquentQuery.getResourceStat(entityManager, request, tableName,
                entityClass), String.format("%s stat fetched successfully", this.fineName));
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> index(HttpServletRequest request, Pageable pageable) {
        String[] roles = getRouteMethodRoles(RouteMethod.INDEX);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(request::isUserInRole)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        preQuery(request);
        Page<T1> resources = EloquentQuery.buildQueryForPage(
                entityManager, tableName, entityClass,
                request, pageable);
        resources.forEach(this::preResponse);
        return processResponse(HttpStatus.OK, resources, String.format("%s list fetched successfully",
                this.fineName));
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> store(HttpServletRequest httpServletRequest, Authentication authentication,
                                   @Validated(ValidationGroups.Create.class) @RequestBody
                                   T2 request)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        String[] roles = getRouteMethodRoles(RouteMethod.STORE);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(httpServletRequest::isUserInRole)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        T1 resource = (T1) entityClass.getDeclaredConstructor().newInstance().resolve(request);
        this.preCreate(httpServletRequest, authentication, resource, request);
        String msg = validateBeforeCommit(resource);
        if (msg != null) throw new IllegalArgumentException(msg);
        repository.saveAndFlush(resource);
        this.postCreate(httpServletRequest, authentication, resource);
        preResponse(resource);
        return processResponse(HttpStatus.CREATED, resource, String.format("%s created successfully", this.fineName));
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/multiple", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> store(HttpServletRequest httpServletRequest, Authentication authentication,
                                   @Validated(ValidationGroups.Create.class)
                                   @Valid @NotEmpty(message = "The request list cannot be empty") @RequestBody
                                   List<@Valid T2> entityRequests)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        String[] roles = getRouteMethodRoles(RouteMethod.STORE_MULTIPLE);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(httpServletRequest::isUserInRole)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        List<Object> entities = new ArrayList<>();
        for (T2 entityRequest : entityRequests) {
            T1 resource = (T1) entityClass.getDeclaredConstructor().newInstance().resolve(entityRequest);
            this.preCreate(httpServletRequest, authentication, resource, entityRequest);
            String msg = validateBeforeCommit(resource);
            if (msg != null) {
                entities.add(resource);
                continue;
            }
            try {
                resource = repository.saveAndFlush(resource);
            } catch (Exception exception) {
                entities.add(exception.getMessage());
                continue;
            }
            this.postCreate(httpServletRequest, authentication, resource);
            preResponse(resource);
            entities.add(resource);
        }
        return processResponse(HttpStatus.CREATED, entities,
                String.format("the %s(s) are created successfully", this.fineName));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> show(HttpServletRequest request, @PathVariable long id) {
        String[] roles = getRouteMethodRoles(RouteMethod.SHOW);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(request::isUserInRole)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        T1 resource = EloquentQuery.getResourceById(repository, id,
                String.format(NO_RESOURCE_FORMAT_STRING, fineName));
        preResponse(resource);
        return processResponse(HttpStatus.OK, resource, String.format("%s fetch successfully", this.fineName));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(HttpServletRequest httpServletRequest, Authentication authentication,
                                    @PathVariable long id,
                                    @ModelAttribute("injectUpdateFieldId") @RequestBody T2 request) {

        String[] roles = getRouteMethodRoles(RouteMethod.UPDATE);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(httpServletRequest::isUserInRole)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        T1 resource = EloquentQuery.getResourceById(repository, id, String.format(NO_RESOURCE_FORMAT_STRING, fineName));
        resource.resolve(request);
        Validator validator = localValidatorFactoryBean.unwrap(HibernateValidatorFactory.class )
                .usingContext()
                .constraintValidatorPayload((resource).getId())
                .getValidator();
        List<ConstraintViolation<T2>> errors =
                new ArrayList<>(validator.validate(request, ValidationGroups.Update.class));
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.get(0).getMessage());
        }
        this.preUpdate(httpServletRequest, authentication, resource, request);
        String msg = validateBeforeCommit(resource);
        if (msg != null) throw new IllegalArgumentException(msg);
        repository.saveAndFlush(resource);
        this.postUpdate(httpServletRequest, authentication, resource);
        preResponse(resource);
        return processResponse(HttpStatus.OK, resource, String.format("%s updated successfully", this.fineName));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> delete(HttpServletRequest request, Authentication authentication, @PathVariable long id) {
        String[] roles = getRouteMethodRoles(RouteMethod.DELETE);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(request::isUserInRole)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        T1 resource = EloquentQuery.getResourceById(repository, id, String.format(NO_RESOURCE_FORMAT_STRING, fineName));
        this.preDelete(request, authentication, resource, id);
        repository.delete(resource);
        this.postDelete(request, authentication, resource);
        preResponse(resource);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    public enum RouteMethod {
        STAT,
        SHOW,
        INDEX,
        STORE,
        UPDATE,
        DELETE,
        STORE_MULTIPLE
    }

}
