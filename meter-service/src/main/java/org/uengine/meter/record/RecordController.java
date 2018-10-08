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
    // - per unit, y: amount x: time lines: per subscription(plan)

    //when calculate time,
    //input1. unit, period, user
    //input2. period, user (all unit, for billing)

    //output for input1.
    //user:
    //  - unit: disk
    //    usages:
    //     - subscriptionId:
    //     - data: [
    //          {time: ,amount: }
    //       ]

    //output for calculate.
    //user:
    //  - unit: disk
    //    usages:
    //     - subscriptionId:
    //       base-plan:
    //       addon-plan:
    //       result: 231


    //output for billing.
    //[{subscriptionId, userId, tenantId, amount, time}]
    //-> if dryRun? pass
    //-> if real? limit:disk1:4e84c3f0-0fe1-4bd1-960e-c5ecd235edc5
    //per unit, with base / addon plan, find correct rule.
    //if rule limitRefreshInterval is SUBSCRIPTION_CYCLE, refresh limit.


    //select metering data.
    // -> group by units, subscriptionId, time series per hour where user = ''
    // -> group by units, subscriptionId, time series for all where user = ''


    // -> select group by subscription id with max time in influxdb
    // -> per record, with base / addon plan, find correct rule.
    // -> per rule,(include default rule) calculate.
    // -> result as follow:
    // {guest: amount(83), subscriptionId(1): amount(13), subscriptionId(2): amount(49) }


    //인플럭스 db 데이터 조회 법.
    //tag list 를 마련하지 말고, where = ? 로 모두 해결. 그러기 위해서는, 고객 별 subscription 리스트,unit 리스트가 필요.
    //그걸로 동시 쿼리를 진행시켜 stream 으로 묶어야 한다.


    //UI

    //billing hosted page

    //paypal express checkout test

    //user sync

    //docker

    @PostMapping(value = "/json", produces = "application/json")
    public Object saveJsonRecord(@RequestBody String records
    ) throws Exception {

        recordProcessor.sendRecordMessage(new RecordMessage(RecordMessage.RecordMessageType.JSON, records));
        return null;
    }

    @PostMapping(value = "/log", produces = "application/json")
    public Object saveLogRecord(@RequestBody String records
    ) throws Exception {
        recordProcessor.sendRecordMessage(new RecordMessage(RecordMessage.RecordMessageType.LOG, records));
        return null;
    }

    /**
     * 미터링 정보를 time series 로 리턴하고, 기간동안 metering summary 를 포함한다.
     *
     * @param request
     * @param response
     * @param unit
     * @param user
     * @param start
     * @param end
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/series", produces = "application/json")
    public Object series(HttpServletRequest request,
                         HttpServletResponse response,
                         @RequestParam(value = "unit", required = false) String unit,
                         @RequestParam(value = "user") String user,
                         @RequestParam(value = "start") String start,
                         @RequestParam(value = "end") String end,
                         @RequestParam(value = "division", defaultValue = "1h") String division
    ) throws Exception {


        //사용자의 subscription id 리스트를 구함.
        //subscription id + unit 으로 각각 쿼리함.

        //시간 시리즈로 정렬.
        //avg 일 경우
        //무료 시간이 시간당이면 바로 제함.

        //무료 시간이 일일이면
        //일일 시리즈로 평균 재정렬
        //일일 시리즈에서 제함.

        //sum 일 경우
        //시간 시리즈로 정렬.
        //무료 시간이 시간당이면 바로 제함.

        //무료 시간이 일일이면
        //일일 시리즈로 합산 재정렬
        //일일 시리즈에서 제함.

        //callable 로 여러 시리즈를 동시 쿼리하도록 함.
        return null;
    }

    /**
     * Killbill 에서 요청하는 usageItems 포맷으로 사용량 집계를 돌려준다.
     *
     * @param request
     * @param response
     * @param user
     * @param start
     * @param end
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/usageItems", produces = "application/json")
    public Object usageItems(HttpServletRequest request,
                             HttpServletResponse response,
                             @RequestParam(value = "user") String user,
                             @RequestParam(value = "start") String start,
                             @RequestParam(value = "end") String end
    ) throws Exception {

        return null;
    }
}
