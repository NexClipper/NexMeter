# 페이팔 라이브 계정 사전 준비

문서 설명

[https://docs.woocommerce.com/document/subscriptions/faq/paypal-reference-transactions/#section-1](https://docs.woocommerce.com/document/subscriptions/faq/paypal-reference-transactions/#section-1)


전화해서 "Reference transactions" 뚤어달라고 해야함.


## 페이팔 프로덕션용 배포하기

DC/OS killbill marathon 파일에서,

```
  "env": {
    "KILLBILL_DAO_URL": "jdbc:mysql://192.168.0.162:12001/killbill",
    "KILLBILL_DAO_USER": "root",
    "KILLBILL_DAO_PASSWORD": "killbill",
    "TEST_MODE": "false"
  },
```

테스트 모드를 false 로 하면, 더이상 샌드박스와 연동되지 않고, 실제 페이팔 계정과 Api 통신을 합니다.