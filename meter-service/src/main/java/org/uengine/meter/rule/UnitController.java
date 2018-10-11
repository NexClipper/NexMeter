package org.uengine.meter.rule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/meter/unit")
public class UnitController {

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private UnitRedisRepository unitInternalService;

    private final Log logger = LogFactory.getLog(getClass());


    @GetMapping(value = "test", produces = "application/json")
    public Object handle(HttpServletRequest request,
                         HttpServletResponse response
    ) throws Exception {
        return unitInternalService.findByName("case");
        //return unitRepository.findAll();
    }

}
