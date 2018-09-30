package org.uengine.meter.record;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uengine.meter.rule.UnitInternalService;
import org.uengine.meter.rule.UnitRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/record")
public class RecordController {

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private UnitInternalService unitInternalService;

    private final Log logger = LogFactory.getLog(getClass());


    @GetMapping(value = "test", produces = "application/json")
    public Object handle(HttpServletRequest request,
                         HttpServletResponse response
    ) throws Exception {
        //1. insert record.
        //2. send kafka
        //3. receive
        //4. per record, user / unit / time / amount
        //5. select user subscription and unit from redis.
        //6. if unit null, user / unit / time / amount save.

        //7. if unit exist and (user null or user do not have match subscription)
        // -> find default rule which apply-plan false.
        // -> if exist, save recode. (limit save, too)

        //8. if unit exist and user has match subscription
        // -> as rule, as subscription id, save recode. (limit save, too)

        //limit process as follow:
        //limit-refresh-interval: 


        //if user changed plan during subscription cycle, in same unit, other rule cloud applied.
        //user / unit / time / amount / null              / default(true) /
        //user / unit / time / amount / subscriptionId(1) / default(false) / base-plan
        //user / unit / time / amount / subscriptionId(2) / default(false) / base-plan / addon-plan

        //when calculate time,
        //output (unit, user, period)
        // -> select group by subscription id with max time in influxdb
        // -> per record, with base / addon plan, find correct rule.
        // -> per rule,(include default rule) calculate.
        // -> result as follow:
        // {guest: amount(83), subscriptionId(1): amount(13), subscriptionId(2): amount(49) }


        return null;
    }
}
