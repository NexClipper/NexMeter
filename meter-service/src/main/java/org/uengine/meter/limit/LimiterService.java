/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uengine.meter.limit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.uengine.meter.limit.kafka.LimitProcessor;
import org.uengine.meter.record.Record;
import org.uengine.meter.rule.Unit;

import java.util.Date;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Service
public class LimiterService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private LimitProcessor limitProcessor;

    @Autowired
    private ObjectMapper objectMapper;

    //for just check limit. -> consume as usage 0

    public Limiter getLimiter(String user, String unit) {
        final Record record = new Record();
        record.setUser(user);
        record.setUnit(unit);
        record.completeDomain();
        record.setAmount(0L);
        return this.consume(record, true);
    }

    public Limiter updateCurrentLimitAmount(String user, String unit, Long amount) {
        final Record record = new Record();
        record.setUser(user);
        record.setUnit(unit);
        record.completeDomain();
        if (record.getRule() != null) {
            final String count_key = this.getKey(record.getUnit(), record.getUser());
            final Unit.CountingMethod countingMethod = record.getRule().getCountingMethod();
            if (Unit.CountingMethod.SUM.equals(countingMethod)) {
                //reset current amount
                this.redisTemplate.boundValueOps(count_key).set(amount);
            }
        }

        return this.getLimiter(user, unit);
    }


    public void saveHistory(Record record, Long current, Long limitAmount, Long interval) {
        final LimitHistory limitHistory = new LimitHistory();
        limitHistory.setUser(record.getUser());
        limitHistory.setUnit(record.getUnit());
        limitHistory.setCurrent(current);
        limitHistory.setLimitAmount(limitAmount);
        limitHistory.setRefreshInterval(interval);
        limitHistory.setBasePlan(record.getBasePlan());
        limitHistory.setAddonPlan(record.getAddonPlan());
        limitHistory.setSubscriptionId(record.getSubscriptionId());
        limitHistory.setTime(new Date().getTime());

        try {
            limitProcessor.sendLimitMessage(objectMapper.writeValueAsString(limitHistory));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String getKey(String unit, String user) {
        return "count::" + unit + "::" + user;
    }

    public Limiter consume(Record record, boolean lookup) {
        Limiter limiter = new Limiter();
        limiter.setUnit(record.getUnit());
        limiter.setUser(record.getUser());
        limiter.setRemaining(0L);
        limiter.setReset(0L);

        //user required
        if ("anonymous".equals(record.getUser())) {
            return limiter;
        }

        //unit required
        if (record.getUnit() == null) {
            return limiter;
        }

        //key for limit
        final String count_key = this.getKey(record.getUnit(), record.getUser());

        //rule required
        final Unit.Rule rule = record.getRule();
        if (rule == null) {
            return limiter;
        }

        //limitRefreshInterval
        final Unit.Rule.LimitRefreshInterval limitRefreshInterval = rule.getLimitRefreshInterval();

        //limitAmount
        final Long limitAmount = rule.getLimitAmount();

        //if sum
        if (Unit.CountingMethod.SUM.equals(rule.getCountingMethod())) {
            //limitRefreshInterval, limitAmount required
            if (limitRefreshInterval == null || limitAmount == null) {
                return limiter;
            }

            //usage
            Long usage = record.getAmount();

            //refreshInterval
            Long refreshInterval = null;


            if (Unit.Rule.LimitRefreshInterval.HOUR.equals(limitRefreshInterval)) {
                //3600 sec.
                refreshInterval = 3600L;
            } else if (Unit.Rule.LimitRefreshInterval.DAY.equals(limitRefreshInterval)) {
                //3600 sec * 24hour
                refreshInterval = 24L * 3600L;
            }

            //lookup
            if (lookup) {
                //set usage 0
                usage = 0L;

                calcRemainingLimit(limitAmount, usage, refreshInterval, count_key, rule.getCountingMethod(), limiter);
                return limiter;
            }
            //perform
            else {
                calcRemainingLimit(limitAmount, usage, refreshInterval, count_key, rule.getCountingMethod(), limiter);
                if (limiter.getRemaining() <= 0) {
                    //save limit 이력
                    this.saveHistory(record, limiter.getCurrent(), limitAmount, refreshInterval);
                }
                return limiter;
            }
        }
        //if avg or peak
        else {
            //limitAmount required
            if (limitAmount == null) {
                return limiter;
            }
            //usage
            final Long usage = record.getAmount();

            //lookup
            if (lookup) {
                Long current = 0L;
                final Object o = this.redisTemplate.boundValueOps(count_key).get();
                if (o != null) {
                    current = new Long((Integer) o);
                }
                limiter.setCurrent(current);
                limiter.setRemaining(Math.max(-1, limitAmount - current));
                return limiter;
            }
            //perform
            else {
                calcRemainingLimit(limitAmount, usage, null, count_key, rule.getCountingMethod(), limiter);
                if (limiter.getRemaining() <= 0) {
                    //save limit 이력
                    this.saveHistory(record, limiter.getCurrent(), limitAmount, null);
                }
                return limiter;
            }
        }
    }

    private synchronized void calcRemainingLimit(
            Long limit,
            Long usage,
            Long refreshInterval,
            String count_key,
            Unit.CountingMethod countingMethod,
            Limiter limiter) {
        if (limit != null) {
            //if sum, add recent usage
            if (Unit.CountingMethod.SUM.equals(countingMethod)) {
                handleExpiration(count_key, refreshInterval, limiter);

                Long current = 0L;

                try {
                    current = this.redisTemplate.boundValueOps(count_key).increment(usage);
                } catch (RuntimeException e) {
                    String msg = "Failed retrieving rate for " + count_key + ", will return limit";
                    this.handleError(msg, e);
                }

                //남은 값은 최소 -1
                limiter.setCurrent(current);
                limiter.setRemaining(Math.max(-1, limit - current));
            }
            //if avg or peak, save current usage
            else {
                this.redisTemplate.boundValueOps(count_key).set(usage);
                limiter.setCurrent(usage);
                limiter.setRemaining(Math.max(-1, limit - usage));
            }
        }
    }

    private synchronized void handleExpiration(String key, Long refreshInterval, Limiter limiter) {
        //여기서는 key 에 만료시간을 설정하는 곳.
        //key 에 만료시간 하루 설정일 경우.
        //들어오는 time 이 하루 기간 이내면 limit 에 추가.
        //들어오는 time 이 미래/또는 과거이면 추가하지 않는다.
        Long expire = null;
        try {
            expire = this.redisTemplate.getExpire(key);
            if (expire == null || expire <= -1) {

                this.redisTemplate.expire(key, refreshInterval, SECONDS);
                expire = refreshInterval;
            }
        } catch (RuntimeException e) {
            String msg = "Failed retrieving expiration for " + key + ", will reset now";
            this.handleError(msg, e);
        }

        limiter.setReset(SECONDS.toMillis(expire == null ? 0L : expire));
    }

    private void handleSaveError(String key, Exception e) {
        log.error("Failed saving rate for " + key + ", returning unsaved rate", e);
    }

    private void handleFetchError(String key, Exception e) {
        log.error("Failed retrieving rate for " + key + ", will create new rate", e);
    }

    private void handleError(String msg, Exception e) {
        log.error(msg, e);
    }
}
