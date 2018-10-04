package org.uengine.meter.record;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.uengine.meter.record.kafka.RecordMessage;
import org.uengine.meter.record.kafka.RecordProcessor;
import org.uengine.meter.rule.UnitRedisRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/record")
public class RecordController {

    @Autowired
    private RecordProcessor recordProcessor;

    @Autowired
    private UnitRedisRepository unitInternalService;

    private final Log logger = LogFactory.getLog(getClass());

    //1. insert record.
    //2. send kafka
    //3. receive
    //4. per record, user / unit / time / amount
    //5. select user subscription and unit from redis.
    //6. if unit rule null, user / unit / time / amount save.

    //7. if unit rule exist and (user null or user do not have match subscription)
    // -> find default rule which apply-plan false.
    // -> if exist, save recode. (limit save, only user exist)

    //8. if unit rule exist and user has match subscription
    // -> as rule, as subscription id, save recode. (limit save, only user exist)


    //limit process as follow:
    //limit-refresh-interval:


    //if user changed plan during subscription cycle, in same unit, other rule should applied.
    //user / unit / time / amount / null              / default(true) /
    //user / unit / time / amount / subscriptionId(1) / default(false) / base-plan
    //user / unit / time / amount / subscriptionId(2) / default(false) / base-plan / addon-plan

    //dashboard:
    //user:
    // - per unit, y: amount x: time lines: null

    //when calculate time,
    //output (unit, user, period)
    // -> select group by subscription id with max time in influxdb
    // -> per record, with base / addon plan, find correct rule.
    // -> per rule,(include default rule) calculate.
    // -> result as follow:
    // {guest: amount(83), subscriptionId(1): amount(13), subscriptionId(2): amount(49) }



    //UI

    //billing hosted page

    //paypal express checkout test

    //user sync

    //docker

    @PostMapping(value = "/json", produces = "application/json")
    public Object saveJsonRecord(HttpServletRequest request,
                                 HttpServletResponse response,
                                 @RequestBody String records
    ) throws Exception {

        recordProcessor.sendRecordMessage(new RecordMessage(RecordMessage.RecordMessageType.JSON, records));

        return null;
    }

    @PostMapping(value = "/log", produces = "application/json")
    public Object saveLogRecord(HttpServletRequest request,
                                HttpServletResponse response,
                                @RequestBody String records
    ) throws Exception {

        recordProcessor.sendRecordMessage(new RecordMessage(RecordMessage.RecordMessageType.LOG, records));
        return null;
    }
}
