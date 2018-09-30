package org.uengine.meter.rule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.http.util.Asserts;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.uengine.meter.Application;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "meter_unit")
@Data
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

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
            return new ObjectMapper().readValue(this.getRulesStr(), new TypeReference<List<Rule>>() {
            });
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    @PostUpdate
    @PostPersist
    public void updateCache() {
        final UnitInternalService internalService = Application.getApplicationContext().getBean(UnitInternalService.class);
        internalService.save(this);
    }

    @PostRemove
    public void deleteCache() {
        final UnitInternalService internalService = Application.getApplicationContext().getBean(UnitInternalService.class);
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
        Assert.notNull(rule.getPeriodSplitting(), "periodSplitting: mustn't be null");

        //When applyPlan true, basePlan required
        if (rule.isApplyPlan()) {
            Assert.notNull(rule.getBasePlan(), "basePlan: mustn't be null, cause it applied to plan");
        }

        //FreePeriod need FreeAmount
        Assert.isTrue(!(rule.getFreeAmount() == null && rule.getFreePeriod() != null), "freeAmount: FreePeriod need FreeAmount");

        //LimitRefreshInterval need LimitAmount
        Assert.isTrue(!(rule.getLimitAmount() == null && rule.getLimitRefreshInterval() != null), "limitAmount: LimitRefreshInterval need LimitAmount");

        //Case countingMethod avg or peak
        if (Rule.CountingMethod.AVG.equals(rule.getCountingMethod()) ||
                Rule.CountingMethod.PEAK.equals(rule.getCountingMethod())) {


            //case1: When PeriodSplitting is Day, FreePeriod should Day or SUBSCRIPTION_CYCLE
            final boolean fault_case1 = Rule.PeriodSplitting.DAY.equals(rule.getPeriodSplitting())
                    && Rule.FreePeriod.HOUR.equals(rule.getFreePeriod());

            Assert.isTrue(!fault_case1, "freePeriod: When PeriodSplitting is Day, FreePeriod should Day or SUBSCRIPTION_CYCLE");

            //case2: When PeriodSplitting is SUBSCRIPTION_CYCLE, FreePeriod should be SUBSCRIPTION_CYCLE
            final boolean fault_case2 = Rule.PeriodSplitting.SUBSCRIPTION_CYCLE.equals(rule.getPeriodSplitting())
                    && !Rule.FreePeriod.SUBSCRIPTION_CYCLE.equals(rule.getFreePeriod());

            Assert.isTrue(!fault_case2, "freePeriod: When PeriodSplitting is SUBSCRIPTION_CYCLE, FreePeriod should be SUBSCRIPTION_CYCLE");

            //case3: When countingMethod is AVG or PEAK, limitRefreshInterval should be null. An over-limit warning is issued immediately.
            Assert.isNull(rule.getLimitRefreshInterval(), "limitRefreshInterval: When countingMethod is AVG or PEAK, limitRefreshInterval should be null. An over-limit warning is issued immediately.");

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

        public static enum CountingMethod {
            AVG, PEAK, SUM
        }

        public static enum PeriodSplitting {
            HOUR, DAY, SUBSCRIPTION_CYCLE
        }

        public static enum LimitRefreshInterval {
            HOUR, DAY, SUBSCRIPTION_CYCLE, MANUALY
        }

        public static enum FreePeriod {
            HOUR, DAY, SUBSCRIPTION_CYCLE
        }
    }
}
