package org.uengine.meter.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uengine.meter.billing.kb.KBApi;
import org.uengine.meter.billing.kb.KBConfig;
import org.uengine.meter.rule.Unit;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BillingRedisRepository {

    private Logger logger = LoggerFactory.getLogger(BillingRedisRepository.class);

    @Autowired
    private BillingService billingService;

    @Cacheable(value = "subscription", key = "#userName")
    public UserSubscriptions findByUserName(String userName) {
        logger.info("find user subscription from killbill : " + userName);
        return billingService.getUserSubscriptions(userName);
    }

    @CachePut(value = "subscription", key = "#userName")
    @Transactional
    public UserSubscriptions updateByUserName(String userName) {
        logger.info("update user subscription cache : " + userName);
        return billingService.getUserSubscriptions(userName);
    }

    @CacheEvict(value = "subscription", key = "#userName")
    @Transactional
    public void deleteByUserName(String userName) {
        logger.info("remove user subscription cache :" + userName);
    }

}
