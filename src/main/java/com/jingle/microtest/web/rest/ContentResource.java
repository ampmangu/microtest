package com.jingle.microtest.web.rest;

import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.jingle.microtest.domain.Contents;
import com.jingle.microtest.domain.User;
import com.jingle.microtest.repository.UserRepository;
import com.jingle.microtest.service.ContentResourceService;
import com.jingle.microtest.web.rest.errors.BadRequestAlertException;
import io.github.jhipster.web.util.HeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.cloudfoundry.com.fasterxml.jackson.annotation.JsonView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for managing {@link Contents}.
 */
@RestController
@RequestMapping("/api")
public class ContentResource {

    private final Logger log = LoggerFactory.getLogger(ContentResource.class);

    private static final String ENTITY_NAME = "contents";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ContentResourceService contentResourceService;

    private final UserRepository userRepository;

    public ContentResource(ContentResourceService contentResourceService, UserRepository userRepository) {
        this.contentResourceService = contentResourceService;
        this.userRepository = userRepository;
    }

    /**
     * {@code POST  /contents} : Create a new content.
     *
     * @param contents the content to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new contents, or with status {@code 400 (Bad Request)} if the contents has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/contents")
    public ResponseEntity<Contents> createContent(@Valid @RequestBody Contents contents, HttpServletRequest request) throws URISyntaxException {
        log.debug("REST request to save Contents : {}", contents);
        if (contents.getId() != null) {
            throw new BadRequestAlertException("A new contents cannot already have an ID", ENTITY_NAME, "idexists");
        }
        if (contents.getUserBelongsTo() == null) {
            Optional<User> optionalUser = userRepository.findOneByLogin(request.getRemoteUser());
            optionalUser.ifPresent(contents::setUserBelongsTo);
        }
        Contents result = contentResourceService.save(contents);
        //Hiding password in JSON response
        User temporal = result.getUserBelongsTo();
        temporal.setPassword("");
        result.setUserBelongsTo(temporal);
        return ResponseEntity.created(new URI("/api/contents/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /contents} : Updates an existing content.
     *
     * @param contents the content to update.
     * @return the {@link ResponseEntity} with status {@code 201 (OK)}
     * or with status {@code 400 (Bad Request)} if the contents is not valid,
     * or with status {@code 500 (Internal Server Error)} if the contents couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/contents")
    @ResponseStatus(HttpStatus.OK)
    public void updateContent(@Valid @RequestBody Contents contents, HttpServletRequest request) throws URISyntaxException {
        log.debug("REST request to update Contents : {}", contents);
        if (contents.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        Optional<Contents> realContent = contentResourceService.findOne(contents.getId());
        if (realContent.isPresent() && realContent.get().getUserBelongsTo().getLogin().equalsIgnoreCase(request.getRemoteUser()))  {
            contents.setUserBelongsTo(realContent.get().getUserBelongsTo());
            contentResourceService.save(contents);
        } else {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
    }

    /**
     * {@code GET  /contents} : get all the contents belonging to the user.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of contents in body.
     */
    @GetMapping("/contents")
    public List<Contents> getAllContents(HttpServletRequest request) {
        log.debug("REST request to get all contents");
        String username = request.getRemoteUser();
        List<Contents> contents =  contentResourceService.findAll().stream().filter(content -> content.getUserBelongsTo() != null).filter(content -> content.getUserBelongsTo().getLogin().equalsIgnoreCase(username)).collect(Collectors.toList());
        //Hiding password
        contents.forEach(content -> content.getUserBelongsTo().setPassword(""));
        return contents;
    }

    /**
     * {@code GET  /contents/:id} : get the "id" content.
     *
     * @param id the id of the content to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the content, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/contents/{id}")
    public ResponseEntity<Contents> getContent(@PathVariable Long id, HttpServletRequest request) {
        log.debug("REST request to get Contents : {}", id);
        Optional<Contents> content = contentResourceService.findOne(id);
        String username = request.getRemoteUser();
        if (content.isPresent() && content.get().getUserBelongsTo().getLogin().equalsIgnoreCase(username)) {
            //Hiding password
            content.get().getUserBelongsTo().setPassword("");
            return ResponseEntity.ok().body(content.get());
        } else {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
    }

    /**
     * {@code DELETE  /contents/:id} : delete the "id" content.
     *
     * @param id the id of the content to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/contents/{id}")
    public ResponseEntity<Void> deleteContent(@PathVariable Long id, HttpServletRequest request) {
        log.debug("REST request to delete Contents : {}", id);
        String username = request.getRemoteUser();
        Optional<Contents> content = contentResourceService.findOne(id);
        if (content.isPresent() && content.get().getUserBelongsTo() != null && content.get().getUserBelongsTo().getLogin().equalsIgnoreCase(username)) {
            contentResourceService.delete(id);
            return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString())).build();
        } else {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
    }
}
