package io.github.barmoury.api.controller;

import io.github.barmoury.api.model.BarmouryUserDetails;
import io.github.barmoury.api.persistence.EloquentQuery;
import io.github.barmoury.api.ValidationGroups;
import io.github.barmoury.api.model.BarmouryModel;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.Entity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.HibernateValidatorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

// TODO validate list of entity for multiple
public abstract class BarmouryController<Resource extends BarmouryModel, ResourceRequest extends BarmouryModel.Request> {

    String tableName;
    String fineName;
    Class<Resource> entityClass;
    @Autowired EntityManager entityManager;
    JpaRepository<Resource, Long> repository;
    @Autowired LocalValidatorFactoryBean localValidatorFactoryBean;
    static final String NO_RESOURCE_FORMAT_STRING = "No %s found with the specified id";

    public String validateBeforeCommit(Resource r) {
        if (r == null) return "Invalid entity";
        return null;
    }

    public void setup(Class<Resource> entityClass, JpaRepository<Resource, Long> repository) {
        this.repository = repository;
        this.entityClass = entityClass;
        Entity entity = this.entityClass.getAnnotation(Entity.class);
        this.tableName = entity.name();
        this.fineName = this.entityClass.getSimpleName();
    }

    public void preCreate(HttpServletRequest request, Authentication authentication, Resource entity, ResourceRequest entityRequest) {}

    public void postCreate(HttpServletRequest request, Authentication authentication, Resource entity) {}

    public void preUpdate(HttpServletRequest request, Authentication authentication, Resource entity, ResourceRequest entityRequest) {}

    public void postUpdate(HttpServletRequest request, Authentication authentication, Resource entity) {}

    public void preDelete(HttpServletRequest request, Authentication authentication, Resource entity, long id) {}

    public void postDelete(HttpServletRequest request, Authentication authentication, Resource entity) {}

    public abstract <T> ResponseEntity<?> processResponse(HttpStatus httpStatus, T data, String message);

    public abstract String[] getRouteMethodRoles(RouteMethod routeMethod);

    @RequestMapping(value = "/stat", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> stat(HttpServletRequest request) {
        String[] roles = getRouteMethodRoles(RouteMethod.STAT);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(request::isUserInRole)) {
            throw new AccessDeniedException("Access denied. You do not have the required role to access this endpoint");
        }
        return processResponse(HttpStatus.OK, EloquentQuery.getResourceStat(entityManager, request, tableName,
                entityClass), String.format("%s stat fetched successfully", this.fineName));
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> index(HttpServletRequest request, Pageable pageable) {
        String[] roles = getRouteMethodRoles(RouteMethod.INDEX);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(request::isUserInRole)) {
            throw new AccessDeniedException("Access denied. You do not have the required role to access this endpoint");
        }
        Page<Resource> clientCategories = EloquentQuery.buildQueryForPage(
                entityManager, tableName, entityClass,
                request, pageable);
        return processResponse(HttpStatus.OK, clientCategories, String.format("%s list fetched successfully",
                this.fineName));
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> store(HttpServletRequest httpServletRequest, Authentication authentication,
                                   @Validated(ValidationGroups.Create.class) @RequestBody
                                   ResourceRequest request)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        String[] roles = getRouteMethodRoles(RouteMethod.STORE);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(httpServletRequest::isUserInRole)) {
            throw new AccessDeniedException("Access denied. You do not have the required role to access this endpoint");
        }
        Resource resource = (Resource) entityClass.getDeclaredConstructor().newInstance().resolve(request);
        this.preCreate(httpServletRequest, authentication, resource, request);
        String msg = validateBeforeCommit(resource);
        if (msg != null) throw new IllegalArgumentException(msg);
        repository.saveAndFlush(resource);
        this.postCreate(httpServletRequest, authentication, resource);
        return processResponse(HttpStatus.CREATED, resource, String.format("%s created successfully", this.fineName));
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/multiple", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> store(HttpServletRequest httpServletRequest, Authentication authentication,
                                   @Validated(ValidationGroups.Create.class)
                                   @Valid @NotEmpty(message = "The request list cannot be empty") @RequestBody
                                   List<@Valid ResourceRequest> entityRequests)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        String[] roles = getRouteMethodRoles(RouteMethod.STORE_MULTIPLE);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(httpServletRequest::isUserInRole)) {
            throw new AccessDeniedException("Access denied. You do not have the required role to access this endpoint");
        }
        List<Object> entities = new ArrayList<>();
        for (ResourceRequest entityRequest : entityRequests) {
            Resource resource = (Resource) entityClass.getDeclaredConstructor().newInstance().resolve(entityRequest);
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
            entities.add(resource);
        }
        return processResponse(HttpStatus.CREATED, entities,
                String.format("the %s(s) are created successfully", this.fineName));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> show(HttpServletRequest request, @PathVariable long id) {
        String[] roles = getRouteMethodRoles(RouteMethod.SHOW);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(request::isUserInRole)) {
            throw new AccessDeniedException("Access denied. You do not have the required role to access this endpoint");
        }
        return processResponse(HttpStatus.OK,
                EloquentQuery.getResourceById(repository, id, String.format(NO_RESOURCE_FORMAT_STRING, fineName)),
                String.format("%s fetch successfully", this.fineName));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(HttpServletRequest httpServletRequest, Authentication authentication,
                                    @PathVariable long id, @RequestBody ResourceRequest request) {

        String[] roles = getRouteMethodRoles(RouteMethod.UPDATE);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(httpServletRequest::isUserInRole)) {
            throw new AccessDeniedException("Access denied. You do not have the required role to access this endpoint");
        }
        Resource resource = EloquentQuery.getResourceById(repository, id, String.format(NO_RESOURCE_FORMAT_STRING, fineName));
        resource.resolve(request);
        Validator validator = localValidatorFactoryBean.unwrap(HibernateValidatorFactory.class )
                .usingContext()
                .constraintValidatorPayload((resource).getId())
                .getValidator();
        List<ConstraintViolation<ResourceRequest>> errors =
                new ArrayList<>(validator.validate(request, ValidationGroups.Update.class));
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.get(0).getMessage());
        }
        this.preUpdate(httpServletRequest, authentication, resource, request);
        String msg = validateBeforeCommit(resource);
        if (msg != null) throw new IllegalArgumentException(msg);
        repository.saveAndFlush(resource);
        this.postUpdate(httpServletRequest, authentication, resource);
        return processResponse(HttpStatus.OK, resource, String.format("%s updated successfully", this.fineName));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> delete(HttpServletRequest request, Authentication authentication, @PathVariable long id) {
        String[] roles = getRouteMethodRoles(RouteMethod.DELETE);
        if (roles != null && roles.length > 0 && Arrays.stream(roles).noneMatch(request::isUserInRole)) {
            throw new AccessDeniedException("Access denied. You do not have the required role to access this endpoint");
        }
        Resource resource = EloquentQuery.getResourceById(repository, id, String.format(NO_RESOURCE_FORMAT_STRING, fineName));
        this.preDelete(request, authentication, resource, id);
        repository.delete(resource);
        this.postDelete(request, authentication, resource);
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
