package org.uengine.meter.record;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import io.krakens.grok.api.Match;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;
import org.uengine.meter.Application;
import org.uengine.meter.billing.BillingRedisRepository;
import org.uengine.meter.billing.UserSubscriptions;
import org.uengine.meter.record.grok.GrokRedisRepository;
import org.uengine.meter.rule.Unit;
import org.uengine.meter.rule.UnitRedisRepository;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Measurement(name = "record")
public class Record {

    @Column(name = "user", tag = true)
    private String user;

    @Column(name = "unit", tag = true)
    private String unit;

    @Column(name = "time")
    private Long time;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "subscriptionId", tag = true)
    private String subscriptionId;

    @Column(name = "basePlan", tag = true)
    private String basePlan;

    @Column(name = "addonPlan", tag = true)
    private String addonPlan;

    @JsonIgnore
    private Unit.Rule rule;

    public Record() {
    }

    public Record(String log) {
        //get patterns from redis.
        final GrokRedisRepository grokRedisRepository = Application.getApplicationContext().getBean(GrokRedisRepository.class);
        final List<String> patterns = grokRedisRepository.findAll();
        for (int i = 0; i < patterns.size(); i++) {
            this.match(patterns.get(i), log);
            //if match, break.
            if (this.time != null && this.user != null && this.amount != null && this.unit != null) {
                break;
            }
        }
    }

    public boolean valid() {
        if (this.time == null || this.user == null || this.amount == null || this.unit == null) {
            return false;
        }
        return true;
    }

    public Record completeDomain() {
        if (this.time == null) {
            this.time = new Date().getTime();
        }
        if (this.user == null) {
            this.user = "anonymous";
        }
        if (this.amount == null || amount < 1) {
            this.amount = 1L;
        }

        final ApplicationContext applicationContext = Application.getApplicationContext();
        final UnitRedisRepository unitRedisRepository = applicationContext.getBean(UnitRedisRepository.class);
        final BillingRedisRepository billingRedisRepository = applicationContext.getBean(BillingRedisRepository.class);

        //if unit rule null, end
        if (this.unit == null) {
            return this;
        }

        //if unit rule empty, end
        final Unit unitRule = unitRedisRepository.findByName(this.unit);
        if (unitRule == null) {
            return this;
        }
        final List<Unit.Rule> rules = unitRule.getRules();
        if (rules.isEmpty()) {
            return this;
        }

        //should find associated rule.
        Unit.Rule associatedRule = null;
        String associatedSubscriptionId = null;

        if (!"anonymous".equals(this.user)) {

            final List<UserSubscriptions.Subscription> subscriptions =
                    billingRedisRepository.findByUserName(this.user).getSubscriptions();

            for (Unit.Rule rule : rules) {
                //only associated plan is target.
                if (!rule.isApplyPlan()) {
                    continue;
                }
                final String basePlan = rule.getBasePlan();
                final String addonPlan = rule.getAddonPlan();

                boolean hasAllPlan = false;

                //if rule has only basePlan
                if (!StringUtils.isEmpty(basePlan) && StringUtils.isEmpty(addonPlan)) {

                    final UserSubscriptions.Subscription subscription =
                            this.findMatchSubscription("BASE", basePlan, subscriptions);
                    if (subscription != null) {
                        hasAllPlan = true;
                        associatedSubscriptionId = subscription.getId();
                    }
                }

                //if rule has both basePlan and addonPlan
                if (!StringUtils.isEmpty(basePlan) && !StringUtils.isEmpty(addonPlan)) {
                    final UserSubscriptions.Subscription baseSubscription =
                            this.findMatchSubscription("BASE", basePlan, subscriptions);
                    final UserSubscriptions.Subscription addonSubscription =
                            this.findMatchSubscription("ADD_ON", addonPlan, subscriptions);

                    if (baseSubscription != null && addonSubscription != null) {
                        hasAllPlan = true;
                        associatedSubscriptionId = addonSubscription.getId();
                    }
                }

                //if hasAllPlan, that is associated rule.
                if (hasAllPlan) {
                    associatedRule = rule;
                    break;
                }
            }
        }

        //if associatedRule not found, set default rule.
        if (associatedRule == null) {
            for (Unit.Rule rule : rules) {
                if (!rule.isApplyPlan()) {
                    associatedRule = rule;
                    break;
                }
            }
        }

        //finally, mapping values
        if (associatedRule != null) {
            this.subscriptionId = associatedSubscriptionId;
            this.basePlan = associatedRule.getBasePlan();
            this.addonPlan = associatedRule.getAddonPlan();
            this.rule = associatedRule;
        }

        return this;
    }

    //subscription
    private UserSubscriptions.Subscription findMatchSubscription(String category, String plan, List<UserSubscriptions.Subscription> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            return null;
        }
        UserSubscriptions.Subscription match = null;
        for (UserSubscriptions.Subscription subscription : subscriptions) {
            if (category.equals(subscription.getCategory()) && plan.equals(subscription.getPlan())) {
                match = subscription;
            }
            break;
        }
        return match;
    }

    public Record(String pattern, String log) {
        this.match(pattern, log);
    }

    private void match(String pattern, String log) {
        final Map<String, Object> capture = this.capture(pattern, log);

        //sample
        //%{TIMESTAMP_ISO8601:timestamp} unit='%{WORD:unit}'

        //2018-09-24T18:57:50.050994 unit='disk'
        //2018-10-03 17:55:26.421 unit='disk'

        //unit, user, timestamp required.
        //if amount is null, amount is 1 and 'amount-auto-assignment' : true

        //TODO
        //timestamp format list add to model
        //unit conversion list add to model
        try {
            if (capture.containsKey("timestamp")) {
                String timestamp = capture.get("timestamp").toString();
                final long time = DateUtils.parseDate(timestamp,
                        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                        "yyyy-MM-dd'T'HH:mm:ss.SSS",
                        "yyyy-MM-dd'T'HH:mm:ss",
                        "dd.MM.yyyy HH:mm:ss",
                        "dd.MM.yyyy").getTime();
                this.time = time;
            }
        } catch (ParseException ex) {
            //if timestamp not exist, current time set.
            this.time = new Date().getTime();
        }

        if (capture.containsKey("user")) {
            this.user = capture.get("user").toString();
        }
        if (capture.containsKey("unit")) {
            this.unit = capture.get("unit").toString();
        }
        if (capture.containsKey("amount")) {
            this.amount = (Long) capture.get("amount");
        }
        this.completeDomain();
    }

    public static Map<String, Object> capture(String pattern, String log) {
        GrokCompiler grokCompiler = GrokCompiler.newInstance();
        grokCompiler.registerDefaultPatterns();

        final Grok grok = grokCompiler.compile(pattern);
        Match gm = grok.match(log);

        /* Get the map with matches */
        return gm.capture();
    }
}
