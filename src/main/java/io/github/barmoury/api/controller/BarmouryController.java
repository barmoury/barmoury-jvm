package io.github.barmoury.api.controller;

import io.github.barmoury.api.persistence.EloquentQuery;
import io.github.barmoury.api.ValidationGroups;
import io.github.barmoury.api.model.BarmouryModel;
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
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BarmouryController<Entity extends BarmouryModel, EntityRequest extends BarmouryModel.Request> {

    @Setter String tableName;
    @Getter @Setter String fineName;
    final String NO_RESOURCE_FORMAT_STRING = "No %s found with the specified id";
    @Setter Class<Entity> entityClass;
    @Autowired EntityManager entityManager;
    @Setter JpaRepository<Entity, Long> repository;
    Map<String, String> statMap = new HashMap<>();
    @Autowired LocalValidatorFactoryBean localValidatorFactoryBean;

    public BarmouryController() {
        statMap.put("total_count", "");
    }

    public void putStatColumn(String key, String value) {
        statMap.put(key, value);
    }

    public String removeStatColumn(String key) {
        return statMap.remove(key);
    }

    public String validateBeforeCommit(Entity r) {
        if (r == null) return "Invalid entity";
        return null;
    }

    public void preCreate(HttpServletRequest request, Entity entity, EntityRequest entityRequest) {}

    public void postCreate(HttpServletRequest request, Entity entity) {}

    public void preUpdate(HttpServletRequest request, Entity entity, EntityRequest entityRequest) {}

    public void postUpdate(HttpServletRequest request, Entity entity) {}

    public void preDelete(HttpServletRequest request, Entity entity, long id) {}

    public void postDelete(HttpServletRequest request, Entity entity) {}

    public abstract <T> ResponseEntity<?> processResponse(HttpStatus httpStatus, T data, String message);

    @RequestMapping(value = "/stat", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> stat(Authentication authentication, HttpServletRequest request) {
        if (statMap == null) {
            throw new UnsupportedOperationException(
                    String.format("The /stat route is not supported for %s", fineName));
        }
        return processResponse(HttpStatus.OK, EloquentQuery.getResourceStat(entityManager, tableName,
                statMap, request), String.format("%s stat fetched successfully", this.fineName));
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> index(Authentication authentication, HttpServletRequest request, Pageable pageable) {
        Page<Entity> clientCategories = EloquentQuery.buildQueryForPage(
                entityManager, tableName, entityClass,
                request, pageable);
        return processResponse(HttpStatus.OK, clientCategories, String.format("%s list fetched successfully",
                this.fineName));
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> store(Authentication authentication, HttpServletRequest httpServletRequest,
                                   @Validated(ValidationGroups.Create.class) @RequestBody
                                   EntityRequest request)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        Entity resource = (Entity) entityClass.getDeclaredConstructor().newInstance().resolve(request);
        this.preCreate(httpServletRequest, resource, request);
        String msg = validateBeforeCommit(resource);
        if (msg != null) throw new IllegalArgumentException(msg);
        repository.saveAndFlush(resource);
        this.postCreate(httpServletRequest, resource);
        return processResponse(HttpStatus.CREATED, resource, String.format("%s created successfully", this.fineName));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> show(Authentication authentication, HttpServletRequest request, @PathVariable long id) {
        return processResponse(HttpStatus.OK,
                EloquentQuery.getResourceById(repository, id, String.format(NO_RESOURCE_FORMAT_STRING, fineName)),
                String.format("%s fetch successfully", this.fineName));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(Authentication authentication, HttpServletRequest httpServletRequest,
                                    @PathVariable long id, @RequestBody EntityRequest request) {

        Entity resource = EloquentQuery.getResourceById(repository, id, String.format(NO_RESOURCE_FORMAT_STRING, fineName));
        resource.resolve(request);
        Validator validator = localValidatorFactoryBean.unwrap(HibernateValidatorFactory.class )
                .usingContext()
                .constraintValidatorPayload((resource).getId())
                .getValidator();
        List<ConstraintViolation<EntityRequest>> errors =
                new ArrayList<>(validator.validate(request, ValidationGroups.Update.class));
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.get(0).getMessage());
        }
        this.preUpdate(httpServletRequest, resource, request);
        String msg = validateBeforeCommit(resource);
        if (msg != null) throw new IllegalArgumentException(msg);
        repository.saveAndFlush(resource);
        this.postUpdate(httpServletRequest, resource);
        return processResponse(HttpStatus.OK, resource, String.format("%s updated successfully", this.fineName));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> delete(Authentication authentication, HttpServletRequest request, @PathVariable long id) {
        Entity resource = EloquentQuery.getResourceById(repository, id, String.format(NO_RESOURCE_FORMAT_STRING, fineName));
        this.preDelete(request, resource, id);
        repository.delete(resource);
        this.postDelete(request, resource);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
