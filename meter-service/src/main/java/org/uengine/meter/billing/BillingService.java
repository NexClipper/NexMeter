package org.uengine.meter.billing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uengine.meter.billing.kb.KBApi;
import org.uengine.meter.billing.kb.KBConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BillingService {

    @Autowired
    private KBApi kbApi;

    @Autowired
    private KBConfig kbConfig;

    /**
     * 사용자 아이디로 현재 정상 이용중인 구독 리스트를 반환한다.
     *
     * @param userName
     * @return
     */
    public UserSubscriptions getUserSubscriptions(String userName, boolean isActive) {
        UserSubscriptions userSubscriptions = new UserSubscriptions();
        Map account = kbApi.getAccountByExternalKey(userName);

        //if account exist? get subscriptions by accountId
        if (account != null) {
            String accountId = account.get("accountId").toString();
            userSubscriptions.setAccountId(accountId);

            List<UserSubscriptions.Subscription> list = this.getAccountSubscriptions(accountId, isActive);
            userSubscriptions.setSubscriptions(list);
        }
        //if account not exist? save empty cache
        else {
            userSubscriptions = new UserSubscriptions();
        }
        return userSubscriptions;
    }

    /**
     * 빌링 어카운트 아이디로 현재 정상 이용중인 구독 리스트를 반환한다.
     *
     * @param accountId
     * @return
     */
    private List<UserSubscriptions.Subscription> getAccountSubscriptions(String accountId, boolean isActive) {
        List<Map> bundles = kbApi.getAccountBundles(accountId);
        if (bundles == null) {
            return new ArrayList<>();
        } else {
            ArrayList<UserSubscriptions.Subscription> list = new ArrayList<>();
            for (int i = 0; i < bundles.size(); i++) {
                Map bundle = bundles.get(i);
                List<Map> subscriptions = (List<Map>) bundle.get("subscriptions");
                for (Map subscription : subscriptions) {
                    UserSubscriptions.Subscription record = new UserSubscriptions.Subscription();
                    record.setId(subscription.get("subscriptionId").toString());
                    record.setPlan(subscription.get("planName").toString());
                    record.setProduct(subscription.get("productName").toString());
                    record.setCategory(subscription.get("productCategory").toString());

                    if (isActive) {
                        if ("ACTIVE".equals(subscription.get("state"))) {
                            list.add(record);
                        }
                    } else {
                        list.add(record);
                    }
                }
            }
            return list;
        }
    }

    public String getUserNameFromKBAccountId(String accountId) {
        Map account = kbApi.getAccountById(accountId);
        if (account == null) {
            return null;
        }
        return account.get("externalKey").toString();
    }
}
