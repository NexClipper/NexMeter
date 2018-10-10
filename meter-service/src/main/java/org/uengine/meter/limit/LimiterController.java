package org.uengine.meter.limit;

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
@RequestMapping("/meter/limit")
public class LimiterController {

    @Autowired
    private LimiterService limiterService;

    private final Log logger = LogFactory.getLog(getClass());

    /**
     * 현재 사용량 제한 상태를 확인한다.
     * @param unit
     * @param user
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/check/{unit}", produces = "application/json")
    public Limiter saveJsonRecord(@PathVariable("unit") String unit,
                                  @RequestParam("user") String user
    ) throws Exception {
        return limiterService.getLimiter(user, unit);
    }

    /**
     * 현재 사용량 제한을 임의의 수량으로 변경한다.
     * @param unit
     * @param user
     * @param amount
     * @return
     * @throws Exception
     */
    @PutMapping(value = "/reset/{unit}", produces = "application/json")
    public Limiter updateCurrentLimitAmount(@PathVariable("unit") String unit,
                                            @RequestParam("user") String user,
                                            @RequestParam("amount") Long amount
    ) throws Exception {
        return limiterService.updateCurrentLimitAmount(user, unit, amount);
    }
    //clearLimiter
}
