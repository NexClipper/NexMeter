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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.uengine.meter.record.Record;
import org.uengine.meter.rule.Unit;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Service
public class LimiterService {

    @Autowired
    private RedisTemplate redisTemplate;

    public Limiter consume(Record record) {

        //user required
        if ("anonymous".equals(record.getUser())) {
            return null;
        }

        //key for limit
        final String key = "limit:" + record.getUnit() + ":" + record.getUser();

        //rule required
        final Unit.Rule rule = record.getRule();
        if (rule == null) {
            return null;
        }
        final Unit.Rule.LimitRefreshInterval limitRefreshInterval = rule.getLimitRefreshInterval();

        //limitAmount
        final Long limitAmount = rule.getLimitAmount();

        //limitRefreshInterval, limitAmount required
        if (limitRefreshInterval == null || limitAmount == null) {
            return null;
        }

        //usage
        final Long usage = record.getAmount();

        //refreshInterval
        Long refreshInterval = null;
        if (Unit.Rule.LimitRefreshInterval.HOUR.equals(limitRefreshInterval)) {
            //3600 sec.
            refreshInterval = 3600L;
        } else if (Unit.Rule.LimitRefreshInterval.DAY.equals(limitRefreshInterval)) {
            //3600 sec * 24hour
            refreshInterval = 24L * 3600L;
        } else if (Unit.Rule.LimitRefreshInterval.SUBSCRIPTION_CYCLE.equals(limitRefreshInterval)
                || Unit.Rule.LimitRefreshInterval.MANUALY.equals(limitRefreshInterval)) {
            //3600 sec * 24hour * unlimited(Max 360 Day)
            //SUBSCRIPTION_CYCLE = refresh when invoice created.
            //MANUALY = refresh when api is executed.
            refreshInterval = 24L * 3600L * 360L;
        }

        final Limiter limiter = new Limiter(key, null, null, null);

        calcRemainingLimit(limitAmount, usage, refreshInterval, key, limiter);

        return limiter;
    }

    private synchronized void calcRemainingLimit(
            Long limit,
            Long usage,
            Long refreshInterval,
            String key,
            Limiter limiter) {
        if (limit != null) {
            handleExpiration(key, refreshInterval, limiter);

            Long current = 0L;

            try {
                current = this.redisTemplate.boundValueOps(key).increment(usage);
            } catch (RuntimeException e) {
                String msg = "Failed retrieving rate for " + key + ", will return limit";
                this.handleError(msg, e);
            }

            //남은 값은 최소 -1
            limiter.setRemaining(Math.max(-1, limit - current));
        }
    }

    private synchronized void removeExpiration(String key) {
        this.redisTemplate.expire(key, 0, SECONDS);
    }

    private synchronized void handleExpiration(String key, Long refreshInterval, Limiter limiter) {
        //여기서는 key 에 만료시간을 설정하는 곳.
        //key 에 만료시간 하루 설정일 경우.
        //들어오는 time 이 하루 기간 이내면 limit 에 추가.
        //들어오는 time 이 미래/또는 과거이면 추가하지 않는다.
        Long expire = null;
        try {
            expire = this.redisTemplate.getExpire(key);
            if (expire == null || expire == -1) {

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
