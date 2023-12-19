package io.github.barmoury.api.controller;

import io.github.barmoury.api.MutableHttpServletRequest;
import io.github.barmoury.api.ValidationGroups;
import io.github.barmoury.api.exception.ConstraintViolationException;
import io.github.barmoury.api.exception.RouteMethodNotSupportedException;
import io.github.barmoury.api.model.ApiResponse;
import io.github.barmoury.api.model.Model;
import io.github.barmoury.api.model.UserDetails;
import io.github.barmoury.audit.Auditor;
import io.github.barmoury.copier.Copier;
import io.github.barmoury.eloquent.QueryArmoury;
import io.github.barmoury.util.FieldUtil;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.SneakyThrows;
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
import java.util.*;

// TODO validate list of entity for multiple
public abstract class Controller<T1 extends Model, T2 extends Model.Request> {

    String fineName;
    Class<T1> entityClass;
    JpaRepository<T1, Long> repository;
    @Autowired @Getter QueryArmoury queryArmoury;
    @PersistenceContext EntityManager entityManager;
    @Autowired LocalValidatorFactoryBean localValidatorFactoryBean;
    public static final String NO_RESOURCE_FORMAT_STRING = "No %s found with the specified id";
    static final String ACCESS_DENIED = "Access denied. You do not have the required role to access this endpoint";

    public void preResponse(T1 entity) {}
    public boolean resolveSubEntities() {
        return true;
    }
    public boolean skipRecursiveSubEntities() {
        return true;
    }
    public void postGetResourceById(HttpServletRequest request, Authentication authentication, T1 entity) {}
    public HttpServletRequest preQuery(MutableHttpServletRequest request, Authentication authentication) { return request; }
    public void preCreate(HttpServletRequest request, Authentication authentication, T1 entity, T2 entityRequest) {}
    public void postCreate(HttpServletRequest request, Authentication authentication, T1 entity) {}
    public void preUpdate(HttpServletRequest request, Authentication authentication, T1 entity, T2 entityRequest) {}
    public void postUpdate(HttpServletRequest request, Authentication authentication, T1 prevEntity, T1 entity) {}
    public void preDelete(HttpServletRequest request, Authentication authentication, T1 entity, Object id) {}
    public void postDelete(HttpServletRequest request, Authentication authentication, T1 entity) {}

    public <T> ResponseEntity<?> processResponse(HttpStatus httpStatus, ApiResponse<T> apiResponse) {
        return ApiResponse.build(httpStatus, apiResponse);
    }

    public <T> ResponseEntity<?> processResponse(HttpStatus httpStatus, T data, String message) {
        return processResponse(httpStatus, new ApiResponse<>(data, message));
    }

    public void setup(Class<T1> entityClass, JpaRepository<T1, Long> repository) {
        this.repository = repository;
        this.entityClass = entityClass;
        this.fineName = this.entityClass.getSimpleName();
    }

    public String validateBeforeCommit(T1 r) {
        if (r == null) return "Invalid entity";
        return null;
    }

    public boolean shouldNotHonourMethod(RouteMethod routeMethod) {
        return routeMethod == null;
    }

    public String[] getRouteMethodRoles(RouteMethod ignored) {
        return new String[0];
    }

    public T1 getResourceById(Object id) {
        return queryArmoury
                .getResourceById(repository, Long.parseLong(id.toString()),
                        String.format(NO_RESOURCE_FORMAT_STRING, fineName));
    }

