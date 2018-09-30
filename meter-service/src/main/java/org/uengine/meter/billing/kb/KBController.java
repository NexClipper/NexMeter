package org.uengine.meter.billing.kb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Created by uengine on 2017. 8. 9..
 * <p>
 * Implementation Principles: - REST Maturity Level : 3 (Hateoas)
 * - Not using old uEngine ProcessManagerBean, this replaces the ProcessManagerBean
 * - ResourceManager and CachedResourceManager will be used for definition caching (Not to use the old DefinitionFactory)
 * - json must be Typed JSON to enable object polymorphism
 * - need to change the jackson engine.
 * TODO: accept? typed json is sometimes hard to read
 */
@RestController
@RequestMapping("/kb")
public class KBController {

    private static final Logger logger = LoggerFactory.getLogger(KBController.class);

    @Autowired
    KBApi kbApi;


    @RequestMapping(value = "/catalog", method = {RequestMethod.GET}, produces = "application/json;charset=UTF-8")
    public Map getCatalog(HttpServletRequest request
    ) throws Exception {
        return kbApi.getCatalog();
    }
}
