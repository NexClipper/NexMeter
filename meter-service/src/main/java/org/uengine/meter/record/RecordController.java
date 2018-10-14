package org.uengine.meter.record;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.uengine.meter.billing.BillingService;
import org.uengine.meter.record.kafka.RecordMessage;
import org.uengine.meter.record.kafka.RecordProcessor;
import org.uengine.meter.rule.Unit;
import org.uengine.meter.rule.UnitRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/meter/record")
public class RecordController {

    @Autowired
    private RecordProcessor recordProcessor;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private RecordService recordService;

    @Autowired
    private BillingService billingService;

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
    //     - rule: rule
    //     - subscriptionId:
    //     - series: [
    //          [time, amount]
    //       ]
    //     - amount-per-hour: [
    //          {
    //              time:
    //          }
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


    //TODO
    //UI. ok
    //paypal express checkout test. ok
    //billing hosted page
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
                         @RequestParam(value = "user", required = false) String user,
                         @RequestParam(value = "start") @DateTimeFormat(pattern = "yyyy-MM-dd HH") Date start,
                         @RequestParam(value = "end") @DateTimeFormat(pattern = "yyyy-MM-dd HH") Date end,
                         @RequestParam(value = "division", defaultValue = "1h") String division
    ) throws Exception {

        ArrayList<Object> list = new ArrayList<>();
        if (StringUtils.isEmpty(unit)) {
            final Iterable<Unit> units = unitRepository.findAll();
            units.forEach(item -> {
                final String perUnitName = item.getName();
                if (StringUtils.isEmpty(user)) {
                    try {
                        final UsageSeries usageSeries = recordService.getDashboardSeries(perUnitName, start, end, division);
                        list.add(usageSeries);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    final UsageUserSeries series = recordService.getUserSeries(perUnitName, user, start, end, division);
                    list.add(series);
                }
            });
        } else {
            if (StringUtils.isEmpty(user)) {
                list.add(recordService.getDashboardSeries(unit, start, end, division));
            } else {
                list.add(recordService.getUserSeries(unit, user, start, end, division));
            }
        }
        return list;
    }

    /**
     * Killbill 에서 요청하는 usageItems 포맷으로 사용량 집계를 돌려준다.
     *
     * @param request
     * @param response
     * @param accountId
     * @param start
     * @param end
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/items", produces = "application/json")
    public Object usageItems(HttpServletRequest request,
                             HttpServletResponse response,
                             @RequestParam(value = "accountId") String accountId,
                             @RequestParam(value = "start") @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                             @RequestParam(value = "end") @DateTimeFormat(pattern = "yyyy-MM-dd") Date end,
                             @RequestParam(value = "division", defaultValue = "1h") String division
    ) throws Exception {

        final String user = billingService.getUserNameFromKBAccountId(accountId);
        ArrayList<Map> list = new ArrayList<>();
        if (user != null) {
            final Iterable<Unit> units = unitRepository.findAll();
            units.forEach(item -> {
                final String perUnitName = item.getName();
                final UsageUserSeries series = recordService.getUserSeries(perUnitName, user, start, end, division);
                final List<Map> kbUsageItems = series.applyKBUsageItems(user);
                list.addAll(kbUsageItems);
            });
        }
        return list;
    }
}
