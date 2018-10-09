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

package org.uengine.meter.record;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.uengine.meter.billing.BillingService;
import org.uengine.meter.billing.UserSubscriptions;
import org.uengine.meter.billing.kb.KBApi;
import org.uengine.meter.limit.LimitHistory;
import org.uengine.meter.limit.Limiter;
import org.uengine.meter.limit.kafka.LimitProcessor;
import org.uengine.meter.rule.Unit;
import org.uengine.meter.rule.UnitRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Service
public class RecordService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BillingService billingService;

    @Autowired
    private RecordInfluxRepository recordInfluxRepository;

    @Autowired
    private UnitRepository unitRepository;

    public Object getSeries(String unit, String user, Date start, Date end, String division) {
        final Unit unitRule = unitRepository.findByName(unit);

        //get all subscriptions
        final UserSubscriptions userSubscriptions = billingService.getUserSubscriptions(user, false);
        final List<UserSubscriptions.Subscription> subscriptions = userSubscriptions.getSubscriptions();

        final Map<String, Unit.Rule> ruleMapPerSubscription = unitRule.findRuleMapPerSubscription(subscriptions);

        if (ruleMapPerSubscription.isEmpty()) {
            return null;
        }


        for (Map.Entry<String, Unit.Rule> entry : ruleMapPerSubscription.entrySet()) {
            String subscriptionId = entry.getKey();
            final Unit.Rule rule = entry.getValue();

            //ruleMapPerSubscription 에서 default key 로 온 것은 디폴트 룰이다.
            //따라서 subscriptionId 를 빈 값으로 쿼리해야 한다.
            if ("default".equals(subscriptionId)) {
                subscriptionId = "";
            }

            //미결제 인원을 뜻하는 anonymous 는 구독 상태가 아닐 때 사용이력을 조회하므로,
            // subscriptionId 를 빈 값으로 쿼리해야 한다.
            if ("anonymous".equals(user)) {
                subscriptionId = "";
            }

            //사용자가 정의되지 않을 때는 모든 사용자에 대한 default rule 로 검색하는 뜻이므로,
            //subscriptionId 과 user 모두 쿼리를 하지 말아야 한다.
            if (user == null) {
                subscriptionId = null;
            }

            final QueryResult.Result result = recordInfluxRepository.findByUnitAndUserAndSubscriptionId(
                    rule.getCountingMethod(),
                    unit,
                    user,
                    subscriptionId,
                    start,
                    end,
                    division
            );
        }
        return null;
    }
}
