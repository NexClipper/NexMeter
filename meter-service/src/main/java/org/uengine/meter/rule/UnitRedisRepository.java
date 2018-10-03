package org.uengine.meter.rule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uengine.meter.billing.BillingRedisRepository;
import org.uengine.meter.billing.UserSubscriptions;
import org.uengine.meter.billing.kb.KBApi;
import org.uengine.meter.billing.kb.KBConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UnitRedisRepository {

    private final Log logger = LogFactory.getLog(getClass());

    @Autowired
    private UnitRepository unitRepository;

    @Cacheable(value = "unit", key = "#name")
    public Unit findByName(String name) {
        logger.info("find unit from jdbc : " + name);
        return unitRepository.findByName(name);
    }

    @CachePut(value = "unit", key = "#unit.name")
    @Transactional
    public Unit save(Unit unit) {
        logger.info("update unit cache : " + unit.getName());
        return unit;
    }

    @CacheEvict(value = "unit", key = "#name")
    @Transactional
    public void deleteByName(String name) {
        logger.info("remove unit cache :" + name);
    }

}