    @SuppressWarnings("unchecked")
    public T2 injectUpdateFieldId(HttpServletRequest httpServletRequest,
                                               T2 resourceRequest) {
        if (!(httpServletRequest.getMethod().equals(RequestMethod.POST.name())
                || httpServletRequest.getMethod().equals(RequestMethod.PUT.name())
                || httpServletRequest.getMethod().equals(RequestMethod.PATCH.name()))) {
            return resourceRequest;
        }
        Map<String, Object> pathParameters = (Map<String, Object>) httpServletRequest
                .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathParameters.containsKey("id")) resourceRequest.updateEntityId = pathParameters.get("id");
        return resourceRequest;
    }

    @SuppressWarnings("unchecked")
    public T1 resolveRequestPayload(Authentication authentication, T2 request) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        UserDetails<?> userDetails = null;
        if (authentication != null && authentication.getPrincipal() != null && authentication.getPrincipal() instanceof UserDetails secondUserDetails) {
            userDetails = secondUserDetails;
        }
        return (T1) entityClass.getDeclaredConstructor()
                .newInstance()
                .resolve(request, queryArmoury, userDetails);
    }

    @RequestMapping(value = "/stat", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> stat(HttpServletRequest request, Authentication authentication) throws ParseException {
        if (shouldNotHonourMethod(RouteMethod.STAT)) {
            throw new RouteMethodNotSupportedException("The GET '**/stat' route is not supported for this resource");
        }
        String[] roles = getRouteMethodRoles(RouteMethod.STAT);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(request::isUserInRole)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        request = preQuery(new MutableHttpServletRequest(request), authentication);
        return processResponse(HttpStatus.OK, queryArmoury.statWithQuery(request, entityClass), String.format("%s stat fetched successfully", this.fineName));
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> index(HttpServletRequest request, Authentication authentication, Pageable pageable) {
        if (shouldNotHonourMethod(RouteMethod.INDEX)) {
            throw new RouteMethodNotSupportedException("The GET '**/' route is not supported for this resource");
        }
        String[] roles = getRouteMethodRoles(RouteMethod.INDEX);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(request::isUserInRole)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        request = preQuery(new MutableHttpServletRequest(request), authentication);
        Page<T1> resources = queryArmoury.pageQuery(request, pageable, entityClass, resolveSubEntities(),
                skipRecursiveSubEntities());
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

        if (shouldNotHonourMethod(RouteMethod.STORE)) {
            throw new RouteMethodNotSupportedException("The POST '**/' route is not supported for this resource");
        }
        String[] roles = getRouteMethodRoles(RouteMethod.STORE);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(httpServletRequest::isUserInRole)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        T1 resource = resolveRequestPayload(authentication, request);
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
    public ResponseEntity<?> storeMultiple(HttpServletRequest httpServletRequest, Authentication authentication,
                                   @Validated(ValidationGroups.Create.class)
                                   @Valid @NotEmpty(message = "The request list cannot be empty") @RequestBody
                                   List<T2> entityRequests)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        if (shouldNotHonourMethod(RouteMethod.STORE_MULTIPLE)) {
            throw new RouteMethodNotSupportedException("The POST '**/multiple' route is not supported for this resource");
        }
        String[] roles = getRouteMethodRoles(RouteMethod.STORE_MULTIPLE);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(httpServletRequest::isUserInRole)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        List<T1> resources = new ArrayList<>();
        List<Object> results = new ArrayList<>();
        for (T2 entityRequest : entityRequests) {
            Validator validator = localValidatorFactoryBean.unwrap(HibernateValidatorFactory.class )
                    .usingContext().getValidator();
            Set<? extends ConstraintViolation<?>> errors = validator
                    .validate(entityRequest, ValidationGroups.Update.class);
            if (!errors.isEmpty()) {
                throw new ConstraintViolationException(entityRequest.getClass(), errors);
            }
            T1 resource = resolveRequestPayload(authentication, entityRequest);
            this.preCreate(httpServletRequest, authentication, resource, entityRequest);
            String msg = validateBeforeCommit(resource);
            if (msg != null) throw new IllegalArgumentException(msg);
            resources.add(resource);
        }
        for (T1 resource : resources) {
            try {
                resource = repository.saveAndFlush(resource);
            } catch (Exception exception) {
                results.add(exception.getMessage());
                continue;
            }
            this.postCreate(httpServletRequest, authentication, resource);
            preResponse(resource);
            results.add(resource);
        }
        return processResponse(HttpStatus.CREATED, results,
                String.format("The %s(s) are created successfully", this.fineName));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> show(HttpServletRequest request, Authentication authentication, @PathVariable Object id) {
        if (shouldNotHonourMethod(RouteMethod.SHOW)) {
            throw new RouteMethodNotSupportedException("The GET '**/{id}' route is not supported for this resource");
        }
        String[] roles = getRouteMethodRoles(RouteMethod.SHOW);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(request::isUserInRole)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        T1 resource = getResourceById(id);
        postGetResourceById(request, authentication, resource);
        preResponse(resource);
        return processResponse(HttpStatus.OK, resource, String.format("%s fetch successfully", this.fineName));
    }

    @SneakyThrows
    @RequestMapping(value = "/{id}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(HttpServletRequest httpServletRequest, Authentication authentication,
                                    @PathVariable Object id,
                                    @RequestBody T2 request) {

        if (shouldNotHonourMethod(RouteMethod.UPDATE)) {
            throw new RouteMethodNotSupportedException("The PATCH '**/{id}' route is not supported for this resource");
        }
        String[] roles = getRouteMethodRoles(RouteMethod.UPDATE);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(httpServletRequest::isUserInRole)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        UserDetails<?> userDetails = null;
        if (authentication != null && authentication.getPrincipal() != null && authentication.getPrincipal() instanceof UserDetails secondUserDetails) {
            userDetails = secondUserDetails;
        }
        injectUpdateFieldId(httpServletRequest, request);
        T1 previousResource = getResourceById(id);
        postGetResourceById(httpServletRequest, authentication, previousResource);
        Validator validator = localValidatorFactoryBean.unwrap(HibernateValidatorFactory.class )
                .usingContext()
                .constraintValidatorPayload(previousResource.getId())
                .getValidator();
        Set<? extends ConstraintViolation<?>> errors = validator
                .validate(request, ValidationGroups.Update.class);
        if (!errors.isEmpty()) {
            throw new ConstraintViolationException(request.getClass(), errors);
        }
        entityManager.detach(previousResource);
        T1 resource = entityClass.getDeclaredConstructor()
                .newInstance();
        Copier.copyBlindly(resource, previousResource);
        resource.resolve(request, queryArmoury, userDetails);
        this.preUpdate(httpServletRequest, authentication, resource, request);
        String msg = validateBeforeCommit(resource);
        if (msg != null) throw new IllegalArgumentException(msg);
        repository.saveAndFlush(resource);
        this.postUpdate(httpServletRequest, authentication, previousResource, resource);
        preResponse(resource);
        return processResponse(HttpStatus.OK, resource, String.format("%s updated successfully", this.fineName));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> destroy(HttpServletRequest request, Authentication authentication, @PathVariable Object id) {

        if (shouldNotHonourMethod(RouteMethod.DESTROY)) {
            throw new RouteMethodNotSupportedException("The DELETE '**/{id}' route is not supported for this resource");
        }
        String[] roles = getRouteMethodRoles(RouteMethod.DESTROY);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(request::isUserInRole)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        T1 resource = getResourceById(id);
        postGetResourceById(request, authentication, resource);
        this.preDelete(request, authentication, resource, id);
        repository.delete(resource);
        this.postDelete(request, authentication, resource);
        preResponse(resource);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/multiple", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> destroyMultiple(HttpServletRequest request, Authentication authentication,
                                             @RequestBody List<Object> ids) {

        if (shouldNotHonourMethod(RouteMethod.DESTROY_MULTIPLE)) {
            throw new RouteMethodNotSupportedException("The DELETE '**/{id}' route is not supported for this resource");
        }
        String[] roles = getRouteMethodRoles(RouteMethod.DESTROY_MULTIPLE);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(request::isUserInRole)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        List<T1> resources = ids.stream().map(this::getResourceById).toList();
        for (T1 resource : resources) {
            postGetResourceById(request, authentication, resource);
            this.preDelete(request, authentication, resource, resource.getId());
            repository.delete(resource);
            this.postDelete(request, authentication, resource);
            preResponse(resource);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    public enum RouteMethod {
        STAT,
        SHOW,
        INDEX,
        STORE,
        UPDATE,
        DESTROY,
        STORE_MULTIPLE,
        DESTROY_MULTIPLE
    }

}
