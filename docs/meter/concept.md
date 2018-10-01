# Concept

**Many subscription-based billing platforms support usage-based billing.
  However, companies still need to be able to measure usage and inform the platform of the exact amount.**

전통적인 사용량 집계 방식으로, 매일 또는 매월 정산 시간에 배치 프로세스를 실행하여 고객의 사용량을 조회하였습니다.

현대의 마이크로 서비스 아키텍처에서, 대부분의 고객의 Level 에 대한 서비스 제공 필터링은 API 게이트웨이에서 발생하고, 
때로는 마이크로 서비스 스스로가 사용자 과금 상태를 문의하고 서비스 제공 여부를 결정해야합니다.
 
컴퓨팅 사용량, API 요청 수 등에서 수익을 발행시키는 서비스 모델에서는 (또는 기타 실시간 응답성을 필요로 하는 과금 모델),  
서비스 제공 전/제공 후 사용자의 과금 상황이나 사용량 상황을 매번 데이터 센터에 쿼리해야 하는데, 전체 시스템의 성능 저하를 유발하게 됩니다. 

이를 해결하기 위해 모든 서비스 마다 캐쉬를 구현하게 될 경우 개발 퍼포먼스의 저하와 시스템 복잡도 증가를 유발하게 됩니다.

**따라서, 기존 어플리케이션의 소스 코드에 영향을 주지 않고 사용량을 수집, 또는 사용량 정보를 제공할 수 있는 독립적인 서비스가 필요하며,
 간단한 카프카 연동 또는 에이전트 설치로 사용이 가능해야 합니다.**

**무엇보다, 어플리케이션에서 사용자 과금/사용량 조회 과정에서
 기존 퍼포먼스를 저하시키지 않게, 들어오는 사용량 스트림을 실시간으로 처리하여 수 ms 이내에 응답을 할 수 있게 하는 것이 목표입니다.**
 

## Use Case

Let's take a company as an example. 
The company specializes in cloud monitoring and provides AI-based anomaly detection api as a business strategy item.

The company considered the following revenue model:

![](image/table1.png)


### Catalog

In this section, the units receiving money are host and ai-analytics, and are divided into standard and pro plans.

Therefore, first set up a plan that allows the billing platform(killbill) to receive these charges:

`Killbill Catalog`

```
<catalog xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:noNamespaceSchemaLocation="CatalogSchema.xsd ">

    <effectiveDate>2013-02-08T00:00:00+00:00</effectiveDate>
    <catalogName>NexCloud</catalogName>

    <recurringBillingMode>IN_ADVANCE</recurringBillingMode>

    <currencies>
        <currency>USD</currency>
    </currencies>

    <units>
        <unit name="host"/>
        <unit name="ai-analytics"/>
    </units>

    <products>
        <product name="NexCloud">
            <category>BASE</category>
        </product>
    </products>
    <plans>
        <plan name="standard-monthly">
            <product>NexCloud</product>
            <finalPhase type="EVERGREEN">
                <duration>
                    <unit>UNLIMITED</unit>
                </duration>
                <usages>
                    <usage name="usage-monthly-in-standard" billingMode="IN_ARREAR" usageType="CONSUMABLE">
                        <billingPeriod>MONTHLY</billingPeriod>
                        <tiers>
                            <tier>
                                <blocks>
                                    <tieredBlock>
                                        <unit>host</unit>
                                        <size>1</size>
                                        <prices>
                                            <price>
                                                <currency>USD</currency>
                                                <value>0.01</value>
                                            </price>
                                        </prices>
                                        <max>-1</max>
                                    </tieredBlock>
                                    <tieredBlock>
                                        <unit>ai-analytics</unit>
                                        <size>100</size>
                                        <prices>
                                            <price>
                                                <currency>EUR</currency>
                                                <value>2</value>
                                            </price>
                                        </prices>
                                        <max>-1</max>
                                    </tieredBlock>
                                </blocks>
                            </tier>
                        </tiers>
                    </usage>
                </usages>
            </finalPhase>
        </plan>
        <plan name="pro-monthly">
            <product>NexCloud</product>
            <finalPhase type="EVERGREEN">
                <duration>
                    <unit>UNLIMITED</unit>
                </duration>
                <usages>
                    <usage name="usage-monthly-in-pro" billingMode="IN_ARREAR" usageType="CONSUMABLE">
                        <billingPeriod>MONTHLY</billingPeriod>
                        <tiers>
                            <tier>
                                <blocks>
                                    <tieredBlock>
                                        <unit>host</unit>
                                        <size>1</size>
                                        <prices>
                                            <price>
                                                <currency>USD</currency>
                                                <value>0.017</value>
                                            </price>
                                        </prices>
                                        <max>-1</max>
                                    </tieredBlock>
                                    <tieredBlock>
                                        <unit>ai-analytics</unit>
                                        <size>200</size>
                                        <prices>
                                            <price>
                                                <currency>EUR</currency>
                                                <value>2</value>
                                            </price>
                                        </prices>
                                        <max>-1</max>
                                    </tieredBlock>
                                </blocks>
                            </tier>
                        </tiers>
                    </usage>
                </usages>
            </finalPhase>
        </plan>
    </plans>
    <priceLists>
        <defaultPriceList name="DEFAULT">
            <plans>
                <plan>standard-monthly</plan>
                <plan>pro-monthly</plan>
            </plans>
        </defaultPriceList>
    </priceLists>
</catalog>

```


### Metering Input

아래는 위의 Catalog 를 통해 과금한 간이 인보이스 표입니다.

`Invoice 2018-01-01 ~ 2018-02-01`

