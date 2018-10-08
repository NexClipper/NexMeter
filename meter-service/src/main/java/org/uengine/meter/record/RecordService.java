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

    public void getSeries(String unit, String user, String division) {
        final Unit unitRule = unitRepository.findByName(unit);
        for (Unit.Rule rule : unitRule.getRules()) {

        }

        //get all subscriptions
        final UserSubscriptions userSubscriptions = billingService.getUserSubscriptions(user, false);
        final List<UserSubscriptions.Subscription> subscriptions = userSubscriptions.getSubscriptions();
        final ArrayList<String> subscriptionIds = new ArrayList<>();
        if (subscriptions != null) {
            for (int i = 0; i < subscriptions.size(); i++) {
                subscriptionIds.add(subscriptions.get(i).getId());

            }
        }
        //unit 에 subscriptionId 를 투입했을 경우, unit 중 적용된 rule 을 알아야 한다.
        //unit 에 적용된 rule 은

        //findByUnitAndUserAndSubscriptionId

        //subscriptionIds 가 없는것, 즉 디폴트 룰 카운팅 메소드로 하나.

        //subscriptionIds 마다 base,addonPlan 으로, 해당 룰을 찾기.

        //해당 룰을 바탕으로 카운팅 메소드와 함께 쿼리.

        //전체 대쉬보드에서는...?

        //쿼리하기 전에, 카운팅 메소드를 알아야 함.
        //unit 을 알아내야 함.

        //final Map account = kbApi.getAccountByExternalKey(user);
    }
}
