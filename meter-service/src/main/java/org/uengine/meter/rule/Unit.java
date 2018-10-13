package org.uengine.meter.rule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.uengine.meter.Application;
import org.uengine.meter.billing.BillingRedisRepository;
import org.uengine.meter.billing.UserSubscriptions;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "meter_unit")
@Data
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private CountingMethod countingMethod;

    public static enum CountingMethod {
        AVG, PEAK, SUM
    }

    @JsonIgnore
    @Column(columnDefinition = "TEXT")
    private String rulesStr;

    public void addRule(Rule rule) {
        final List<Rule> rules = this.getRules();
        rules.add(rule);
        this.setRules(rules);
    }

    public void setRules(List<Rule> rules) {
        try {
            this.rulesStr = new ObjectMapper().writeValueAsString(rules);
        } catch (Exception ex) {
            this.rulesStr = "[]";
        }
    }

    public List<Rule> getRules() {
        try {
            final List<Rule> list = new ObjectMapper().readValue(this.getRulesStr(), new TypeReference<List<Rule>>() {
            });
            //override countingMethod
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setCountingMethod(this.countingMethod);
            }
            return list;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    /**
     * 주어진 구독 리스트에 해당하는 rule 을 매핑하여 반환한다.
     *
     * @param subscriptions
     * @return
     */
    public Map<String, Rule> findRuleMapPerSubscription(List<UserSubscriptions.Subscription> subscriptions) {
        Map<String, Unit.Rule> ruleMap = new HashMap<>();

        //if rule empty, return
        final List<Rule> rules = this.getRules();
        if (rules == null || rules.isEmpty()) {
            return ruleMap;
        }

        //set defaultRule
        final Rule defaultRule = this.findDefaultRule();
        if (defaultRule != null) {
            ruleMap.put("default", defaultRule);
        }

        for (Unit.Rule rule : rules) {
            //only associated plan is target.
            if (!rule.isApplyPlan()) {
                //default rule
                continue;
            }
            final String basePlan = rule.getBasePlan();
            final String addonPlan = rule.getAddonPlan();
            boolean hasAllPlan = false;

            //if rule has only basePlan
            if (!StringUtils.isEmpty(basePlan) && StringUtils.isEmpty(addonPlan)) {

                final UserSubscriptions.Subscription subscription =
                        findMatchSubscription("BASE", basePlan, subscriptions);
                if (subscription != null) {
                    ruleMap.put(subscription.getId(), rule);
                }
            }

            //if rule has both basePlan and addonPlan
            if (!StringUtils.isEmpty(basePlan) && !StringUtils.isEmpty(addonPlan)) {
                final UserSubscriptions.Subscription baseSubscription =
                        findMatchSubscription("BASE", basePlan, subscriptions);
                final UserSubscriptions.Subscription addonSubscription =
                        findMatchSubscription("ADD_ON", addonPlan, subscriptions);

                if (baseSubscription != null && addonSubscription != null) {
                    ruleMap.put(addonSubscription.getId(), rule);
                }
            }
        }

        return ruleMap;
    }

    //subscription
    public static UserSubscriptions.Subscription findMatchSubscription(String category, String plan, List<UserSubscriptions.Subscription> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            return null;
        }
        UserSubscriptions.Subscription match = null;
        for (UserSubscriptions.Subscription subscription : subscriptions) {
            if (category.equals(subscription.getCategory()) && plan.equals(subscription.getPlan())) {
                match = subscription;
                break;
            }
        }
        return match;
    }

    public Rule findDefaultRule() {
        Rule defaultRule = null;
        final List<Rule> rules = this.getRules();
        if (rules != null && !rules.isEmpty()) {
            for (Rule rule : rules) {

                //Because applyPlan false means default metering rule, it should only one.
                if (!rule.isApplyPlan()) {
                    defaultRule = rule;
                }
            }
        }
        return defaultRule;
    }

    @PostUpdate
    @PostPersist
    public void updateCache() {
        final UnitRedisRepository internalService = Application.getApplicationContext().getBean(UnitRedisRepository.class);
        internalService.save(this);
    }

    @PostRemove
    public void deleteCache() {
        final UnitRedisRepository internalService = Application.getApplicationContext().getBean(UnitRedisRepository.class);
        internalService.deleteByName(this.getName());
    }


    //update 시에 안먹힘.
    @PreUpdate
    @PrePersist
    public void validate() {
        final List<Rule> rules = this.getRules();
        if (rules != null && !rules.isEmpty()) {
            int defaultRuleCount = 0;
            for (Rule rule : rules) {

                //Because applyPlan false means default metering rule, it should only one.
                if (!rule.isApplyPlan()) {
                    defaultRuleCount++;
                }
                Assert.isTrue(defaultRuleCount <= 1, "applyPlan: Because applyPlan false means default metering rule, it should only one.");

                //validate rule
                this.validateRule(rule);
            }
        }
    }

    private boolean validateRule(Rule rule) {
        Assert.notNull(rule.getCountingMethod(), "countingMethod: mustn't be null");

        //When applyPlan true, basePlan required
        if (rule.isApplyPlan()) {
            Assert.notNull(rule.getBasePlan(), "basePlan: mustn't be null, cause it applied to plan");
        }

        //FreePeriod need FreeAmount both
        Assert.isTrue(!(rule.getFreeAmount() == null && rule.getFreePeriod() != null), "freeAmount: FreePeriod need FreeAmount");
        Assert.isTrue(!(rule.getFreePeriod() == null && rule.getFreeAmount() != null), "freePeriod: FreeAmount need FreePeriod");

        //Case countingMethod avg or peak
        if (CountingMethod.AVG.equals(rule.getCountingMethod()) ||
                CountingMethod.PEAK.equals(rule.getCountingMethod())) {

            //case1: periodSplitting mustn't be null
            Assert.notNull(rule.getPeriodSplitting(), "periodSplitting: mustn't be null");

            //case2: If FreePeriod exist, FreePeriod should equals PeriodSplitting
            final boolean fault_case1 = rule.getFreePeriod() != null &&
                    !rule.getFreePeriod().toString().equals(rule.getPeriodSplitting().toString());
            Assert.isTrue(!fault_case1, "freePeriod: If FreePeriod exist, FreePeriod should equals PeriodSplitting");

            //case3: When countingMethod is AVG or PEAK, limitRefreshInterval should be null. An over-limit warning is issued immediately.
            Assert.isNull(rule.getLimitRefreshInterval(), "limitRefreshInterval: When countingMethod is AVG or PEAK, limitRefreshInterval should be null. An over-limit warning is issued immediately.");
        }

        //Case countingMethod sum
        if (CountingMethod.SUM.equals(rule.getCountingMethod())) {
            //case1: periodSplitting must be null
            Assert.isTrue(rule.getPeriodSplitting() == null, "periodSplitting: must be null");

            //case2: if limitAmount exist, LimitRefreshInterval mustn't be null
            Assert.isTrue(!(rule.getLimitAmount() != null && rule.getLimitRefreshInterval() == null), "limitRefreshInterval: If limitAmount exist, limitRefreshInterval mustn't be null");
        }

        return true;
    }

    @Data
    @NoArgsConstructor
    public static class Rule {
        private boolean applyPlan;
        private String basePlan;
        private String addonPlan;
        private CountingMethod countingMethod;
        private PeriodSplitting periodSplitting;
        private Long limitAmount;
        private LimitRefreshInterval limitRefreshInterval;
        private Long freeAmount;
        private FreePeriod freePeriod;
        private boolean putEmptyPeriod;

        public static enum PeriodSplitting {
            HOUR, DAY
        }

        public static enum LimitRefreshInterval {
            HOUR, DAY
        }

        public static enum FreePeriod {
            HOUR, DAY
        }
    }
}