| unit / plan  | total count | standard | pro |
|--------------|-------------|----------|-----|
| host         | 1,000       | $10      | $17 |
| ai-analytics | 1,000       | $20      | $10 |


빌링 플랫폼의 역할은 가격표에 따라 인보이스를 발행하고, 구독의 라이프사이클을 조정하고 실제 과금을 집행하는 것 까지 입니다. 

미터링 서비스를 통해 유닛의 사용량, 즉 표의 `total count` 수량을 빌링 플랫폼에 제공해야 하며, 또는 사용량을 제한해야 합니다. 
미터링 서비스가 사용량을 계산하기 위해서는 각 어플리케이션의 로그에 **user,unit,time,amount** 정보가 포함되야 합니다.

| Pamameter | Description | Type   |
|-----------|-------------|--------|
| user      | 누가        | String |
| unit      | 어떤 유닛을 | String |
| time      | 언제        | Milliseconds |
| amount    | 얼마나 썻나 | Long   |

미터링 서비스는 다음의 정보를 `kafka` 또는 `http` 로 받을 수 있습니다. `kafka` 사용시 채널은 **record** 를 사용하고, 
`http` 사용시 **POST /record** 로 전송해야 합니다.

**body example**
```
[
  {user: "some@gmail.com", unit: "monitoring", time: "1534377601010", amount: 1}
  {user: "some@gmail.com", unit: "monitoring", time: "1534377602034", amount: 2}
  {user: "some@gmail.com", unit: "monitoring", time: "1534377605092", amount: 1}
  {user: "some@gmail.com", unit: "monitoring", time: "1534377612334", amount: 1}
  {user: "some@gmail.com", unit: "monitoring", time: "1534377614123", amount: 2}
]
```

위와 같은 프로토콜은 앱에서 직접 데이터를 미터링 서비스로 보내야 하므로 비 효율적일 수 있습니다. 더 좋은 방법은 `FileBeat` 등의 
데이터 파이프라인 도구를 사용하여, 호스트 머신 레벨에서 앱의 로그를 미터링 서비스의 kafka 채널로 전송하는 것 입니다.

미터링 서비스에서 `Grok` 로그 필터를 사용하여 데이터를 빠르게 처리 할 수 있습니다.

![](image/grok.png)

**Log example**
```
2018-09-24T09:57:50,051+0000 user='some@gmail.com', unit='monitoring' amount=1...
2018-09-24T09:57:51,151+0000 user='some@gmail.com', unit='monitoring' amount=2...
2018-09-24T09:57:51,231+0000 user='some@gmail.com', unit='monitoring' amount=1...
2018-09-24T09:57:52,152+0000 user='some@gmail.com', unit='monitoring' amount=1...
2018-09-24T09:57:52,332+0000 user='some@gmail.com', unit='monitoring' amount=2...
```


**Grok Filter Example**

```
\[%{TIMESTAMP_ISO8601:timestamp}]\s+user='%{WORD:user}'\s+unit='%{WORD:unit}'\s+amount=%{NUMBER:client}
```

[Grok 패턴 연습페이지](http://grokconstructor.appspot.com/do/match#result)


### Metering Rule


#### Counting Method

`countingMethod` 는 사용량을 카운팅하는 방법으로, **AVG**, **PEAK**, **SUM** 세가지로 구분할 수 있습니다. 
**AVG,PEAK** 는 평균 또는 최대치를 카운팅하고, **SUM** 은 합산하여 카운팅합니다. 유닛별로 살펴보면 다음과 같습니다:

 - **host** : 매 시간 평균 몇개의 호스트에서 정보를 수집하고 있는지 알아야 하므로 **AVG** 를 사용합니다. 만약 매 시간 최대치로 과금을 부여하고자 할 경우
 **PEAK** 를 사용합니다.    

![](image/avg.png)

![](image/peak.png)


- **monitoring,analytics** : 매일 몇개의 리퀘스트를 수행하였는지 계산해야 하므로, **SUM** 을 사용합니다.

![](image/sum.png)

#### Period Splitting

`periodSplitting` 는 per day, per hour 등 과 같이 **기간 분할 카운팅** 을 설정하는 방법으로, **HOUR,DAY,SUBSCRIPTION_CYCLE** 이 있습니다.

**SUBSCRIPTION_CYCLE** 일 경우, 따로 기간 분할을 수행하지 않고, 인보이스에 발행 된 구독 기간대로 사용량을 집계합니다.

아래는 기간분할에 따라 달라지는 **AVG** 집계 모습입니다.

![](image/period.png)


#### Free Amount

`freeAmount` 와 `freePeriod` 는 기간별 무료 카운트 수를 의미합니다. `freeAmount` 는 무료 카운트 수이며, `freePeriod` 는 
**HOUR,DAY,SUBSCRIPTION_CYCLE** 이 있습니다.

아래는 무료 카운팅을 적용한 집계 모습의 예시입니다.

![](image/free.png)


#### Limitation

`limitAmount` 와 `limitRefreshInterval` 은 기간별 사용량 제한 카운트 수를 의미합니다. `limitAmount` 는 제한 카운트 수이며,
 `limitRefreshInterval` 은 사용량 제한을 초기화 하는 기간으로 **HOUR,DAY,SUBSCRIPTION_CYCLE,MANUALY** 이 있습니다.
 
 **SUBSCRIPTION_CYCLE** 은 인보이스 발행 시점에 초기화 되며, **MANUALY** 는 자동 초기화를 지원하지 않고 Api 를 통해서만 초기화 하는 경우입니다.
 
![](image/limit.png)



 






