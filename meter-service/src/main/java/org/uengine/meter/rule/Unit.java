package org.uengine.meter.rule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;
import org.uengine.meter.Application;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

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
        if (Rule.CountingMethod.AVG.equals(rule.getCountingMethod()) ||
                Rule.CountingMethod.PEAK.equals(rule.getCountingMethod())) {

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
        if (Rule.CountingMethod.SUM.equals(rule.getCountingMethod())) {
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

        public static enum CountingMethod {
            AVG, PEAK, SUM
        }

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
