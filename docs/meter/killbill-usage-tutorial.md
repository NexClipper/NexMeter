# Usage: consumable in arrear

## Introduction

In this tutorial we will look at the Kill Bill *usage billing* feature. In particular, we will see how recurring subscriptions and usage can be configured together. The tutorial will focus on a type of usage that we refer to as *consumable in arrear* and that we explained in that [blog](http://killbill.io/blog/usage-billing/); in short, that is the ability to charge the customer for units he consumed at the end of the billing period.


## Scenario


Let's assume you want to build an online store selling artisanal chocolates, and geared towards the chocolate aficionados. The service you provide is based on a monthly subscription, that includes 1 box of your (organic certified and local produced) home made chocolates. In addition, you also provide videos on the topic (the history of chocolate, its fabrication, recipes of chocolate based dishes, the economy of chocolate, chocolate health benefits, ...) that are accessible online from your website. Customers that subscribed to the base service, can also stream your videos and will be invoiced based on their usage at the end of each period:

* The base service is available for $30/month and includes that ecstatic box of chocolate,
* The first 5 videos (streamed during the month) will be invoiced at $2/video
* The next videos (streamed during the month) will be invoiced at $1/video


## Configuration

Your catalog will have to specify the units for the usage section and also define the available plans; for the sake of simplicity we omitted other catalog sections, but to see how the pieces fit together you can look at the example provided with the [source code](https://github.com/killbill/killbill/blob/master/profiles/killbill/src/main/resources/SpyCarAdvanced.xml).


```
<units>
	<unit name="chocolate-monthly-videos"/>
</units>
<!--...-->
<plan name="chocolate-monthly">
     <product>Chocolate</product>
     <finalPhase type="EVERGREEN">
         <duration>
             <unit>UNLIMITED</unit>
         </duration>
         <recurring>
             <billingPeriod>MONTHLY</billingPeriod>
             <recurringPrice>
                 <price>
                     <currency>USD</currency>
                     <value>30.00</value>
                 </price>
             </recurringPrice>
         </recurring>
         <usages>
             <usage name="chocolate-monthly-videos" billingMode="IN_ARREAR" usageType="CONSUMABLE">
                 <billingPeriod>MONTHLY</billingPeriod>
                 <tiers>
                     <tier>
                         <blocks>
                             <tieredBlock>
                                 <unit>chocolate-videos</unit>
                                 <size>1</size>
                                 <prices>
                                     <price>
                                         <currency>USD</currency>
                                         <value>2</value>
                                     </price>
                                 </prices>
                                 <max>5</max>
                             </tieredBlock>
                         </blocks>
                     </tier>
                     <tier>
                         <blocks>
                             <tieredBlock>
                                 <unit>chocolate-videos</unit>
                                 <size>1</size>
                                 <prices>
                                     <price>
                                         <currency>USD</currency>
                                         <value>1</value>
                                     </price>
                                 </prices>
                                 <max>10000</max>
                             </tieredBlock>
                         </blocks>
                     </tier>
                 </tiers>
             </usage>
         </usages>
     </finalPhase>
</plan>
```

The plan description shows the following:

* There is a recurring section that specifies that customer should be invoiced (in advance) at the beginning of each month for $30
* There is a section usage that defines two tiers, each of them for a unit of `chocolate-videos` with a size of 1 (we pay for each video)
** The first tier specifies that below 5 videos, the price per video (unit) is set to $2.
** The second tier specifies that after 5 videos and below 10000 (arbitrary number large enough that ensures it will not be met), the price per video becomes $1.


## Putting The Pieces Together


### Usage and Metering

Kill Bill's usage module provides an API that can be used to record the per customer's usage. For instance, in order to record 1 unit of `chocolate-videos`, one would use the API `POST /1.0/kb/usages` with the following json body:

```
{
    "subscriptionId": "365987b2-5443-47e4-a467-c8962fc6995c",
    "unitUsageRecords": [
        {
            "unitType": "chocolate-videos",
            "usageRecords": [
                {
                    "recordDate": "2014-03-14",
                    "amount": 1
                }
            ]
        }
    ]
}
```

The calls to record usage are made per subscription, but they can include multiple unitTypes (if the catalog had defined them), and for each unit they can specify as many records as desired; the records themselves specify a date of when those units were consumed.

In our example, the online store could directly make the call to Kill Bill using that API to record each video as they are consumed.

Note that in scenarios where the consumption is much higher (for e.g in the case of a telecom company offering cell-phone minutes), one would need to use a *metering system* that would first *aggregate* the units on a daily granularity before making the call to Kill Bill. You can check the initial implementation of our [metring module](https://github.com/killbill/killbill-meter-plugin) that provides that aggregation functionality.


### Example of a Customer Usage

Let's assume Louis, who is very passionate about chocolate, subscribed to the service on March 13th and watched 13 videos during his first month.

On march 13th, the system will trigger an invoice that will cover the billing period march 13th -> april 13th, and it will contain one RECURRING item with a price of $30; in return, he will receive his first box of chocolate.

On april 13th, the system will now trigger a new invoice that will cover:

* A RECURRING item for the billing period from april 13th -> may 13th with a price of $30,
* A USAGE item for a price of $18 (= 5 * 2 + 8 * 1)

So we can see, that each invoice will include an ITEM for the RECURRING piece (charged ahead, i.e in advance), as well as a USAGE item (charged at the end of the billing period when usage is known, i.e in arrear).

== Additional Resources

We have integration tests that can used an example:

* [Catalog](https://github.com/killbill/killbill/blob/master/profiles/killbill/src/main/resources/SpyCarAdvanced.xml)
* [Test](https://github.com/killbill/killbill-integration-tests/blob/master/killbill-integration-tests/core/test_usage.rb)
