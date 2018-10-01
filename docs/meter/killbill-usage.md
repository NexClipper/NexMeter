# Usage Catalog

## Usage Modes

Kill Bill supports invoicing customers based on their usage data, and the definition of such pricing scheme is defined in the catalog.
The `Plan` section of the Kill Bill catalog can contain one or several `usage` section(s) to define how customers will be billed.
These sections can be defined in addition to other non usage based billing pricing scheme, such as fixed or recurring price.

Each usage section will need to specify a `billingMode`:
The only billing mode supported today is the `IN_ARREAR` mode, which allows to bill customer based on usage data from previous periods.
We plan to add the `IN_ADVANCE` mode in the future to support prepaids-like scenario -- e.g toll bridge pass -- where consumer purchases a
certain number of units *in advance* and such units are decremented as consumer uses the service.

Each usage section will also need to specify a `usageType` and along with it, specify the `unit` (types) for which customer is being billed for -- e.g cell phone/minutes.
The usage type can be one of the following:

* `CONSUMABLE` : This is is used for defining prices that are based on the number of units (of a certain types) being consumed -- e.g water consumed in the last period. 
* `CAPACITY` : This is used for defining prices that are based on rates or upper bound values. In this scheme, we bill based on the maxium value seeing over the period -- e.g MBytes/sec

The following [blog post](http://killbill.io/blog/usage-billing) provides additional context about this use cases.

For each usage section, we support defining pricing *tiers* to allow billing customer differently based on their usage.
Such tiers work differently depending of the usage type, `CONSUMABLE` versus `CAPACITY`.

Each usage section also needs to define the `billingPeriod` -- e.g `MONTHLY` -- to define how often and for which period consumers are being billed.

## Consumable In Arrear Mode

In this mode, each `tier` definition contains a `tieredBlock`, that defines the limits and price associated to a particular `unit` type. We support two `TierBlockPolicy` policies:

* `ALL_TIER`: Users are charged across each tier based on their usage; the idea is to offer some form of tiered pricing where the upper level prices can be charged to a lower amount -- e.g clould usage where we want to reward users to consume more -- or higher -- e.g electricy bill to discourage users to consume more.
* `TOP_TIER`: In this model we still go through each tier, but end up charging everything at the top tier pricing. In this model, it probably makes sense to offer lower pricing for the top tier, this is a rewarding model.


### Consumable In Arrear Mode: `ALL_TIER`

Let's start with an example of a `tieredBlock` definition: The following tier block section defines the pricing for the `cell-phone-minutes` unit, and can be understood the following way:

```
<tieredBlock>
    <unit>cell-phone-minutes</unit>
    <size>10</size>
    <prices>
        <price>
            <currency>EUR</currency>
            <value>1.00</value>
        </price>
    </prices>
    <max>100</max>
</tieredBlock>
```

* This defines a block of size `10`, where each block is priced at 1.00 EUR
* The first `100` blocks of this size will be priced at 1.00 EUR
* If a consumer uses more than `100` blocks of this size, he will be charged at the price of the next tier.

As mentionned previously, such usage section can contain multiple tiers and also charge for multiple unit types.
The following section shows the defintion for 2 units, *cell-phone-minutes* and *Mbytes*, and define two tiers.
The `max` value for the last tier is defined with *-1* to allow for unbounded value:


```
<tiers>
    <tier>
        <blocks>
            <tieredBlock>
                <unit>cell-phone-minutes</unit>
                <size>10</size>
                <prices>
                    <price>
                        <currency>EUR</currency>
                        <value>1.00</value>
                    </price>
                </prices>
                <max>100</max>
            </tieredBlock>
            <tieredBlock>
                <unit>Mbytes</unit>
                <size>1</size>
                <prices>
                    <price>
                        <currency>EUR</currency>
                        <value>0.5</value>
                    </price>
                </prices>
                <max>1024</max>
            </tieredBlock>
        </blocks>
    </tier>
    <tier>
        <blocks>
            <tieredBlock>
                <unit>cell-phone-minutes</unit>
                <size>10</size>
                <prices>
                    <price>
                        <currency>EUR</currency>
                        <value>0.50</value>
                    </price>
                </prices>
                <max>-1</max>
            </tieredBlock>
            <tieredBlock>
                <unit>Mbytes</unit>
                <size>1</size>
                <prices>
                    <price>
                        <currency>EUR</currency>
                        <value>0.1</value>
                    </price>
                </prices>
                <max>-1</max>
            </tieredBlock>
        </blocks>
    </tier>
</tiers>
```

In the `CONSUMABLE` mode, when defining usage section with multiple unit (types), it is important to realize that the consumer *is charged independently
for each unit*, so for example, with the definition provided above:

Given the following usage data for the period:

* 1500 cell-phone-minutes
* 2048 Mbytes


So the total amount would be 739.4.

For more information on this model, see our [tutorial](http://docs.killbill.io/latest/consumable_in_arrear.html).


### Consumable In Arrear Mode: `TOP_TIER`

Using the same example from the previous section would lead to a different result:

The consumer would be charged for:

* cell-phone-minutes: 150 blocks of size 10 at 0.50 EUR = 150 * 0.5 = 75
* Mbytes: 2048 blocks of size 1 at 0.10 EUR = 2048 * 0.1 = 204.8

So the total amount would be 279.8

## Capacity In Arrear Mode

In the `CAPACITY` mode, each `tier` definition contains a list of `limit`, specifying for each `unit` (type) what is the maxium value for this tier. *In contrary to `CONSUMABLE` mode, the billing happens across the units*. Let's assume the following definition for one tier, with 2 different types of units, `bandwith-meg-sec` and `members`:


```
<tier>
   <limits>
       <limit>
           <unit>bandwith-meg-sec</unit>
           <max>100</max>
       </limit>
       <limit>
           <unit>members</unit>
           <max>500</max>
       </limit>
   </limits>
   <recurringPrice>
       <price>
           <currency>EUR</currency>
           <value>5.00</value>
       </price>
   </recurringPrice>
</tier>
```

Given the following usage data for the period:

* `bandwith-meg-sec`: A peak of 50 in the period
* `members`: A peak of 350 active members in the period

The user would be charged 5.00 EUR.

However if the `members` peak data was 501, this would move to the next tier -- not shown for simplicity.

So, in this model, the peak data for each unit is used to *define which tier to use*, and based on the tier we simply apply the pricing defined. 
